package rice.email.proxy.smtp;

import rice.post.*;

import rice.email.*;
import rice.email.proxy.smtp.manager.*;
import rice.email.proxy.util.*;
import rice.email.proxy.smtp.commands.*;
import rice.selector.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class NonBlockingSmtpServerImpl extends SelectionKeyHandler implements SmtpServer {

  public static int MAX_CONNECTIONS = 6;
  
  boolean acceptNonLocal = false;
  boolean gateway = false;
  boolean quit = false;
  int port;

  SmtpManager manager;

  SmtpCommandRegistry registry;

  Workspace workspace;

  EmailService email;
  
  ServerSocketChannel channel;
  
  SelectionKey key;
  
  int connections = 0;

  public NonBlockingSmtpServerImpl(int port, EmailService email, boolean gateway, PostEntityAddress address, boolean acceptNonLocal) throws Exception {
    this.acceptNonLocal = acceptNonLocal;
    this.gateway = gateway;
    this.port = port;
    this.email = email;
    this.manager = new SimpleManager(email, gateway, address);
    this.registry = new SmtpCommandRegistry();
    this.registry.load();
    this.workspace = new InMemoryWorkspace();

    initialize();
  }

  public int getPort() {
    return port;
  }
  
  public void start() {
  }

  public void initialize() throws IOException {
    // bind to port
    channel = ServerSocketChannel.open();
    channel.configureBlocking(false);
    channel.socket().bind(new InetSocketAddress(getPort()));
    
    SelectorManager.getSelectorManager().invoke(new Runnable() {
      public void run() {
        try {
          key = SelectorManager.getSelectorManager().register(channel, NonBlockingSmtpServerImpl.this, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
          System.out.println("ERROR modifiying SMTP server socket key " + e);
        }
      }
    });
  }
  
  protected void setAcceptable(final boolean acceptable) {
    // register interest in accepting connections
    SelectorManager.getSelectorManager().invoke(new Runnable() {
      public void run() {
        key.interestOps((acceptable ? SelectionKey.OP_ACCEPT : 0));
      }
    });
  }
  
  public void accept(SelectionKey key) {
    try {
      boolean turnoff = false;
      synchronized (this) {
        connections++;
      
        turnoff = (connections >= MAX_CONNECTIONS);
      }
      
      if (turnoff)
        setAcceptable(false);
      
      final Socket socket = ((SocketChannel) ((ServerSocketChannel) key.channel()).accept()).socket();
      
      System.out.println("Accepted connection " + connections + " of " + MAX_CONNECTIONS + " from " + socket.getInetAddress());
      
      if (acceptNonLocal || gateway || socket.getInetAddress().isLoopbackAddress() ||
          (socket.getInetAddress().equals(InetAddress.getLocalHost()))) {
        Thread thread = new Thread("SMTP Server Thread for " + socket.getInetAddress()) {
          public void run() {
            try {
              SmtpHandler handler = new SmtpHandler(registry, manager, workspace);
              handler.handleConnection(socket);
              socket.close();
              
              synchronized (NonBlockingSmtpServerImpl.this) {
                connections--; 
              }
              
              setAcceptable(true);
              System.out.println("Done with connection - now at " + connections + " of " + MAX_CONNECTIONS);
            } catch (IOException e) {
              System.out.println("IOException occurred during handling of connection - " + e);
            }
          }
        };
        
        thread.start();
      } else {
        System.out.println("Connection not local - aborting");
        
        OutputStream o = socket.getOutputStream();
        PrintWriter out = new PrintWriter(o, true);
        
        out.println("554 Connections only allowed locally");
        out.flush();
        socket.close();
      }
    } catch (IOException e) {
      System.out.println("IOException occurred during accepting of connection - " + e);
    }
  }
}

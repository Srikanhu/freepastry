
package rice.proxy;

import java.io.*;
import java.net.*;
import java.util.*;

import java.awt.*;
import javax.swing.*;

/**
 * This class represents a generic Java process launching program which reads in
 * preferences from a preferences file and then invokes another JVM using those
 * prefs.  If the launched JVM dies, this process can be configured to restart
 * the JVM any number of times before giving up.  This process can also be configured
 * to launch the second JVM with a specified memory allocation, etc...
 *
 * @author Alan Mislove
 */
public class Proxy {
  
  public static String PROXY_PARAMETERS_NAME = "proxy";
  
  protected Process process;
  
  protected LivenessMonitor lm;
    
  public Proxy() {
  }
  
  public void run(String params) throws IOException, InterruptedException {
    Parameters parameters = new Parameters(params);
    int count = 0;
    
    if (parameters.getBooleanParameter("proxy_automatic_update_enable")) {
      AutomaticUpdater au = new AutomaticUpdater(parameters);
      au.start();
    }
    
    if (parameters.getBooleanParameter("proxy_sleep_monitor_enable")) {
      SleepMonitor sm = new SleepMonitor(parameters);
      sm.start();
    }
    
    while (true) {
      String command = buildJavaCommand(parameters);
      String[] environment = buildJavaEnvironment(parameters);

      System.out.println("[Loader       ]: Launching command " + command);
      
      process = (environment.length > 0 ? Runtime.getRuntime().exec(command, environment) : Runtime.getRuntime().exec(command));
      lm = new LivenessMonitor(parameters, process);

      if (parameters.getBooleanParameter("proxy_liveness_monitor_enable"))
        lm.start();
      
      Printer error = new Printer(process.getErrorStream(), "[Error Stream ]: ");
      
      int exit = process.waitFor();    
      lm.die();
      
      // re-initialize parameters for debugging purposes
      parameters = new Parameters(params);
      
      if (exit != -1) { 
        System.out.println("[Loader       ]: Child process exited with value " + exit + " - restarting client");
        count++;
        
        if (count < parameters.getIntParameter("restart_max")) {
          System.out.println("[Loader       ]: Waiting for " + parameters.getIntParameter("restart_delay") + " milliseconds");   
          Thread.sleep(parameters.getIntParameter("restart_delay"));
        }
      } else {
        System.out.println("[Loader       ]: Child process exited with value " + exit + " - exiting loader");   
        break;
      }
    }
    
    System.exit(0);
  }

  protected String[] buildJavaEnvironment(Parameters parameters) {
    HashSet set = new HashSet();
    
    if (parameters.getBooleanParameter("java_profiling_enable") ||
        parameters.getBooleanParameter("java_thread_debugger_enable"))  {
      if (System.getProperty("os.name").toLowerCase().indexOf("windows") < 0) {
        set.add("LD_LIBRARY_PATH=" + parameters.getStringParameter("java_profiling_native_library_directory"));
      } 
    }
    
    return (String[]) set.toArray(new String[0]);
  }   
  
  protected String buildJavaCommand(Parameters parameters) {
    StringBuffer result = new StringBuffer();
    
    if (! ("".equals(parameters.getStringParameter("java_wrapper_command")))) {
      result.append(parameters.getStringParameter("java_wrapper_command"));
      result.append(" \"");
    }
    
    if (((parameters.getStringParameter("java_home") == null) ||
         (parameters.getStringParameter("java_home").equals(""))) && 
        (System.getProperty("os.name").toLowerCase().indexOf("windows") < 0))
      parameters.setStringParameter("java_home", System.getProperty("java.home"));
    
    if ((parameters.getStringParameter("java_home") != null) && (! ("".equals(parameters.getStringParameter("java_home"))))) {
      result.append(parameters.getStringParameter("java_home"));
      result.append(System.getProperty("file.separator"));
      result.append("bin");
      result.append(System.getProperty("file.separator"));
    }
    
    result.append(parameters.getStringParameter("java_command"));
    result.append(" -Xmx");
    result.append(parameters.getStringParameter("java_maximum_memory"));
    result.append(" -Xss");
    result.append(parameters.getStringParameter("java_stack_size"));
    if (System.getProperty("RECOVER") != null)
      result.append(" -DRECOVER=\""+System.getProperty("RECOVER")+"\"");
    
    if (parameters.getBooleanParameter("java_memory_free_enable")) {
      result.append(" -Xmaxf" + parameters.getDoubleParameter("java_memory_free_maximum"));
    }
    
    if (parameters.getBooleanParameter("java_use_server_vm")) {
      result.append(" -server");
    }
    
    if (parameters.getBooleanParameter("java_interpreted_mode")) {
      result.append(" -Xint");
    }
    
    if (parameters.getBooleanParameter("java_prefer_select") ||
        (parameters.getBooleanParameter("java_prefer_select_automatic_osx") && 
         System.getProperty("os.name").toLowerCase().indexOf("mac os x") >= 0)) {
      result.append(" -Djava.nio.preferSelect=true");
    }
    
    if (parameters.getBooleanParameter("java_debug_enable")) {
      result.append(" -Xdebug -Djava.compiler=NONE -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=");
      result.append(parameters.getStringParameter("java_debug_port"));
    }
    
    if (parameters.getBooleanParameter("java_hprof_enable")) {
      result.append(" -Xrunhprof");
    }
    
    if (parameters.getStringParameter("java_other_options") != null) {
      result.append(" " + parameters.getStringParameter("java_other_options"));
    }
    
    if (parameters.getBooleanParameter("java_profiling_enable")) {
      if (System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) {
        result.append(" -Djava.library.path=");
        result.append(parameters.getStringParameter("java_profiling_native_library_directory"));
      }
      
      result.append(" -Xrunpri");
      if (! parameters.getBooleanParameter("java_profiling_memory_enable"))
          result.append(":dmp=1");
          
      result.append(" -Xbootclasspath/a:");
      result.append(parameters.getStringParameter("java_profiling_library_directory"));
      result.append(System.getProperty("file.separator"));
      result.append("oibcp.jar -cp ");
      result.append(parameters.getStringParameter("java_profiling_library_directory"));
      result.append(System.getProperty("file.separator"));
      result.append("optit.jar");
      result.append(System.getProperty("path.separator"));
      
      DynamicClasspath dc = new DynamicClasspath(new File("."), parameters.getStringArrayParameter("java_classpath"));
      result.append(dc.getClasspath());
      
      result.append(" intuitive.audit.Audit -port ");
      result.append(parameters.getStringParameter("java_profiling_port"));
    } else if (parameters.getBooleanParameter("java_thread_debugger_enable"))  {
      result.append(" -Xruntdi:port=");
      result.append(parameters.getIntParameter("java_thread_debugger_port"));
      result.append(",analyzer=t");
      
      result.append("-Xint -Xbootclasspath/a:");
      result.append(parameters.getStringParameter("java_thread_debugger_library_directory"));
      result.append(System.getProperty("file.separator"));
      result.append("oibcp.jar -cp ");
      result.append(parameters.getStringParameter("java_thread_debugger_library_directory"));
      result.append(System.getProperty("file.separator"));
      result.append("optit.jar");
      result.append(System.getProperty("path.separator"));

      DynamicClasspath dc = new DynamicClasspath(new File("."), parameters.getStringArrayParameter("java_classpath"));
      result.append(dc.getClasspath());
    } else {    
      result.append(" -cp ");
        
      DynamicClasspath dc = new DynamicClasspath(new File("."), parameters.getStringArrayParameter("java_classpath"));
      result.append(dc.getClasspath());
    }
    
    result.append(" ");
    result.append(parameters.getStringParameter("java_main_class"));
    result.append(" ");
    result.append(parameters.getStringParameter("java_main_class_parameters"));
    
    if (! ("".equals(parameters.getStringParameter("java_wrapper_command")))) {
      result.append(" \"");
    }
    
    return result.toString();
  }
  
  public void restart() {
    if (process != null) 
      process.destroy();
    
    if (lm != null)
      lm.die();
  }
  
  public boolean verifyJar(String filename, byte[] hash, String md5) {
   /* String[] cp = new DynamicClasspath().getClasspath().split(System.getProperty("path.separator"));
    JarFile newJar = new JarFile(filename);
    int i=0;
    
    while ((oldJar == null) && (i < cp.length)) 
      if (! cp[i].equals(filename))
        JarFile oldJar = new JarFile(cp[i]);
      
    Certificate cert = oldJar */
    
    return rice.post.security.SecurityUtils.toHex(hash).trim().equalsIgnoreCase(md5.trim());
  }
  
  public static void main(String[] args) throws IOException, InterruptedException {
    Proxy proxy = new Proxy();
    proxy.run(PROXY_PARAMETERS_NAME);
  }
  
  private class Printer extends Thread {
    
    protected BufferedReader reader;
    protected String prefix;
    
    public Printer(InputStream input, String prefix) {
      this.reader = new BufferedReader(new InputStreamReader(input));
      this.prefix = prefix;
      
      start();
    }
    
    public void run() {
      try {
        boolean done = false;
       
        while (! done) {
          String line = reader.readLine();

          if (line != null) 
            System.out.println(prefix + line);
          else
            done = true;
        }
      } catch (IOException e) {
        System.err.println(e);
        e.printStackTrace();
      }
    }
  }
  
  private class LivenessMonitor extends Thread {
   
    protected Process process;
    
    protected int sleep;
    
    protected int timeout;
    
    protected boolean alive;
    
    protected boolean answered;
    
    public LivenessMonitor(Parameters parameters, Process process) {
      this.alive = true;
      this.process = process;
      this.sleep = parameters.getIntParameter("proxy_liveness_monitor_sleep");
      this.timeout = parameters.getIntParameter("proxy_liveness_monitor_timeout");
    }
    
    public void run() {
      try {
        // sleep to let JVM boot up
        Thread.sleep(2*timeout);
        
        while (alive) {
          this.answered = false;
          LivenessMonitorTest test = new LivenessMonitorTest(this, process);
          test.start();
          
          long start = System.currentTimeMillis();
          
          Thread.sleep(timeout);
          
          if (! answered) {
            System.err.println("SERIOUS ERROR: Process did not respond to liveness check - started at " + start + " now " + System.currentTimeMillis() + " - killing process");
            process.destroy();
            die();
          }
          
          Thread.sleep(sleep);
        }
      } catch (InterruptedException e) {
        die();
      }
    }
    
    public void die() {
      this.alive = false;
    }
    
    public void answered() {
      answered = true;
    }
  }
   
  protected class LivenessMonitorTest extends Thread {

    protected Process process;
    
    protected LivenessMonitor monitor;
    
    public LivenessMonitorTest(LivenessMonitor monitor, Process process) {
      this.monitor = monitor;
      this.process = process;
    }
    
    public void run() {
      try {
        int i = 27;
        
        process.getOutputStream().write(i);
        process.getOutputStream().flush();
        int j = 0;
        
        while ((j != i) && (j >= 0)) 
          j = process.getInputStream().read();
        
        if (j >= 0)
          monitor.answered();
      } catch (IOException e) {
        System.err.println("ERROR: Got IOException while checking liveness!" + e + " This is usually an unrecoverable JVM crash - we're going to exit now.");
        e.printStackTrace();
        System.exit(-1);
      } catch (NullPointerException e) {
        System.out.println("Liveness test ended in NullPointerExceptin " + e);
        e.printStackTrace();
      }
    }
  }
  
  protected class AutomaticUpdater extends Thread {
   
    protected Random rng = new Random();
    
    protected int interval;
    
    protected String root;
    protected String url;
    
    protected Parameters parameters;
    
    public AutomaticUpdater(Parameters parameters) {
      this.interval = parameters.getIntParameter("proxy_automatic_update_interval");
      this.root = parameters.getStringParameter("proxy_automatic_update_root");
      this.url = root + parameters.getStringParameter("proxy_automatic_update_latest_filename");
      this.parameters = parameters;
    }
    
    public void run() {
      while (true) {
        try {
          Thread.sleep(rng.nextInt(interval));
          
          try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            HttpFetcher hf = new HttpFetcher(new URL(url), baos);
            
            hf.fetch();
            
            String filename = new String(baos.toByteArray()).trim();
            
            if (filename.indexOf("\t") > 0)
              filename = filename.substring(0, filename.indexOf("\t"));
            
            if (! new File(".", filename).exists()) {
              ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
              HttpFetcher hf1 = new HttpFetcher(new URL(root + filename + ".md5sum"), baos1);
              
              hf1.fetch();
              
              String md5 = new String(baos1.toByteArray()).trim();
              
              if (parameters.getBooleanParameter("proxy_show_dialog") && parameters.getBooleanParameter("proxy_automatic_update_ask_user")) {
                String message = "A new version of the ePOST software has been detected.\n\n" +
                "Would you like to automatically upgrade to '" + filename + "' and restart your proxy?";
                int i = JOptionPane.showOptionDialog(null, message, "Updated Software Detected", 
                                                     0, JOptionPane.INFORMATION_MESSAGE, null, 
                                                     new Object[] {"Disable Automatic Updating", "Later", "Yes"}, "Yes");
                
                if (i == 0) {
                  String message2 = "Are your sure you wish to disable automatic updating?\n\n" +
                  "You can re-enable it by changing the field 'proxy_automatic_update_enable'\n" +
                  "in your proxy.params file.";
                  int j = JOptionPane.showConfirmDialog(null, message2, "Confirm Automatic Update Disable", 
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                  
                  if (j == JOptionPane.YES_OPTION) {
                    parameters.setStringParameter("proxy_automatic_update_enable", "false");
                    parameters.writeFile();
                    return;
                  }
                } else if (i == 2) {
                  hf = new HttpFetcher(new URL(root + filename), new FileOutputStream(new File(".", filename)));
                  byte[] bytes = hf.fetch();
                  
                  if (verifyJar(filename, bytes, md5)) {
                    restart();
                  } else {
                    System.err.println("ERROR - Corrupted download detected on file " + filename + " - hash " + rice.post.security.SecurityUtils.toHex(bytes) + " required " + md5);

                    JOptionPane.showMessageDialog(null, "It appears that your update download was corrupted - ePOST will try \n" + 
                                                        "again at the next update interval.\n\n" +
                                                  "Hash: " + rice.post.security.SecurityUtils.toHex(bytes) + " Required: " + md5,
                                                  "Corrupted Download Detected", JOptionPane.WARNING_MESSAGE, null);
                    
                    new File(".", filename).delete();
                  }
                }
              } else {
                hf = new HttpFetcher(new URL(root + filename), new FileOutputStream(new File(".", filename)));
                byte[] bytes = hf.fetch();
                
                if (verifyJar(filename, bytes, md5)) {
                  restart();   
                } else {
                  System.err.println("ERROR - Corrupted download detected on file " + filename + " - hash " + rice.post.security.SecurityUtils.toHex(bytes) + " required " + md5);
                  new File(".", filename).delete();
                }
              }
            }
          } catch (Exception e) {
            System.err.println("ERROR: Got exception " + e + " while running automatic update - ignoring");
            e.printStackTrace();
          }
        } catch (InterruptedException e) {
        }
      }
    }
  }
  
  protected class DynamicClasspath {
    
    protected File dir;
    
    protected File[] files;
    
    protected String[] other;
    
    public DynamicClasspath(File dir, String[] other) {
      this.dir = dir;
      this.other = other;
      
      this.files = dir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String filename) {
          return filename.endsWith(".jar");
        }
      });
      
      Arrays.sort(this.files, new Comparator() {
        public int compare(Object a, Object b) {
          long am = ((File) a).lastModified();
          long bm = ((File) b).lastModified();
          
          if (am < bm) return 1;
          else if (am > bm) return -1;
          else return 0;
        }
        
        public boolean equals(Object o) {
          return false;
        }
      });
    }
    
    public String getClasspath() {
      String seperator = System.getProperty("path.separator");
      StringBuffer buf = new StringBuffer();
      
      for (int i=0; i<files.length; i++) {
        buf.append(files[i].getName());
        
        if ((i < files.length-1) || ((other != null) && (other.length > 0)))
          buf.append(seperator);
      }
      
      if (other != null) {
        for (int i=0; i<other.length; i++) {
          buf.append(other[i]);
          
          if (i < other.length-1)
            buf.append(seperator);
        }
      }
      
      return buf.toString();
    }
  }
  
  private class SleepMonitor extends Thread {
    
    protected int sleep;
    
    protected int timeout;
    
    protected long last;
        
    public SleepMonitor(Parameters parameters) {
      this.sleep = parameters.getIntParameter("proxy_sleep_monitor_sleep");
      this.timeout = parameters.getIntParameter("proxy_sleep_monitor_timeout");
    }
    
    public void run() {
      this.last = System.currentTimeMillis();
      
      while (true) {
        try {          
          Thread.sleep(sleep);
          
          if (System.currentTimeMillis() - last > timeout) {
            System.err.println("INFO: Sleep detected - " + (System.currentTimeMillis() - last) + " millis elapsed - restarting ePOST!");
            restart();
          }
          
          last = System.currentTimeMillis();
        } catch (InterruptedException e) {}
      }
    }
  }  
}

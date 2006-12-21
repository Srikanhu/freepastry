/*************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate 

Copyright 2002, Rice University. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither  the name  of Rice  University (RICE) nor  the names  of its
contributors may be  used to endorse or promote  products derived from
this software without specific prior written permission.

This software is provided by RICE and the contributors on an "as is"
basis, without any representations or warranties of any kind, express
or implied including, but not limited to, representations or
warranties of non-infringement, merchantability or fitness for a
particular purpose. In no event shall RICE or contributors be liable
for any direct, indirect, incidental, special, exemplary, or
consequential damages (including, but not limited to, procurement of
substitute goods or services; loss of use, data, or profits; or
business interruption) however caused and on any theory of liability,
whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even
if advised of the possibility of such damage.

********************************************************************************/
package rice.email.proxy.imap.commands;

import rice.email.proxy.imap.ImapConnection;
import rice.email.proxy.imap.ImapState;

import rice.email.proxy.mailbox.MailboxException;

import java.io.IOException;


/**
 * RENAME command.
 * 
 * <p>
 * <a  href="http://asg.web.cmu.edu/rfc/rfc2060.html#sec-6.3.3">
 * http://asg.web.cmu.edu/rfc/rfc2060.html#sec-6.3.3 </a>
 * </p>
 */
public class RenameCommand extends AbstractImapCommand {
  String old_folder;
  String new_folder;
  
  public RenameCommand() {
    super("RENAME");
  }
  
  public boolean isValidForState(ImapState state) {
    return state.isAuthenticated();
  }
  
  public void execute() {
    try {
      getState().getMailbox().renameFolder(getOldFolder(), getNewFolder());
      taggedSimpleSuccess();
    } catch (MailboxException e) {
      taggedExceptionFailure(e);
    }
  }
  
  public String getOldFolder() {
    return old_folder;
  }
  
  public String getNewFolder() {
    return new_folder;
  }
  
  public void setOldFolder(String folder) {
    old_folder = folder;
  }
  
  public void setNewFolder(String folder) {
    new_folder = folder;
  }
}

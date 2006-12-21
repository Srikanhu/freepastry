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
package rice.email.proxy.imap.commands.search;

import rice.email.proxy.imap.ImapConnection;
import rice.email.proxy.mail.StoredMessage;
import rice.email.proxy.mailbox.*;


public class NoArgSearchPart extends SearchPart {

  public boolean includes(StoredMessage msg) {
    if (getType().equals("ALL")) {
      return true;
    } else if (getType().equals("ANSWERED")) {
      return handleFlag(msg, FlagList.ANSWERED_FLAG, true);
    } else if (getType().equals("DELETED")) {
      return handleFlag(msg, FlagList.DELETED_FLAG, true);
    } else if (getType().equals("DRAFT")) {
      return handleFlag(msg, FlagList.DRAFT_FLAG, true);
    } else if (getType().equals("FLAGGED")) {
      return handleFlag(msg, FlagList.FLAGGED_FLAG, true);
    } else if (getType().equals("NEW")) {
      return (handleFlag(msg, FlagList.RECENT_FLAG, true) &&
              handleFlag(msg, FlagList.SEEN_FLAG, false));
    } else if (getType().equals("OLD")) {
      return handleFlag(msg, FlagList.RECENT_FLAG, false);
    } else if (getType().equals("RECENT")) {
      return handleFlag(msg, FlagList.RECENT_FLAG, true);
    } else if (getType().equals("SEEN")) {
      return handleFlag(msg, FlagList.SEEN_FLAG, true);
    } else if (getType().equals("UNANSWERED")) {
      return handleFlag(msg, FlagList.ANSWERED_FLAG, false);
    } else if (getType().equals("UNDELETED")) {
      return handleFlag(msg, FlagList.DELETED_FLAG, false);
    } else if (getType().equals("UNDRAFT")) {
      return handleFlag(msg, FlagList.DRAFT_FLAG, false);
    } else if (getType().equals("UNFLAGGED")) {
      return handleFlag(msg, FlagList.FLAGGED_FLAG, false);
    } else if (getType().equals("UNSEEN")) {
      return handleFlag(msg, FlagList.SEEN_FLAG, false);
    } else {
      return false;
    }
  }

  protected boolean handleFlag(StoredMessage msg, String flag, boolean set) {
    if (set)
      return msg.getFlagList().isSet(flag);
    else
      return ! msg.getFlagList().isSet(flag);
  }
}
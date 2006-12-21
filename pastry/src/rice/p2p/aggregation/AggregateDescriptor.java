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
package rice.p2p.aggregation;

import rice.p2p.commonapi.Id;

class AggregateDescriptor {
  
  public Id key;
  public long currentLifetime;
  public ObjectDescriptor[] objects;
  public Id[] pointers;
  public boolean marker;
  public int referenceCount;

  public AggregateDescriptor(Id key, long currentLifetime, ObjectDescriptor[] objects, Id[] pointers) {
    this.key = key;
    this.currentLifetime = currentLifetime;
    this.objects = objects;
    this.pointers = pointers;
    this.marker = false;
    this.referenceCount = 0;
  }
  
  public int lookupNewest(Id id) {
    int result = -1;
    for (int i=0; i<objects.length; i++)
      if (objects[i].key.equals(id))
        if ((result == -1) || (objects[i].version > objects[result].version))
          result = i;
    return result;
  }

  public int lookupSpecific(Id id, long version) {
    for (int i=0; i<objects.length; i++)
      if (objects[i].key.equals(id) && (objects[i].version == version))
        return i;
        
    return -1;
  }
  
  public void addReference() {
    referenceCount ++;
  }
  
  public int objectsAliveAt(long pointInTime) {
    int result = 0;
    for (int i=0; i<objects.length; i++)
      if (objects[i].isAliveAt(pointInTime))
        result ++;
    return result;
  }
  
  public int bytesAliveAt(long pointInTime) {
    int result = 0;
    for (int i=0; i<objects.length; i++)
      if (objects[i].isAliveAt(pointInTime))
        result += objects[i].size;
    return result;
  }
}  


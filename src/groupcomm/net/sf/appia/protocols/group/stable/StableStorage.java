/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Initial developer(s): Alexandre Pinto and Hugo Miranda.
 * Contributor(s): See Appia web page for a list of contributors.
 */
package net.sf.appia.protocols.group.stable;

import net.sf.appia.core.AppiaError;

import org.apache.log4j.Logger;

public class StableStorage {
    private static Logger log = Logger.getLogger(StableStorage.class);

    private Header[] storage;
    private StableInfo cache_no=null;
    private int cache_rank=-1;

    public StableStorage() {
        storage=new Header[0];
    }

    public void reset(int size) {
        int i;
        for (i=0  ; i < storage.length ; i++) {
            if (storage[i].last != null)
                clean(i,storage[i].last.seqno);
        }

        if (size < storage.length) {
            Header[] aux=new Header[size];
            System.arraycopy(storage,0,aux,0,size);
            storage=aux;
            return;
        }

        if (size > storage.length) {
            Header[] aux=new Header[size];
            System.arraycopy(storage,0,aux,0,storage.length);
            for (i=storage.length ; i < aux.length ; i++)
                aux[i]=new Header();
            storage=aux;
            return;
        }
    }

    public void clean(int rank, long seqno) {
        Header h=storage[rank];

        while ((h.first != null) && (h.first.seqno <= seqno)) {

            if (debugFull) {
                log.debug("Cleaned "+h.first.seqno+" from "+rank);
            }
            h.first.omsg.discardAll();
            h.first=h.first.next;
        }

        if (h.first == null)
            h.last=null;
    }

    public void put(int rank, StableInfo no) {
        if ((storage[rank].last != null) && (storage[rank].last.seqno != no.seqno-1))
            throw new AppiaError("StableStorage: This is impossible");

        if (storage[rank].last == null) {
            storage[rank].first=storage[rank].last=no;
        } else {
            storage[rank].last.next=no;
            storage[rank].last=no;
        }
    }

    public StableInfo get(int rank, long seqno) {
        StableInfo no=null;

        if ((cache_rank == rank) && (cache_no != null) && (cache_no.seqno == seqno)) {
            no=cache_no;
            cache_no=cache_no.next;
            return no;
        }

        for (no=storage[rank].first ; no != null ; no=no.next) {
            if (no.seqno == seqno) {
                cache_rank=rank;
                cache_no=no.next;
                return no;
            } else if (no.seqno > seqno)
                return null;
        }

        return null;
    }

    private class Header {
        public StableInfo first=null;
        public StableInfo last=null;
    }

    // DEBUG
    private static final boolean debugFull=true;

    /*
  private void debug() {
    int i;
    StableInfo aux;

    for (i=0 ; i < storage.length ; i++) {

      log.debug("\t DEBUG STORAGE rank "+i);

      for (aux=storage[i].first ; aux != null ; aux=aux.next) {
        log.debug("\t\t DEBUG STORAGE seqno "+aux.seqno);
      }
    }
  }
     */


}
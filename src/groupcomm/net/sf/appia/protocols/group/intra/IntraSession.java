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
 
/**
 * Title:        Apia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Alexandre Pinto & Hugo Miranda & Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto & Hugo Miranda & Luis Rodrigues
 * @version 1.0
 */
package net.sf.appia.protocols.group.intra;


import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.protocols.group.AppiaGroupError;
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.suspect.Fail;

import org.apache.log4j.Logger;


public class IntraSession extends Session {
    private static Logger log = Logger.getLogger(IntraSession.class);
    
	/**
	 * Number of members that retransmit InstallView event. <br>
	 * InstallView isn't reliably broadcasted, but if a member doesn't receive it 
	 * it will remain in the previous view, and eventually it will be suspected.
	 * Even in this scenario, virtual synchrony properties aren't violated.<br>
	 * To avoid this, put in <i>K</i> a large enough value to be greater than 
	 * any view length.
	 */
  public static final int K=2;

  public IntraSession(Layer layer) {
    super(layer);
  }

  public void handle(Event event) {

    // InstallView
    if (event instanceof InstallView) {
      handleInstallView((InstallView)event); return;
      // Fail      
    } else if (event instanceof Fail) {
      handleFail((Fail)event); return;
    // ViewChange
    } else if (event instanceof ViewChange) {
      handleViewChange((ViewChange)event); return;
    // PreView
    } else if (event instanceof PreView) {
      handlePreView((PreView)event); return;
    // NewView
    } else if (event instanceof NewView) {
      handleNewView((NewView)event); return;
    // GroupInit
    } else if (event instanceof GroupInit) {
      handleGroupInit((GroupInit)event); return;
    // View
    } else if (event instanceof View) {
      handleView((View)event); return;
    }

    log.warn("Unwanted event (\""+event.getClass().getName()+"\") received. Continued...");
    try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
  }

  private ViewState vs;
  private LocalState ls;
  private Endpt my_endpt;
  private boolean new_view;
  private boolean installing;
  private boolean preparing;

  private void handleGroupInit(GroupInit ev) {
    my_endpt=ev.getEndpt();

    // values already tested by GroupInit
    ViewState new_vs=ev.getVS();
    LocalState new_ls=new LocalState(new_vs,my_endpt);

    try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

    sendView(new_vs,new_ls,ev.getChannel());
  }

  private void handleView(View ev) {
    vs=ev.vs;
    ls=ev.ls;

    new_view=false;
    installing=false;
    preparing=false;

    try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
  }

  private void handlePreView(PreView ev) {
    sendInstallView(ev.getChannel(),ev.vs,K);

    if (ev.vs.getRank(my_endpt) >= 0) {
      sendView(ev.vs,new LocalState(ev.vs,my_endpt),ev.getChannel());
    }
  }

  private void handleInstallView(InstallView ev) {
    // i am waiting for a View event
    if (preparing || installing)
       return;

    if (ls.failed[ev.orig]) {
      log.debug("discarded InstallView because it came from a failed member");
      return;
    }

    int k=ev.getMessage().popInt();

    ViewState new_vs=ViewState.pop(ev.getMessage());

    // see if i am not a member of the new view
    if (new_vs.getRank(my_endpt) < 0) {
      log.debug("discarded InstallView because i don't belong to view");
      return;
    }

    LocalState new_ls=new LocalState(new_vs,my_endpt);

    // resend InstallView
    if (new_ls.my_rank < k) {
      try {
        ViewState.push(new_vs,ev.getMessage());
        ev.getMessage().pushInt(k);
        ev.setDir(Direction.DOWN);
        ev.setSourceSession(this);
        ev.init();
        ev.go();
      } catch (AppiaEventException ex) {
        ex.printStackTrace();
        log.warn("impossible to resend InstallView");
      }
    }

    sendView(new_vs,new_ls,ev.getChannel());
  }

  private void handleFail(Fail ev) {
    try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

    if (ls.am_coord && !new_view) {
      if (debugFull)
        log.debug("Started view change due to Fail");
      
      sendNewView(ev.getChannel());
    }
  }

  private void handleNewView(NewView ev) {
    if (preparing || installing)
      return;

    try {
      ViewState new_vs=vs.next(my_endpt);
      new_vs.remove(ls.failed);
      sendPreView(new_vs,ev.getChannel());
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new AppiaGroupError("IntraSession: impossible to create new view state");
    }
  }

  private void handleViewChange(ViewChange ev) {
    if (ls.am_coord && !new_view) {
      if (debugFull)
        log.debug("Started view change due to ViewChange");
      
      sendNewView(ev.getChannel());
    }
  }

  private void sendView(ViewState vs, LocalState ls, Channel channel) {
    installing=true;
    try {
      View view=new View(vs,ls);
      EchoEvent echo=new EchoEvent(view,channel,Direction.DOWN,this);
      echo.go();
    } catch (AppiaEventException ex) {
      ex.printStackTrace();
      System.err.println("appia:group:IntraSession: impossible to send View");
    }
  }

  private void sendPreView(ViewState new_vs, Channel channel) {
    preparing=true;
    try {
      PreView ev=new PreView(new_vs,vs.group,vs.id);
      EchoEvent echo=new EchoEvent(ev,channel,Direction.UP,this);
      echo.go();
    } catch (AppiaEventException ex) {
      ex.printStackTrace();
      System.err.println("appia:group:IntraSession: impossible to send PreView");
    }
  }

  private void sendNewView(Channel channel) {
    new_view=true;
    try {
      NewView ev=new NewView(vs.group,vs.id);
      EchoEvent echo=new EchoEvent(ev,channel,Direction.UP,this);
      echo.go();
    } catch (AppiaEventException ex) {
      ex.printStackTrace();
      throw new AppiaGroupError("IntraSession: impossible to send NewView");
    }
  }

  private void sendInstallView(Channel channel, ViewState new_vs, int k) {
    try {
      InstallView ev=new InstallView(channel,Direction.DOWN,this,vs.group,vs.id);
      ViewState.push(new_vs,ev.getMessage());
      ev.getMessage().pushInt(k);
      ev.go();
    } catch (AppiaEventException ex) {
      ex.printStackTrace();
      log.warn("impossible to send InstallView");
    }
  }

  // DEBUG
  public static final boolean debugFull=true;
}

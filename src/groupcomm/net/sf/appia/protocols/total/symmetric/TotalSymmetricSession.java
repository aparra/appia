package net.sf.appia.protocols.total.symmetric;

import java.util.Vector;
import java.util.Random;
import java.lang.Math;
import java.net.InetSocketAddress;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.events.Send;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.suspect.Fail;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.total.symmetric.events.SymmetricAlive;
import net.sf.appia.protocols.total.symmetric.events.SymmetricAliveTimer;
import net.sf.appia.protocols.total.symmetric.events.SymmetricChangeTimer;


/**
 * TotalSymmetricSession implements a total order protocol based on
 * Lamport's total/causal logical clock algorithm.
 */
public class TotalSymmetricSession extends Session { 


    // The maximum time that elapses before a message with no data is sent is
    // calculated like this: random nr. between 0 and 
    // CAUSAL_ALIVER_TIMER_RANDOM added to CAUSAL_ALIVE_TIMER_MIN
    public static final int CAUSAL_ALIVE_TIMER_MIN = 5000;
    public static final int CAUSAL_ALIVE_TIMER_RANDOM = 1000;
    public static final int CAUSAL_ALIVE_TIMER_MAX = CAUSAL_ALIVE_TIMER_MIN +
    CAUSAL_ALIVE_TIMER_RANDOM;
    public static final int MSG_LIVE_TIME = 5000;
    public static final int CAUSAL_MUDANCA_TIMEOUT = CAUSAL_ALIVE_TIMER_MAX +
    MSG_LIVE_TIME;
    //private static final boolean DEBUG=false;

    /* 
     * State change timestamp - after LAST_EST the clock must be
     * reinitialized.
     */
    private static final long LAST_EST = Long.MAX_VALUE/2;
    // keeps the element's addresses temporarily
    private ViewState vs=null;
    // last timestamp received from each emitter
    private long[] lastTsReceived;
    // last timestamp delivered of each emitter
    private long[] lastTsDelivered;
    // temporarily holds the messages ordered by timestamp until they are
    // delivered
    private Vector<MsgTotalSymmetric> msgQueue;
    // indicates which elements are failed
    private boolean[] failed;
    // indicates if there is a timer active
    private boolean aliveTimerOn=false;
    // lamport logical clock
    private long clock;
    // the channel we're on.
    private Channel channel;
    // indicates if the group is blocked
    private boolean bloqueado=true;
    // our rank in the group
    private int my_rank;
    // transition state
    private boolean transition=false;
    // change state
    private boolean change=false;

    /*************************************************************/
    public TotalSymmetricSession(Layer l) { super(l); }

    private void incrementClock() {
        //clock+=my_rank+1;
        clock++;
    }

    private void updateClock(long est){
        /*
	  long aux;

	  if (clock<=est)
	  {
	  aux=est-clock;
	  aux=aux/(my_rank+1);
	  aux++;
	  clock+=(aux*(my_rank+1));
	  }
         */
        if(clock<=est) clock=est++;
    }

    /* turns off a SymmetricAliveTimer (when we receive such a timer we send a
     * SymmetricAlive) if there is a last timestamp received by another rank less
     * than mine, because only when all the members are waiting for my
     * timestamp to deliver the messages should the SymmetricAliveTimer stay on.
     */  
    private void turnoffCausalAliveTimer(){
        SymmetricAliveTimer cat=null;
        long minEst=minRecvTs();

        // if(DEBUG) debug("estamos na funcao turnoffCausalAliveTimer, 
        // minEst:"+String.valueOf(minEst)+", a minha est:"+
        // String.valueOf(lastTsReceived[my_rank]) );
        if (minEst<lastTsReceived[my_rank] && aliveTimerOn) {
            // if(DEBUG) debug("vai ser cancelado o CasualAliveTimer");
            try {
                cat = new SymmetricAliveTimer("causal alive timer", 0, channel,
                        this, EventQualifier.OFF);
                cat.go();
                aliveTimerOn = false;
            } catch(AppiaException e){//if(DEBUG) e.printStackTrace();
                error("Could not turn off a SymmetricAliveTimer");
            }
        }
    }


    /* Creates the SymmetricAliveTimer:
     * if my last timestamp received (lastTsReceived) is the smallest, that
     * means that all the other members and I are waiting for my timestamp to
     * deliver messages.
     */
    private void createCausalAliveTimer(){
        SymmetricAliveTimer cat = null;
        long minEst = minRecvTs();
        int ms = 0;

        if (minEst==lastTsReceived[my_rank] && aliveTimerOn==false) {

            // calculate the timer's time
            for(int i=0;i<my_rank+1;i++)
                ms = (new Random()).nextInt(CAUSAL_ALIVE_TIMER_RANDOM);
            ms += CAUSAL_ALIVE_TIMER_MIN;
            // if(DEBUG) debug("ms will be = "+String.valueOf(ms));

            try {
                cat = new SymmetricAliveTimer("causal alive timer",
                        ms,
                        channel, this, 
                        EventQualifier.ON);
                cat.go();
                aliveTimerOn = true;
            } catch(AppiaException e){//if(DEBUG) e.printStackTrace();
                error("Impossible to create a SymmetricAliveTimer");
            }
        }
    }

    /* puts us in normal (initial) state - the clock has turned around */
    private void stateNormal() {
        transition = false;
        change = false;	
    }

    /* puts us in transition state - in this state we can receive msgs with
     * timestamps above and below LAST_EST, we just need to convert them.
     */
    private void stateTransition() {
        transition = true;
    }

    /* puts us in change state - convert the timestamps that are in the two
     * arrays and queue.
     */
    private void stateChange() {
        MsgTotalSymmetric msg;
        SymmetricChangeTimer cmt=null;
        int i;

        // set timer to (later) terminate the change state
        try{
            cmt= new SymmetricChangeTimer(channel, this);
            cmt.go();

            // convert the two arrays
            for (i=0;i<lastTsReceived.length;i++) {
                lastTsReceived[i]=lastTsReceived[i] % LAST_EST;
                lastTsDelivered[i]=lastTsDelivered[i] % LAST_EST;
            }

            // convert the msgQueue
            for (i=0;i<msgQueue.size();i++) {
                msg=(MsgTotalSymmetric)msgQueue.get(i);
                msg.est=msg.est % LAST_EST;
            }

            // convert the clock
            clock = clock % LAST_EST;
            change = true;
        } catch(AppiaException e){
            //if(DEBUG) e.printStackTrace();
            error("Impossible to create SymmetricChangeTimer");
            // the function will be called later
        }

    }

    // switch to transition or change state if possible
    private void switchState() {
        long est;
        if (transition == false) {
            est = maxRecvTs();
            if(est > LAST_EST) stateTransition();
        } else {
            if (change == false) {
                est = minDelivTs();
                // if(DEBUG) debug("will try to switch to state change if "+
                // String.valueOf(est) + " > 2 !");
                if (est > LAST_EST) stateChange();
            }
        }
    }

    /* change the timestamp in case we are in transition state */
    private void modifyTimestamp(MsgTotalSymmetric msg){
        int rank;

        if (transition) {
            rank = msg.evt.orig;
            if (change == false) {
                if (msg.est < LAST_EST) {
                    if (msg.est <= lastTsReceived[rank]) {
                        msg.est=msg.est+LAST_EST;
                    }
                }
            } else {
                if( msg.est>=LAST_EST) msg.est=msg.est % LAST_EST;
            }
        }
    }

    private long modifyTimestamp(int rank, long est) {
        long auxEst;

        if (transition) {
            //rank = evt.orig;
            if (change == false) {
                if (est < LAST_EST) {
                    if (est <= lastTsReceived[rank]) {
                        auxEst = est + LAST_EST;
                        return auxEst;
                    }
                }
            } else {
                if (est >= LAST_EST) {
                    auxEst = est % LAST_EST;
                    return auxEst;
                }
            }
        }
        return est;
    }





    /************************************************************/

    /* creates the structures when the group is initialized */
    private void handleGroupInit(GroupInit e) {
        //if(DEBUG) debug("my rank is: " + String.valueOf(my_rank));
        msgQueue = new Vector<MsgTotalSymmetric>(10,10);
        try {
            e.go();
        } catch(AppiaEventException ex){
            // if(DEBUG) ex.printStackTrace();
            error("Nao foi possivel enviar o evento GroupInit");
        }
    }

    /* updates the viewstate, puts us in non-blocked state and flushes any
     * messages in the buffers.
     */
    private void handleView(View e) {
        // if(DEBUG) debug(" -- Previous view had " + 
        // String.valueOf(this.vs.addresses.length) +
        // " elements, new view has " + String.valueOf(e.vs.addresses.length) +
        // " elements --");
        this.vs = e.vs;
        my_rank = e.ls.my_rank;
        lastTsReceived = new long[vs.addresses.length];
        lastTsDelivered = new long[vs.addresses.length];
        clock = 1; // initialize the clock


        // the group may have changed - the nr. of elements may be smaller
        updateGroup();
        try {
            e.go();
        } catch(AppiaEventException ex) {
            //if(DEBUG) ex.printStackTrace();
            error("Nao foi possivel enviar o evento View");
        }
        bloqueado = false;
    }


    private void handleCausalAliveTimer(SymmetricAliveTimer e) {
        SymmetricAlive aux=null;
        aliveTimerOn = false;
        // create a SymmetricAlive to send to the other members and create a clone
        // for me to receive/process
        try {
            SymmetricAlive ca =
                new SymmetricAlive(channel, this, vs.group, vs.id, clock);
            incrementClock();
            aux = (SymmetricAlive) ca.cloneEvent();
            aux.setSourceSession(this);
            aux.setDir(Direction.UP);
            aux.orig = my_rank;
            aux.init();
            ca.go();
            receiveCausalAlive(aux);
        } catch(AppiaEventException ex) {
            //if(DEBUG) ex.printStackTrace();
            error("Impossible to send SymmetricAlive event");
            clock--;
        } catch(CloneNotSupportedException ex) {
            error("Impossible to send SymmetricAlive event");
            clock--;
        }
    }


    // handle the events that have the messages in them
    private void handleGroupSendableEvent(GroupSendableEvent e) {
        GroupSendableEvent copiaE = null;

        if (e.getDir() == Direction.DOWN) {
            addHeader(e);
            try {
                copiaE = (GroupSendableEvent) e.cloneEvent();
                e.go();
                copiaE.setSourceSession(this);
                copiaE.setDir(Direction.UP);
                copiaE.orig=my_rank;
                copiaE.source = new Endpt("0"); // temporary - needs fix.
                copiaE.init();
            } catch(AppiaEventException ex) {
                // if(DEBUG) ex.printStackTrace();
                error("Impossible to resend a message");
                return;
            } catch(CloneNotSupportedException ex){
                error("Impossible to clone a message");
            }

        }
        else copiaE = e;

        dispatchMsg(copiaE);
        // check if my message is the last one, if so set a timer to send a 
        // SymmetricAlive
        if (e.getDir()==Direction.UP) createCausalAliveTimer();
        else turnoffCausalAliveTimer();
        // debug //
        //if(DEBUG) showDebugTables();

    }


    /* Update the structures with the new group view */
    private void updateGroup(){
        long[] aux1, aux2;
        int j = 0;

        // check if the group has the same number of members
        if(vs.addresses.length==lastTsReceived.length) return;

        aux1 = new long[vs.addresses.length];
        aux2 = new long[vs.addresses.length];
        /* System.out.println("faileds = " + String.valueOf(failed.length) +
	   " old vector = " + String.valueOf(lastTsReceived.length) + 
	   " new vector = " + String.valueOf(aux1.length));
	   for(int i=0;i<failed.length;i++)
	   if(failed[i]) debug("element " + String.valueOf(i) + " went down");
         */

        for (int i = 0; i < failed.length; i++) {
            if (failed[i] == false) {
                aux1[j]=lastTsReceived[i];
                aux2[j]=lastTsDelivered[i];
                j++;
            }
            // this was removed because right now the msgQueue is empty
            // else {
            //    if(DEBUG)
            //       debug("o elemento com o rank "+String.valueOf(i)+" saiu");
            //	  actualizarMensagens(i);
            // }
        }
        lastTsReceived = aux1;
        lastTsDelivered = aux2;
        failed = null;
    }




    // place/add our header to the msgs, i.e., place the timestamp
    private void addHeader(GroupSendableEvent e){
        e.getMessage().pushObject(new Long(clock));
        incrementClock();
    }

    // to obtain the rank of whom to send the message from
    private int getSourceRank(GroupSendableEvent e){
        if (e.getDir()==Direction.DOWN)
            return my_rank; // if it comes from above it's me
        else return e.orig;
    }

    // to obtain the rank of the msgs's receiver
    private int getDestRank(GroupSendableEvent e) {
        if (e instanceof Send) 
            return vs.getRankByAddress((InetSocketAddress)e.dest); // should not be used
        else return -1; //indica multicast / para todos
    }

    // update the lastTsReceived vector, i.e., place the timestamp of the 
    // message in the vector
    private void placeReceivedTs(MsgTotalSymmetric msg) {
        lastTsReceived[getSourceRank(msg.evt)] = msg.est;
        updateClock(msg.est);
    }

    // indicates if it is possible to send a message
    private boolean canSend() {
        for (int i = 0; i < lastTsReceived.length; i++) {
            if (lastTsReceived[i] <= lastTsDelivered[i])
                return false;
        }
        return true;
    }

    // insert messages in the queue
    private void insertMsgQueue(MsgTotalSymmetric msg) {
        MsgTotalSymmetric auxMsg;
        int i;

        if (msgQueue.isEmpty())
            msgQueue.add(msg);
        else {

            for (i = 0; i < msgQueue.size(); i++) {
                auxMsg = (MsgTotalSymmetric) msgQueue.get(i);
                if(msg.est <= auxMsg.est) {
                    msgQueue.add(i, msg);
                    return;
                }
            }
            msgQueue.add(i, msg);
        }
    }

    // returns the smallest timestamp in lastTsReceived
    private long minRecvTs() {
        long minEst = 0;
        int i;

        for (i = 1, minEst = lastTsReceived[0]; i < lastTsReceived.length; i++)
            minEst = Utils.min(minEst, lastTsReceived[i]);
        return minEst;	
    }

    // returns the smallest timestamtp in lastTsDelivered
    private long minDelivTs() {
        long minEst = 0;
        int i;

        for (i = 1,minEst = lastTsDelivered[0]; i < lastTsDelivered.length;i++)
            minEst = Math.min(minEst, lastTsDelivered[i]);
        return minEst;
    }

    // returns the largest timestamp in lastTsReceived
    private long maxRecvTs() {
        long maxEst = 0;
        int i;

        for (i = 1, maxEst = lastTsReceived[0]; i < lastTsReceived.length; i++)
            maxEst = Math.max(maxEst, lastTsReceived[i]);
        return maxEst;	
    }


    // send all the msgs in the queue
    private void flushQueue() {
        MsgTotalSymmetric msg;
        int x;

        for (int i=0;i<msgQueue.size();i++) {
            msg = (MsgTotalSymmetric)msgQueue.get(i);

            if ((x = msgsWithSameTs(msgQueue, i, msg.est))>1) {
                // if(DEBUG) debug("call ordenarFila on index:"+
                // String.valueOf(i) + ", num_elt:" + String.valueOf(x));
                reorderQueue(msgQueue, i, x);
                for(int j=0;j<x;j++) sendQueuedMsg(i);
            } else
                sendQueuedMsg(i); 
            i--; // next time test on the same index
        }
    }


    // send all mesgs that can be sent
    private void sendMsgs() {
        MsgTotalSymmetric msg;
        long minEst = minRecvTs();
        int x;

        // if(DEBUG) debug("In enviarMsgs: smallest timestamp is:" + 
        // String.valueOf(minEst)+"!");
        for (int i = 0; i < msgQueue.size(); i++) {
            msg = (MsgTotalSymmetric) msgQueue.get(i);	
            if(minEst >= msg.est) {
                // case send - already received msgs from everybody - send to
                // minEst
                if ((x = msgsWithSameTs(msgQueue, i, msg.est)) > 1) {
                    // if(DEBUG) debug("call ordenarFila on index:" +
                    // String.valueOf(i) + ", num_elt:"+String.valueOf(x));
                    reorderQueue(msgQueue, i, x);
                    for(int j=0;j<x;j++) sendQueuedMsg(i);
                } else
                    sendQueuedMsg(i); 
                i--; // next time test on the same index
            }
        }
    }

    /* send a msg in queue fila on index i and update the pertaining structures
     * (lastTsDelivered)
     */
    private void sendQueuedMsg(int i) {
        int rank;
        MsgTotalSymmetric msg;

        msg = (MsgTotalSymmetric) msgQueue.get(i);
        rank = getDestRank(msg.evt);
        //if(DEBUG) debug("msg sent with est: " + String.valueOf(msg.est) +
        // ", orig:" + String.valueOf(getSourceRank(msg.evt))+", dest: " +
        // String.valueOf(rank)+", dirct:"+
        // String.valueOf(msg.evt.getDirection().direction));
        if (rank != -1)
            lastTsDelivered[rank]=msg.est;
        else 
            for(int j = 0; j < lastTsDelivered.length; j++) 
                lastTsDelivered[j]=msg.est;
        try {
            msg.evt.go();
        } catch(AppiaEventException ex){
            //if(DEBUG) ex.printStackTrace();	
            error("Could not send a message, message discarded");
        }

        msgQueue.remove(i);
    }

    /* returns the nr. of msgs with timestamp == est that are in the queue from
     * index i, expecting i has timestamp est (not checked here)
     */
    private int msgsWithSameTs(Vector<MsgTotalSymmetric> fila, int i, long est) {
        int ret = 1;
        MsgTotalSymmetric msg;

        // case where there is no more msgs in the queue
        if(i==fila.size()-1) return ret;

        for (int j=i+1;j<fila.size();j++,ret++) {
            msg = (MsgTotalSymmetric)msgQueue.get(j);
            if (est != msg.est) return ret;
        }
        return ret;
    }

    /*  Reorder "num_msg" msgs in the queue fila starting from index i.
     *  Messages are order in a deterministic order:
     *  The message with the smallest sender rank will be first.
     */
    private void reorderQueue(Vector<MsgTotalSymmetric> fila, int i, int num_msg) {
        int rank;
        MsgTotalSymmetric msg;
        boolean tudoOrdenado = false;	

        while (!tudoOrdenado) {
            int antRank = -1;
            tudoOrdenado = true;
            for (int j = i; j < (i+num_msg); j++) {
                msg = (MsgTotalSymmetric)fila.get(j);
                rank = getSourceRank(msg.evt);
                // if(DEBUG) debug("my sender rank is:"+
                // String.valueOf(rank)+", and index:"+String.valueOf(j));
                if (antRank != -1 && antRank > rank) {
                    //exchange positions
                    fila.add(j-1, fila.remove(j));
                    tudoOrdenado = false;	
                } else
                    antRank = rank;
            }
        }
    }

    // place a message in the queue and send a message if possible
    private void dispatchMsg(GroupSendableEvent e) {
        MsgTotalSymmetric msg = new MsgTotalSymmetric(e);
        modifyTimestamp(msg);
        placeReceivedTs(msg);
        insertMsgQueue(msg);
        if(bloqueado == false && canSend()) {
            // if(DEBUG) debug("calling sendMsgs()");
            sendMsgs();
        }
        switchState();
    }


    /* receiva a SymmetricAlive - update lastTsReceived with the est of the 
     * causalAlive and deliver messages if possible.
     */
    private void receiveCausalAlive(SymmetricAlive e){
        long est;

        Message om = e.getMessage();
        est = ((Long)om.popObject()).longValue();
        est = modifyTimestamp(e.orig, est);
        lastTsReceived[e.orig] = est;
        if (bloqueado == false && canSend())
            sendMsgs();
        switchState();
        // if(DEBUG) showDebugTables();
    }


    // function that handles the events - called by Appia
    public void handle(Event e) {
        try {
            if (e instanceof ChannelInit) {
                // if(DEBUG) debug("handled ChannelInit");	    	
                channel = e.getChannel();
                e.go();
            } else if (e instanceof GroupInit) {
                // if(DEBUG) debug("handled GroupInit");	    	
                handleGroupInit((GroupInit)e);
            } else if (e instanceof Fail) {
                // if(DEBUG) debug("handled Fail - nr. of failed elements: " +
                // String.valueOf(((Fail)e).failed.length));  	
                if(failed == null)
                    failed = ((Fail) e).failed;
                else
                    for(int i = 0; i < ((Fail) e).failed.length; i++)
                        if (((Fail) e).failed[i])
                            failed[i] = true;
                e.go();
            }
            // this event is sent by the group when "something goes wrong",
            // p.e. when a member leaves.
            // this event will then be followed by a view, after we have sent
            // this event with e.go()
            else if (e instanceof BlockOk) {
                if (e.getDir()==Direction.UP) {
                    //if(DEBUG) debug("handled BlockOk");
                    bloqueado = true;
                    flushQueue();
                }
                e.go();
            }
            // the view is sent by the group protocol when, p.e., the group has
            // changed
            else if (e instanceof View) {
                //if(DEBUG) debug("handled View");	    	
                handleView((View) e);
            }
            else if ( e instanceof SymmetricAliveTimer) {
                //if(DEBUG) debug("handled SymmetricAliveTimer");
                handleCausalAliveTimer((SymmetricAliveTimer)e);
            }
            else if ( e instanceof SymmetricChangeTimer) {
                //if(DEBUG) debug("handled SymmetricChangeTimer");
                stateNormal(); // change to normal state
            }
            else if (e instanceof GroupSendableEvent) {
                if ( e instanceof SymmetricAlive) {
                    /* if(DEBUG) {
		       int rank = getSourceRank((GroupSendableEvent)e);
		       debug("handled a SymmetricAlive from:" +
		       String.valueOf(rank));
		       }
                     */
                    receiveCausalAlive((SymmetricAlive)e);
                } else {
                    // if(DEBUG) debug("handled GroupSendableEvent");  	
                    // these are the events that have messages
                    handleGroupSendableEvent((GroupSendableEvent)e);
                }
            }
            /* else {
	       if(DEBUG) debug("handled unknow event");
	       e.go();
	       }
             */
        }
        catch(AppiaEventException ex) {
            System.err.println("Exception trying to call event.go()");
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    /*
    private void showDebugTables(){
	MsgTotalSymmetric msg;

	debug("vector lastTsReceived");
	for(int i = 0; i < lastTsReceived.length; i++)
	    debug(" i" + String.valueOf(i) + " - " + 
		  String.valueOf(lastTsReceived [i]));
	debug("\n");
	debug("vector lastTsDelivered");
	for(int i = 0; i < lastTsDelivered.length; i++)
	    debug("i" + String.valueOf(i) + " - " +
		  String.valueOf(lastTsDelivered[i]));
	debug("\n");
	debug("nr. of msgs in the queue: " + String.valueOf(msgQueue.size()));
	for (int i = 0; i < msgQueue.size(); i++) {
	    msg = (MsgTotalSymmetric)msgQueue.get(i);
	    debug("est of msg " + String.valueOf(i) + " is: " +
		  String.valueOf(msg.est));
	}
    }

    private static void debug(String line) {
	System.out.println("DEBUG: "+line);
    }

    private static void pause() {
	try {
	    DataInputStream dt = new DataInputStream(System.in);
	    String l = dt.readLine();
	} catch(IOException e) { e.printStackTrace(); }
    }
     */

    private static void error(String line){
        System.err.println("Erro causal: "+line); 
    }

    public void boundSessions(Channel channel) {}
}

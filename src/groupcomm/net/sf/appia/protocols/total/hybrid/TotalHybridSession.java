package net.sf.appia.protocols.total.hybrid;

import java.util.ListIterator;
import java.util.StringTokenizer;

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
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;


/**
 * TotalHybridSession is the actual implementation of the Hybrid Protocol.
 */
public class TotalHybridSession extends Session implements InitializableSession {
	
	
	private Channel channel;
	
	private LocalState ls;
	private ViewState vs;
	
	/**********PROTOCOL VARIABLES***********/
	//Process identifier is on the LocalState
	private int pid; 
	private int messageCount;
	private int lastDelivered;
	//private int role;
	private int roleNumber;
	private int state;
	private int ticketCounter;
	//ProcessDescriptor sequencer;
	private int sequencer; //sequencer rank
	private Configuration configuration;
	
	private MessageList messageList;
	private MessageList pendingList;
	private MessageList recvPendingList;
	
	private UnorderedTicketList unorderedList;
	private ListOfTickets orderedList;
	private ListOfTickets finalOrderedList;
	private ListOfTickets issuedList;
	/*******************************************/
	
	//APPIA VARIABLES OR NON PROTOCOL SPECIFIC 
	/****************************************/
	private Average transTimes[];
	private Delays delays[];
	//the largest ticket received
	private int maxticket=0;
	private boolean blocked = false;
	/****************************************/
	
	/*FIXED CONFIGURATION*/
	private ConfigProps props;
//	private boolean blockedConfig = false; //The roles of the nodes are locked and not dynamic
	
	/*CONSTANTS*/
	private  static final int ACTIVE=0;
	private  static final int PASSIVE=1;
	private static final int CHGSEQ=2;
	private static final int TOPASSIVE=3;
	private  static final int TOACTIVE=4;
	private static final int NOCHG=5;
	
	/**
	 * Basic constructor.
	 */
	public TotalHybridSession(Layer l) {
		super(l);
		
		unorderedList= new UnorderedTicketList(1);
		orderedList= new ListOfTickets();
		finalOrderedList = new ListOfTickets();
		issuedList= new ListOfTickets();
		messageList= new MessageList();		
		pendingList=new MessageList();
		recvPendingList = new MessageList();
	}
	
	public void init(ConfigProps props){
		this.props = props;
	}
	
	public void init(SessionProperties params) {
		
		String strIds=null;
		String strBools=null;
		
		String node = null;
		String seq = null;
		
		if(params.containsKey("ids"))
			strIds = params.getString("ids");
		if(params.containsKey("acts"))
			strBools = params.getString("acts");
		
		if(params.containsKey("node"))
			node = params.getString("node");
		if(params.containsKey("seq"))
			seq = params.getString("seq");
		
		String[] ids = null;
		boolean[] acts = null;
		
		StringTokenizer tok = new StringTokenizer(strIds,",");
		ids = new String[tok.countTokens()];
		for(int i = 0; tok.hasMoreTokens(); i++){
			ids[i] = tok.nextToken();
		}
		
		tok = new StringTokenizer(strBools,",");
		acts = new boolean[tok.countTokens()];
		for(int i = 0; tok.hasMoreTokens(); i++){
			acts[i] = tok.nextToken().equals("true");
		}
		
		for(int i=0; i<ids.length; i++)
			debug("ids-> "+i+" -> "+ids[i]);
		for(int i=0; i<acts.length; i++)
			debug("ids-> "+i+" -> "+acts[i]);
		
		props = new ConfigProps(ids,acts,node,seq);
	}
	
	public void handle(Event e) {
		
//		if(!(e instanceof TotalHybridTimer))
//			debug("EVENTO :: "+e.getClass() +" direction:: "+(e.getDir()==Direction.DOWN? "DOWN":"UP"));
		
		if (e instanceof ChannelInit)
			handleChannelInit(e);
//		else if(e instanceof InfoEvent)
//			handleInfoEvent((InfoEvent) e);
		else if(e instanceof View)
			handleView((View) e);
		else if(e instanceof BlockOk)
			handleBlockOk((BlockOk)e);
		else if(e instanceof TotalHybridTimer)
			handleTotalHybridTimer((TotalHybridTimer)e);
		else if (e instanceof UniformInfoEvent)
			handleUniformInfo((UniformInfoEvent) e);
		else if (e instanceof UniformTimer)
			handleUniformTimer((UniformTimer) e);
		else if(e instanceof GroupSendableEvent)	   
			handleGroupSendableEvent((GroupSendableEvent)e);
		else
			try {
				e.go();
			} catch (AppiaEventException e1) {
				e1.printStackTrace();
			}
	}
	
	private void handleChannelInit(Event e){
		channel=e.getChannel();
		try{			    
			e.go();
		}
		catch(AppiaEventException ex) {
			System.err.println("Error sending event");
		}
		try{
			TotalHybridTimer tht = new TotalHybridTimer(channel,Direction.DOWN,this,EventQualifier.ON);
			tht.go();
			
//			UniformTimer ut = new UniformTimer(UNIFORM_INFO_PERIOD,e.getChannel(),Direction.DOWN,this,EventQualifier.ON);
//			ut.go();
		}
		catch(AppiaException ex) {
			System.err.println("Error sending event");
		}
	}
	
	private boolean uTimerLaunched;
	
	private void handleView(View e) {
		blocked = false;	
		ls=e.ls;
		vs=e.vs;
		
		debug("Received view");
		cleanMessages();
		
		configuration=new Configuration(e.ls.failed.length);
		configuration.setState(e.vs,e.ls);
		
		unorderedList= new UnorderedTicketList(configuration.getSizeGroup());
		
		debug("New View. pid::"+ e.ls.my_rank);
		
		pid=e.ls.my_rank;
		
		if(props!=null && e.vs.view.length == props.nodeids.length) {
			debug("Using given configuration");
			for(int i=0;i!=props.nodeids.length;i++) {
				//		System.out.println("noid:: "+props.nodeids[i]);
				if(props.active[i])
					configuration.goingActive(e.vs.getRank(new Endpt(props.nodeids[i])));		
				else
					configuration.goingPassive(e.vs.getRank(new Endpt(props.nodeids[i])));
			}
			sequencer = e.vs.getRank(new Endpt(props.sequencer));
			debug("My sequencer is:: " +props.sequencer+" and is rank is:: "+sequencer);
			if(sequencer==pid) {
				state=ACTIVE;	
				debug("STATE = ACTIVE");
			}
			else {
				state=PASSIVE;
				debug("STATE = PASSIVE");
			}
		}
		else {	   
			sequencer=0;
			if(pid==0){
				state=ACTIVE;	
				debug("STATE = ACTIVE");
			}
			else {
				state=PASSIVE;
				debug("STATE = PASSIVE");
			}
		}
		
		lastTicket = new Ticket[e.vs.view.length];
		for (int i = 0; i < lastTicket.length; i++)
			lastTicket[i] = new Ticket(-1,-1,new MsgId(-1,-1));
		
		try {
			e.go();
		}
		catch(AppiaEventException ex) {
			System.err.println("Exception sending event");
		}
		
		if (!uTimerLaunched){
			UniformTimer ut;
			try {
				ut = new UniformTimer(UNIFORM_INFO_PERIOD,e.getChannel(),Direction.DOWN,this,EventQualifier.ON);
				ut.go();
				uTimerLaunched = true;
			} catch (AppiaEventException e1) {
				e1.printStackTrace();
			} catch (AppiaException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private void handleBlockOk(Event e) {
		MessageNode mn = null;
		
		while ((mn = pendingList.removesFirst()) != null) {
			TotalHybridHeader thh = new TotalHybridHeader(pid,++messageCount);
			setHeader(mn.getEvent(),thh);
			try {			    
				mn.getEvent().go();
				debug("sent pending msg");
			}
			catch(AppiaEventException ex) {
				System.err.println("Error sending event");
			}
		}
		try {			    
			e.go();
			debug("sent blockok");
		}
		catch(AppiaEventException ex) {
			System.err.println("Error sending event");
		}
		blocked = true;
	}
	
	private void handleGroupSendableEvent(GroupSendableEvent e){
		if (e.getDir()==Direction.UP) {
			handleGroupSendableEventUp(e);
		}
		else {
			handleGroupSendableEventDown(e);
		}
	}
	
	private void handleGroupSendableEventUp(GroupSendableEvent e){
		TotalHybridHeader header;
		Ticket t[];
		
		Message msg = e.getMessage();
		if (msg.popBoolean()) {
			Ticket[] uniformInfo = new Ticket[lastTicket.length];
			for (int i = uniformInfo.length; i > 0; i--)
				uniformInfo[i-1] = (Ticket) msg.popObject();
//			debug("["+ls.my_rank+"] Uniform table received from "+e.orig+": ");
//			if (debugOn)
//				for (int i = 0; i < uniformInfo.length; i++)
//					debug("RANK :"+i+" | LAST_TICKET: "+uniformInfo[i]);
			mergeUniformInfo(uniformInfo);
//			debug("Uniformity information table now is: ");
//			if (debugOn)
//				for (int i = 0; i < lastTicket.length; i++)
//					debug("RANK :"+i+" | LAST_TICKET: "+lastTicket[i]);
		}
		
		header=getHeader((GroupSendableEvent) e);
		t=header.getTickets();
		
		debug("["+pid+"] Received GroupSendableEvent (UP) from "+e.orig);
		
		if(header.getType()==TotalHybridHeader.PERIODIC)
			debug("["+pid+"] periodic msg");
		
		if(header.getType()==TotalHybridHeader.PENDING){
			MessageNode mn = new MessageNode(e,header);
			recvPendingList.insert(mn);
			return;
		}
		
		if(t!=null){
			for(int i=0;i!=t.length;i++){
				if(t[i].getTicketId() > maxticket)
					maxticket=t[i].getTicketId();
				unorderedList.insert(t[i]);
			}
			moveTickets();
		}
		
		if(props==null)
			rateSync();
		
		if(header.getType()==TotalHybridHeader.REASSING) {
			for(int i=header.getFrom()+1; i <= header.getTo(); i++){
				messageList.changeSequencer(header.getSource(),header.getSequence(),header.getSequencer());
			}		
		}
		else if(header.getType()!=TotalHybridHeader.PERIODIC) {
			messageList.insert(new MessageNode(e,header));
			GroupSendableEvent clone = null;
			try {
				clone = (GroupSendableEvent) e.cloneEvent();
				clone.setSourceSession(this);
				clone.init();
			} catch (CloneNotSupportedException ex) {
				ex.printStackTrace();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
            // nonius TODO: make compatible with jGCS
			//try {
				// deliver spontaneous event to application
				// SpontaneousEvent spontaneous = new SpontaneousEvent(channel,Direction.UP,this,clone);
				// spontaneous.go();
			//} catch (AppiaEventException e1) {
			//	e1.printStackTrace();
			//}
			
			if(state==ACTIVE){
//				debug("["+pid+"] calling issue tickets");
				issueTickets();
			}
		}
		
		deliverInOrder();
		deliverUniformMessage();
	}
	
	private void handleGroupSendableEventDown(GroupSendableEvent e){
		Ticket t;
		Ticket tList[];
		TotalHybridHeader thh;
		
		if(blocked)
			debug("TRYING TO SEND MSGS AFTER BLOCKOK:: "+e);
		
		Message msg;
		switch(state){
		
		case ACTIVE: 
			messageCount++;
			t=new Ticket(pid,ticketCounter,new MsgId(pid,messageCount));
			//	    debug("issuing ticket in (rank):: "+pid+" with seq:: "+messageCount);
			issuedList.insert(t);
			
			ticketCounter++;
			if(ticketCounter > maxticket)
				maxticket++;
			
			tList=issuedList.toArray();
			
			thh= new TotalHybridHeader(TotalHybridHeader.DATA,pid,messageCount,sequencer,tList);
			setHeader(e,thh);
			
			msg = e.getMessage();
			for (int i = 0; i < lastTicket.length; i++)
				msg.pushObject(lastTicket[i]);
			msg.pushBoolean(true);
			
			try{			    
				e.go();
			}
			catch(AppiaEventException ex) {
				System.err.println("Error sending event");
			}
			break;
			
		case PASSIVE:
			messageCount++;
			debug("["+pid+"] Sending message to order by ::" + sequencer);
			thh= new TotalHybridHeader(TotalHybridHeader.DATA,pid,messageCount,sequencer,null);	    
			setHeader(e,thh);
			
			msg = e.getMessage();
			for (int i = 0; i < lastTicket.length; i++)
				msg.pushObject(lastTicket[i]);
			msg.pushBoolean(true);
			
			try{			    
				e.go();
			}
			catch(AppiaEventException ex) {
				System.err.println("Error sending event");
			}
			break;
			
		default:
			debug("pending ::"+e);
		pendingList.insert(new MessageNode(e,null));
		break;
		}    
	}
	
	
	private void handleTotalHybridTimer(TotalHybridTimer e){
		//debug("TotalHybrid Timer(issued): " +issuedList.size());
		//System.out.println("["+pid+"] timer expired");
		//issuedList.printList();
		
		if(!blocked && state == ACTIVE && issuedList.size()> 0 && vs.view.length > 1){
			GroupSendableEvent gse=null;
			
			debug("["+pid+"] TotalHybrid Timer:: " +issuedList.size());
			
			try{
				gse= new GroupSendableEvent(channel,Direction.DOWN,this, configuration.getGroup(), configuration.getViewId());
				gse.init();
				
				Ticket tks[]=issuedList.toArray();
				
				TotalHybridHeader thh= new TotalHybridHeader(tks);
				
				setHeader(gse,thh);
				
				Message msg = gse.getMessage();
				//for (int i = 0; i < lastTicket.length; i++)
				//	msg.pushObject(lastTicket[i]);
				msg.pushBoolean(false);
				
				debug("["+pid+"] Sending periodic");
				gse.go();
			}
			catch(AppiaEventException ex) {
				System.err.println("Error sending event");
			}
		}
	}
	
	private long timeLastMsgSent;
	private static final long UNIFORM_INFO_PERIOD = 10;
	
	private void handleUniformTimer(UniformTimer timer) {
		//debug("Uniform timer expired. Now is: "+timer.getChannel().getTimeProvider().currentTimeMillis());
		if (!blocked && timer.getChannel().getTimeProvider().currentTimeMillis() - timeLastMsgSent >= UNIFORM_INFO_PERIOD) {
			debug("Last message sent was at time "+timeLastMsgSent+". Will send Uniform info!");
			UniformInfoEvent event = new UniformInfoEvent();
			
			//event.getObjectsMessage().pushLong(lastOrder);
			Message msg = event.getMessage();
			for (int i = 0; i < lastTicket.length; i++)
				msg.pushObject(lastTicket[i]);
			
			event.setChannel(timer.getChannel());
			event.setDir(Direction.DOWN);
			event.setSourceSession(this);
			try {
				event.init();
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Ticket[] lastTicket;
	
	private void handleUniformInfo(UniformInfoEvent event) {
		debug("Received UniformInfo from "+event.orig);
		//lastOrderList[event.orig] = event.getObjectsMessage().popLong();
		Message msg = event.getMessage();
		Ticket[] uniformInfo = new Ticket[lastTicket.length];
		for (int i = uniformInfo.length; i > 0; i--)
			uniformInfo[i-1] = (Ticket) msg.popObject();
		debug("["+ls.my_rank+"] Uniform table received from "+event.orig+": ");
		if (debugOn)
			for (int i = 0; i < uniformInfo.length; i++)
				debug("RANK :"+i+" | LAST_TICKET: "+uniformInfo[i]);
		mergeUniformInfo(uniformInfo);
		debug("Uniformity information table now is: ");
		if (debugOn)
			for (int i = 0; i < lastTicket.length; i++)
				debug("RANK :"+i+" | LAST_TICKET: "+lastTicket[i]);
		deliverUniformMessage();
	}
	
	/*
	 * takes the messages already total ordered and trys to deliver them
	 */
	private void deliverInOrder(){
		do{
			//orderedList.printList();
			//	    debug("ticket list size:: "+orderedList.size());
			MsgId mid=orderedList.getFirstMsgID();
			if(mid==null)
				return;

			//	    messageList.printList();
			//	    debug("");
			//	    debug("msg list size:: "+messageList.size());
			MessageNode mn= messageList.getMessageNode(mid.getSource(),mid.getSequence());
			if(mn==null)
				return;
			
			//messageList.removeMessageNode(mn);
			orderedList.remove(0);
			
			deliverRegularMessage(mn);
		}while(true);
		
	}
	
	/**
	 * delivers the messages to the upper layers.
	 */
	private void deliverRegularMessage(MessageNode mn){
		
		debug(" --->  DELIVERING OPTIMISTIC (REGULAR) message from:");
		
		if(mn.getHeader().getType()==TotalHybridHeader.DATA){
			GroupSendableEvent clone = null;
			try{
				clone = (GroupSendableEvent) ((GroupSendableEvent)mn.getEvent()).cloneEvent();
				clone.setSourceSession(this);
				clone.init();
				
	            // nonius TODO: make compatible with jGCS
//				RegularEvent re = new RegularEvent(channel,Direction.UP,this,clone);
//				re.go();
//				debug("Optimistically delivered event: "+mn.getEvent());
			}
			catch(AppiaEventException ex) {
				System.err.println("Error optimistacally sending event");
				ex.printStackTrace();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			
			if(pid==mn.getHeader().getSource()){
				lastDelivered=mn.getHeader().getSequence();
				if(state==CHGSEQ && lastDelivered==messageCount){
					sequencer=selectNewSequencer();
					state=PASSIVE;
					debug("STATE = PASSIVE");
					sendsPendingMsgs();
				}
			}
		}
		
		if(mn.getHeader().getType()==TotalHybridHeader.GOINGACTIVE){
			
			configuration.goingActive(mn.getHeader().getSource());//puts the process in the configuration as active
			debug("Delivering GOING ACTIVE to "+mn.getHeader().getSource()+" I am "+configuration.getMyRank());
			if(pid==mn.getHeader().getSource()){
				roleNumber++;
				lastDelivered=mn.getHeader().getSequence();
				state=ACTIVE;
				debug("STATE = ACTIVE");
				sequencer= pid;
				issueTickets();
				sendsPendingMsgs();
			}
		}
		
		if(mn.getHeader().getType()==TotalHybridHeader.GOINGPASSIVE){
			configuration.goingPassive(mn.getHeader().getSource());
			
			if(!configuration.existActive()) { 
				configuration.goingActive(mn.getHeader().getSource());
				if(pid==mn.getHeader().getSource()){//must exist at least one active process
					lastDelivered=messageCount;
					state=ACTIVE;
					debug("STATE = ACTIVE");
					sequencer= pid;
					issueTickets();
					sendsPendingMsgs();
				}
			}
			else {
				if(pid==mn.getHeader().getSource()) {
					
					roleNumber++;
					lastDelivered=mn.getHeader().getSequence();
					state=PASSIVE;
					debug("STATE = PASSIVE");
					sendsPendingMsgs();
				}
				if(state!=ACTIVE && !blocked){//maybe this is needed?????
					lastDelivered=messageCount;
					sequencer=selectNewSequencer();
					
					GroupSendableEvent rmsg=null;
					try{
						rmsg= new GroupSendableEvent(channel,Direction.DOWN,this,configuration.getGroup(), configuration.getViewId());
						rmsg.init();
					}
					catch(AppiaEventException ex) {
						System.err.println("Error creating event");
					}
					TotalHybridHeader header= new TotalHybridHeader(pid,lastDelivered,messageCount,sequencer);
					setHeader(rmsg,header);
					
					try{			
						rmsg.go();
					}
					catch(AppiaEventException ex) {
						System.err.println("Error sending event");
					}
				}
			}
			
		}
	}
	
	private void deliverUniformMessage() {
		do{
			//orderedList.printList();
			//	    debug("ticket list size:: "+orderedList.size());
			MsgId mid=finalOrderedList.getFirstMsgID();
			if(mid==null)
				return;

			Ticket nextTicket = finalOrderedList.getFirstTicket();
			if (!isUniform(nextTicket))
				return;
			
			//	    messageList.printList();
			//	    debug("");
			//	    debug("msg list size:: "+messageList.size());
			MessageNode mn= messageList.getMessageNode(mid.getSource(),mid.getSequence());
			if(mn==null)
				return;
			
			messageList.removeMessageNode(mn);
			//orderedList.remove(nextTicket);
			finalOrderedList.remove(0);
			
			//deliverFINALMessage(MessageNode mn)
			debug(" --->  DELIVERING FINAL message from:");
			if(mn.getHeader().getType()==TotalHybridHeader.DATA) {
				try {
					mn.getEvent().go();
					debug("Optimistically delivered event: "+mn.getEvent());
				}
				catch(AppiaEventException ex) {
					System.err.println("Error FINAL sending event");
				}
			}
		} while(true);
	}

	private boolean isUniform(Ticket ticket) {
		debug("["+ls.my_rank+"] Checking the following ticket is uniform:");
		debug("["+ls.my_rank+"] "+ticket);
		int seenCount = 0;
		for (int i = 0; i < lastTicket.length; i++) {
			debug("["+ls.my_rank+"] if "+lastTicket[i].getSource()+":"+lastTicket[i].getTicketId()+" >= "+
					ticket.getSource()+":"+ticket.getTicketId());
			if (lastTicket[i].compareTo(ticket) >= 0) {
				seenCount++;
				debug("["+ls.my_rank+"] seenCount = "+seenCount);
			}
		}
		if (seenCount > lastTicket.length/2) { // &&
			//messageList.getMessageNode(ticket.getMsgId().getSource(),ticket.getMsgId().getSequence()) != null) {
			debug("["+ls.my_rank+"] has majority!");
			return true;
		}
		debug("["+ls.my_rank+"] doesn't have majority!");
		return false;
	}

	private void mergeUniformInfo(Ticket[] table) {
		for (int i = 0; i < table.length; i++)
			if (table[i].compareTo(lastTicket[i]) > 0)
				lastTicket[i] = table[i];
	}
	
	private TotalHybridHeader getHeader(GroupSendableEvent e){
		
		//return (TotalHybridHeader)e.getObjectsMessage().pop();	
		return new TotalHybridHeader(e.getMessage(),e.orig);
	}
	
	private void setHeader(GroupSendableEvent e, TotalHybridHeader h){
		
		//e.getObjectsMessage().push((Object) h);
		h.writeHeader(e.getMessage());
	}
	
	/* move tickets in order from unordered list to ordered list*/
	private void moveTickets(){
		
			debug("<<<<<<<<<<<<<<<<<<<<<<<<<< LIST(before) >>>>>>>>>>>>>>>>>>>>>>>>>>>");
			debug("SIZE:: "+unorderedList.size());
			//System.out.println("["+pid+"] unorderedList:");
			//unorderedList.printList();
		
		while(true){
//			Ticket t=unorderedList.getNextTicket(props.active);//configuration.getActives());
			Ticket t=unorderedList.getNextTicket(configuration);//configuration.getActives());
			
			if(t!=null) {
				debug("["+ls.my_rank+"] NEW ORDERED TICKET INSERTED:");
				debug("["+ls.my_rank+"] "+t);
				finalOrderedList.insert(t);
				orderedList.insert(t);
				lastTicket[ls.my_rank] = t;
			}
			else
				break;	    
		}	
		
			debug(">>>>>>>>>>>>>>>>>>>>>>>>>> LIST(after) <<<<<<<<<<<<<<<<<<<<<<<<<<<");
			debug("SIZE:: "+unorderedList.size());
			//System.out.println("["+pid+"] unorderedList:");
			//unorderedList.printList();
			debug("END OF LISTINGS");
		
		
	}

	/*
	 does the rate syncronization
	 
	 */
	private void rateSync(){
		int fast=getFastest();
		
		if(fast!=-1){
			int t=(int) (delays[fast].getAverage()/transTimes[fast].getAverage());
			t+=maxticket;
			
			if(ticketCounter < t )
				ticketCounter=t;
			
		}
	}
	
	/*
	 creates the tickets
	 */
	private void issueTickets(){
		
		ListIterator<MessageNode> li= messageList.elements();
		
		while(li.hasNext()){
			MessageNode mn= (MessageNode)li.next();
			//debug("["+pid+"] Trying to issue ticket to msg: ");
			//mn.print();
			int pd= mn.getHeader().getSequencer();
			int source= mn.getHeader().getSource();
			int sequence= mn.getHeader().getSequence();
			
			
			if(source!=pid && pd==pid  && !(issuedList.exists(new MsgId(source,sequence)))){
				if(!mn.isIssued()){
					Ticket t=new Ticket(pid,ticketCounter,new MsgId(source,sequence));
					debug("["+pid+"] I'm the assigned sequencer... issuing ticket from "+source+" with seq "+sequence);
					ticketCounter++; //update Ti
					if(ticketCounter > maxticket)
						maxticket++;
					issuedList.insert(t);
					mn.issued();
					//unorderedList.insert(t);
					debug("["+pid+"] Ticket issued!");
				}
			}					 
		}
	}
	
	private int selectNewSequencer(){
		return getNearest();//think!!!
	}
	
	//return the nearest active process
	private int getNearest(){
		float minAverage=-1;
		int posMin=-1;
		
		
		if(configuration!=null)
			
			for(int i=0;i!=delays.length;i++){
				
				if(i!=pid && configuration.isActive(i)){
					if(minAverage==-1){
						minAverage=delays[i].getAverage();
						posMin=i;
					}
					else{
						if(minAverage> delays[i].getAverage()){
							minAverage=delays[i].getAverage();
							posMin=i;
						}
					}
				}
			}
		
		return posMin;
	}
	
	//return the rank of the process with the largest intermessage rate
	private int getFastest(){
		float bestAverage=-1;
		int posMin=-1;
		
		for(int i=0;i!=transTimes.length;i++){
			if(transTimes[i].getAverage() > bestAverage && configuration.isActive(i)){
				bestAverage=transTimes[i].getAverage();
				posMin=i;
			}
		}
		return posMin;
	}
	
	
	private void sendsPendingMsgs(){
		MessageNode mn;
		
		debug("Sending pending messages");
		while(true){
			mn=pendingList.removesFirst();
			if(mn==null)
				return;
			debug("calling handle of pending message");
			handle(mn.getEvent());
		}
	}
	
	//during view change delivers the pending messages
	private void cleanMessages(){
		MessageNode mn;
		
		while(!messageList.isEmpty()){
			mn=messageList.getNextMessage();
			
			if(mn.getHeader().getType()==TotalHybridHeader.DATA){
				
				try{
					mn.getEvent().go();
				}
				catch(AppiaEventException ex) {
					System.err.println("Error sending event");
				}
			}
		}
		
		while(!recvPendingList.isEmpty()){
			mn=recvPendingList.getNextMessage();
			
			if(mn.getHeader().getType()==TotalHybridHeader.PENDING){
				
				try{
					mn.getEvent().go();
					debug("delivering pending msg");
				}
				catch(AppiaEventException ex) {
					System.err.println("Error sending event");
				}
			}
			else
				debug("Strange msgs in recvPendingList");
		}
		
		orderedList= new ListOfTickets();
		finalOrderedList = new ListOfTickets();
		issuedList= new ListOfTickets();	
		
	}
	
//	private void handleInfoEvent(InfoEvent e){
//	transTimes=e.getTimes();
//	delays=e.getDelays();
//	
//	if(!blockedConfig){
//		if(state==ACTIVE)
//			handleInfoEventActive();
//		if(state==PASSIVE)
//			handleInfoEventPassive();
//	}
//	
//	
//	try{			    
//		e.go();
//	}
//	catch(AppiaEventException ex) {
//		System.err.println("Error sending event");
//	}
//}
//
//private void handleInfoEventActive(){
//	GroupSendableEvent gse=null;
//	
//	if(blocked){//MUST THINK ABOUT THIS
//		debug("Is blocked so ignores InfoEvent(ACTIVE)");
//		return;
//	}
//	
//	if(state == TOPASSIVE || state == TOACTIVE || state == CHGSEQ){
//		debug("already changing state do not change again");
//		return;
//	}
//	
//	if(timeToGo()==TOPASSIVE){
//		messageCount++;
//		try{
//			gse= new GroupSendableEvent(channel,Direction.DOWN,this,configuration.getGroup(), configuration.getViewId());
//			gse.init();
//		}
//		catch(AppiaEventException ex){
//			System.err.println("Error sending event");
//		}
//		Ticket t=new Ticket(pid,ticketCounter,new MsgId(pid,messageCount));
//		//	    debug("issuing ticket in (rank):: "+pid+" with seq:: "+messageCount);
//		
//		ticketCounter++;
//		if(maxticket < ticketCounter)
//			maxticket++;
//		
//		issuedList.insert(t);
//		
//		state=TOPASSIVE;
//		debug("STATE = TOPASSIVE");
//		
//		Ticket tks[]=issuedList.toArray();
//		
//		TotalHybridHeader thh= new TotalHybridHeader(TotalHybridHeader.GOINGPASSIVE,pid,messageCount,sequencer,tks);
//		
//		setHeader(gse,thh);
//		
//		try{
//			gse.go();
//		}
//		catch(AppiaEventException ex) {
//			System.err.println("Error sending event");
//		}
//	}
//}
//
//private void handleInfoEventPassive(){
//	if(blocked){
//		debug("Is blocked so ignores InfoEvent(PASSIVE)");
//		return;	
//	}   
//	
//	if(timeToGo()==CHGSEQ){
//		state=CHGSEQ;
//		debug("STATE = CHGSEQ");
//	}
//	
//	if(timeToGo()==TOACTIVE){
//		
//		GroupSendableEvent gse=null;
//		
//		messageCount++;
//		
//		state=TOACTIVE;
//		debug("STATE = TOACTIVE");
//		
//		
//		try{
//			gse= new GroupSendableEvent(channel,Direction.DOWN,this, configuration.getGroup(), configuration.getViewId());
//			gse.init();
//		}
//		catch(AppiaEventException ex){
//			System.err.println("Error sending event");
//		}
//		
//		TotalHybridHeader thh= new TotalHybridHeader(TotalHybridHeader.GOINGACTIVE,pid,messageCount,sequencer,null);
//		
//		setHeader(gse,thh);
//		try{
//			gse.go();
//			debug("SENT GOING ACTIVE");
//		}
//		catch(AppiaEventException ex) {
//			System.err.println("Error Sending event");
//		}
//		
//	}	      
//}
	
//	private int timeToGo(){
//	
//	int nearActive=getNearest();
//	
//	
//	if(nearActive==-1){
//		
//		return NOCHG;
//	}
//	
//	if(state==ACTIVE){
//		
//		if((delays[nearActive].getAverage() + transTimes[configuration.getMyRank()].getAverage()) >( 2 * delays[nearActive].getAverage()))
//			return TOPASSIVE;
//	}
//	if(state==PASSIVE){
//		if(delays[nearActive].getAverage() + transTimes[configuration.getMyRank()].getAverage() <= (2 * delays[nearActive].getAverage()))
//			return TOACTIVE;
//		
//		//if the actual server is not the best
//		if(nearActive!= sequencer)
//			return CHGSEQ;
//	}
//	
//	return NOCHG;
//	
//}
	private static boolean debugOn = false;
	
	private void debug(String msg){
		if (debugOn)
			System.out.println("THP debug message ::  "+msg);
	}
}

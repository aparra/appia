package net.sf.appia.protocols.total.hybrid;

import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.ViewState;

/**
 * Stores information about the group.
 * For example the current view and if the members are active or passive.
 */
public class Configuration{
	private int elements[];
	private ViewState viewState;
	private LocalState localState;
	
	public static final int ACTIVE=0;
	public static final int PASSIVE=1;
	
	/**
	 * Creates a new configuration.
	 * All the members are considered active.
	 * @param size the size of the group.
	 */
	public Configuration(int size){
		elements=new int[size];
		
		for(int i=0;i!=size;i++)
			elements[i] = PASSIVE;
		elements[0] = ACTIVE;
	}
	
	/**
	 * Sets a certain member as active.
	 * @param rank the member to be changed to active.
	 */
	public void goingActive(int rank){
		elements[rank] = ACTIVE;
	}
	
	/**
	 * Sets a certain member as passive.
	 * @param rank the member to be changed to passive.
	 */
	public void goingPassive(int rank){
		elements[rank]=PASSIVE;
	}
	
	/**
	 * Verifies if a exists at least an active member of the group.
	 * @return true if exists and false otherwise.
	 */
	public boolean existActive(){
		for(int i=0; i != elements.length; i++)
			if(elements[i] == ACTIVE)
				return true;
		return false;
	}
	
	/**
	 * Verifies is a certain meber is active
	 * @param rank member.
	 */
	public boolean isActive(int rank){
		return (elements[rank] == ACTIVE);
	}
	
	/**
	 * Sets the current view.
	 * @param vs The current ViewState.
	 * @param ls The current LocalState.
	 */
	public void setState(ViewState vs, LocalState ls){
		viewState = vs;
		localState = ls;
	}
	
	/**
	 * Returns the size of the group.
	 * @return The size of the group.
	 */
	public int getSizeGroup(){
		return elements.length;
	}
	
	/**
	 * Get the current group.
	 * @return The group.
	 */
	public Group getGroup(){
		return viewState.group;
	}
	
	/**
	 * Get the ViewId
	 * @return The current ViewId
	 */
	public ViewID getViewId(){
		return viewState.id;
	}
	
	/**
	 * Get the current ViewState.
	 * @return ViewState.
	 */
	public ViewState getViewState(){
		return viewState;
	}
	
	/**
	 * Get the rank of this node.
	 * @return The rank.
	 */
	public int getMyRank(){
		return localState.my_rank;
	}
	
	/**
	 * verifies if a certain endpoint exists in the current view
	 * @param endpt The Endpt to be searched
	 * @return True if the endpoint exists and false otherwise.
	 */
	public boolean exists(Endpt endpt){
		
		for(int i=0;i!=elements.length;i++){
			if( endpt.equals(viewState.view[i]))
				return true;
		}
		return false;
	}
	
	/**
	 * Return the number of active members in the current group
	 * @return Active members in the group.
	 */
//	public int getActives(){
//		int actives=0;
//		for(int i=0;i!=elements.length;i++){
//			if(elements[i]==ACTIVE)
//				actives++;
//		}
//		return actives;
//	}
	
	public boolean[] getActives() {
		boolean actives[] = new boolean[elements.length];
		for (int i = 0; i < elements.length; i++)
			if (elements[i] == ACTIVE)
				actives[i] = true;
		return actives;
	}
	
	/**
	 * Given an Endpt returns the rank in the group.
	 * @param endpt the Endpt to be searched.
	 * @return The rank of this Endpt or -1 if none is found.
	 */
	public int getRank(Endpt endpt){
		for(int i=0;i!=elements.length;i++){
			if( endpt.equals(viewState.view[i]))
				return i;
		}
		return -1;
	}
	
	/**
	 * Prints the current configuration.
	 * For debugging.
	 */
	public void printConfiguration(){
		String type;
		
		System.out.println("......CONFIGURATION......");
		for(int i=0;i!=elements.length;i++){
			if(elements[i]==ACTIVE)
				type="ACTIVE";
			else
				type="PASSIVE";
			
			System.out.print("  rank "+i+"  tipo->" +type);
		}
		System.out.println();
	}
}

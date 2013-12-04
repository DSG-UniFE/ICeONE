/**
 * 
 */
package core.disService;

import java.util.ArrayList;
import java.util.List;

import core.DTNHost;
import core.Message;

/**
 * Class that represent the HELLO Messages exchanged by DisService-based routers
 * @author Alex
 *
 */
public class DisServiceHelloMessage extends Message {

	/**
	 * @param from The node generating the message
	 * @param id String that carries the ID of the Hello Message
	 * @param size an int to indicate the size of the Hello Message
	 */
	
	private ArrayList<String> msgIDs;
	private ArrayList<Integer> nodeSubscriptionsList;
	
	public DisServiceHelloMessage(DTNHost from, String id, int size, List<String> receivedMsgIDs,
									List<Integer> nodeSubscriptions) {
		super(from, null, id, size);
		
		this.msgIDs = new ArrayList<String>(receivedMsgIDs.size());
		for (String msgID : receivedMsgIDs) {
			this.msgIDs.add(msgID);
		}
		this.nodeSubscriptionsList = new ArrayList<Integer>(nodeSubscriptions.size());
		for (Integer subID : nodeSubscriptions) {
			this.nodeSubscriptionsList.add(subID);
		}
	}

	public ArrayList<String> getMsgIDs() {
		return msgIDs;
	}

	public ArrayList<Integer> getNodeSubscriptionsList() {
		return nodeSubscriptionsList;
	}
	

	/**
	 * Deep copies message data from other message. If new fields are
	 * introduced to this class, most likely they should be copied here too
	 * (unless done in constructor).
	 * @param m The PrioritizedMessage from where the data is copied
	 */
	@SuppressWarnings("unchecked")
	protected void copyFrom(DisServiceHelloMessage disServiceHelloMessage) {
		super.copyFrom(disServiceHelloMessage);
		this.msgIDs = (ArrayList<String>) disServiceHelloMessage.msgIDs.clone();
		this.nodeSubscriptionsList = (ArrayList<Integer>) disServiceHelloMessage.nodeSubscriptionsList.clone();
	}
	
	@Override
	public DisServiceHelloMessage replicate() {
		DisServiceHelloMessage disServiceHelloMessage = new DisServiceHelloMessage(getFrom(), getId(), getSize(),
																					msgIDs, nodeSubscriptionsList);
		disServiceHelloMessage.copyFrom(this);
		return disServiceHelloMessage;
	}

}

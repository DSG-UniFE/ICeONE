package messageForwardingOrderManager;

import java.util.ArrayList;
import java.util.List;

import core.Message;
import core.MessageQueueManager;
import core.Settings;
import strategies.MessagePrioritizationStrategy;

/**
 * {@link FIFOManager} does not modify the message lists orders,
 * thereby returning the messages in the same order in which they
 * are passed to the orderMessageListForForwarding() method.
 * 
 * @author Alex
 *
 */
public class FIFOManager extends MessageForwardingOrderManager {

	public FIFOManager(Settings s, MessageQueueManager queueManager,
						MessagePrioritizationStrategy orderingStrategy) {
		super(MessageForwardingManagerImplementation.FIFO_MANAGER, queueManager, orderingStrategy);
	}

	public FIFOManager(FIFOManager fifoManager) {
		super(fifoManager);
	}

	@Override
	public List<Message> orderMessageListForForwarding(List<Message> inputList) {
		if (inputList == null) {
			return new ArrayList<Message>(0);
		}
		
		List<Message> orderedList = new ArrayList<Message>(inputList);
		return orderedList;
	}

	@Override
	public MessageForwardingOrderManager replicate() {
		return new FIFOManager(this);
	}

}
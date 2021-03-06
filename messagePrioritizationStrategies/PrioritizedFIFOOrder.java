/**
 * 
 */
package messagePrioritizationStrategies;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import core.Connection;
import core.Message;
import core.SimError;
import core.Tuple;

/**
 * @author Alessandro Morelli
 *
 */
public class PrioritizedFIFOOrder extends MessageCachingPrioritizationStrategy {

	static PrioritizedFIFOOrder singletonInstance = null;
	static Comparator<Message> comparator = new Comparator<Message>() {
		/** Compares two tuples by their messages' receiving time, priority,
		 *  and the number of times they were forwarded */
		@Override
		public int compare(Message m1, Message m2) {
			double diff = m1.getReceiveTime() - m2.getReceiveTime();
			int pDiff = m1.getPriority() - m2.getPriority();
			
			if ((pDiff == 0) && (diff == 0)) {
				return 0;
			}
			
			return (pDiff < 0 ? 1 : (pDiff > 0 ? -1 : (diff < 0 ? -1 : 1)));
		}
	};
	
	static Comparator<Object> reverseOrderComparator = new Comparator<Object>() {
		/** Compares two tuples by their messages' receiving time, priority,
		 *  and the number of times they were forwarded */
		@Override
		@SuppressWarnings(value = "unchecked")
		public int compare(Object o1, Object o2) {
			double diff;
			Message m1, m2;
			
			if (o1 instanceof Tuple) {
				m1 = ((Tuple<Message, Connection>)o1).getKey();
				m2 = ((Tuple<Message, Connection>)o2).getKey();
			}
			else if (o1 instanceof Message) {
				m1 = (Message) o1;
				m2 = (Message) o2;
			}
			else {
				throw new SimError("Invalid type of objects in the list");
			}

			diff = m1.getReceiveTime() - m2.getReceiveTime();
			int pDiff = m1.getPriority() - m2.getPriority();
			if ((pDiff == 0) && (diff == 0)) {
				return 0;
			}
			
			// Lowest priority, first-received messages go first
			return (pDiff < 0 ? -1 : (pDiff > 0 ? 1 : (diff < 0 ? -1 : 1)));
		}
	};
	
	private PrioritizedFIFOOrder() {
		super(MessageCachingPrioritizationStrategy.CachingPrioritizationMode.Prioritized_FIFO);
	}
	
	/* (non-Javadoc)
	 * @see messagePrioritizationStrategies.MessageForwardingOrderStrategy#MessageProcessingOrder(java.util.List)
	 */
	static PrioritizedFIFOOrder getOrderingInstance() {
		if (singletonInstance == null) {
			singletonInstance = new PrioritizedFIFOOrder();
		}
		
		return singletonInstance;
	}

	@Override
	public void sortList(List<Message> inputList) {
		Collections.sort(inputList, PrioritizedFIFOOrder.comparator);
	}

	@Override
	public void sortListInReverseOrder(List<Message> inputList) {
		Collections.sort(inputList, PrioritizedFIFOOrder.reverseOrderComparator);
	}
	
	@Override
	public int comparatorMethod(Message m1, Message m2) {
		return PrioritizedFIFOOrder.comparator.compare(m1, m2);
	}
	
}

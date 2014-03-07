/**
 * 
 */
package strategies;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import core.Connection;
import core.Message;
import core.SimError;
import core.Tuple;

/**
 * @author Alex
 *
 */
public class PrioritizedFIFOForwardingOrder extends MessageForwardingOrderStrategy {

	static PrioritizedFIFOForwardingOrder singletonInstance = null;
	static Comparator<Object> comparator = new Comparator<Object>() {
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
			int pDiff = m1.getPriority().ordinal() - m2.getPriority().ordinal();
			if ((pDiff == 0) && (diff == 0)) {
				return 0;
			}
			
			return (pDiff < 0 ? 1 : (pDiff > 0 ? -1 : (diff < 0 ? -1 : 1)));
		}
	};
	
	private PrioritizedFIFOForwardingOrder() {}
	
	/* (non-Javadoc)
	 * @see strategies.MessageForwardingOrderStrategy#MessageProcessingOrder(java.util.List)
	 */
	static PrioritizedFIFOForwardingOrder getForwardingOrderInstance() {
		if (singletonInstance == null) {
			singletonInstance = new PrioritizedFIFOForwardingOrder();
		}
		
		return singletonInstance;
	}
	
	@Override
	public <T> List<T> MessageProcessingOrder(List<T> inputList) {
		Collections.sort(inputList, PrioritizedFIFOForwardingOrder.comparator);		
		return inputList;
	}
	
	@Override
	public int ComparatorMethod(Message m1, Message m2) {
		return PrioritizedFIFOForwardingOrder.comparator.compare(m1, m2);
	}

}
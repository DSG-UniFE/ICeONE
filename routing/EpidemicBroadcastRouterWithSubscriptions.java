/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.uncommons.maths.random.MersenneTwisterRNG;

import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.ParseException;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.NetworkInterface;
import core.SeedGeneratorHelper;
import core.Settings;
import core.SimError;
import core.disService.PublisherSubscriber;
import core.disService.SubscriptionListManager;

/**
 * Epidemic message router with drop-oldest buffer and broadcast transmissions.
 */
public class EpidemicBroadcastRouterWithSubscriptions
	extends BroadcastEnabledRouter implements PublisherSubscriber {
	
	/** identifier for the sending probability ({@value})*/
	public static final String MESSAGE_DISSEMINATION_PROBABILITY_S = "msgDissProbability";
	/** identifier for the binary-mode setting ({@value})*/
	public static final String MESSAGE_ACCEPT_PROBABILITY_S = "msgAcceptProbability";
	
	private final double sendProbability;
	private final double receiveProbability;

	private SubscriptionListManager nodeSubscriptions;
	private final SubscriptionBasedDisseminationMode pubSubDisseminationMode;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EpidemicBroadcastRouterWithSubscriptions(Settings s) {
		super(s);
		
		try {
			this.nodeSubscriptions = new SubscriptionListManager(s);
		} catch (ParseException e) {
			throw new SimError("Error parsing configuration file");
		}
		
		int subpubDisMode = s.contains(PublisherSubscriber.SUBSCRIPTION_BASED_DISSEMINATION_MODE_S) ?
				s.getInt(PublisherSubscriber.SUBSCRIPTION_BASED_DISSEMINATION_MODE_S) :
				SubscriptionBasedDisseminationMode.FLEXIBLE.ordinal();
		if ((subpubDisMode < 0) || (subpubDisMode > SubscriptionBasedDisseminationMode.values().length)) {
			throw new SimError(PublisherSubscriber.SUBSCRIPTION_BASED_DISSEMINATION_MODE_S +
								" value " + "in the settings file is out of range");
		}
		
		this.pubSubDisseminationMode = SubscriptionBasedDisseminationMode.values()[subpubDisMode];
		if (this.pubSubDisseminationMode == SubscriptionBasedDisseminationMode.SEMI_POROUS) {
			this.sendProbability = s.contains(MESSAGE_DISSEMINATION_PROBABILITY_S) ? s.getDouble(MESSAGE_DISSEMINATION_PROBABILITY_S) : 0.5;
			this.receiveProbability = s.contains(MESSAGE_ACCEPT_PROBABILITY_S) ? s.getDouble(MESSAGE_ACCEPT_PROBABILITY_S) : 0.5;
		}
		else {
			this.sendProbability = (this.pubSubDisseminationMode ==
									SubscriptionBasedDisseminationMode.FLEXIBLE) ? 1.0 : 0.0;
			this.receiveProbability = (this.pubSubDisseminationMode ==
										SubscriptionBasedDisseminationMode.FLEXIBLE) ? 1.0 : 0.0;
		}
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected EpidemicBroadcastRouterWithSubscriptions(EpidemicBroadcastRouterWithSubscriptions r) {
		super(r);
		
		this.sendProbability = r.sendProbability;
		this.receiveProbability = r.receiveProbability;
		this.pubSubDisseminationMode = r.pubSubDisseminationMode;
		this.nodeSubscriptions = r.nodeSubscriptions.replicate();
	}

	@Override
	public EpidemicBroadcastRouterWithSubscriptions replicate() {
		return new EpidemicBroadcastRouterWithSubscriptions(this);
	}
	
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canBeginNewTransfer()) {
			return; // transferring, don't try other connections yet
		}
		
		/* First, try to send the messages that can be delivered to their
		 * final recipient; this is consistent with any dissemination policy.
		 */
		while (canBeginNewTransfer() && (exchangeDeliverableMessages() != null));
		
		/* Then, try to send messages that cannot be delivered directly to hosts
		 * that subscribed their interest to them. The chosen dissemination
		 * policy will affect the set of messages that can be sent this way. */
		if (canBeginNewTransfer()) {
			List<NetworkInterface> idleInterfaces = getIdleNetworkInterfaces();
			Collections.shuffle(idleInterfaces, RANDOM_GENERATOR);
			/* try to send those messages over all idle interfaces */
			for (NetworkInterface idleInterface : idleInterfaces) {
				List<Message> availableMessages = sortListOfMessagesForForwarding(
						getMessagesAccordingToDisseminationPolicy(idleInterface));
				for (Message m : availableMessages) {
					if (BROADCAST_OK == tryBroadcastOneMessage(m, idleInterface)) {
						// Move on to the remaining idle network interfaces.
						break;
					}
				}
			}
		}
	}

	/**
	 * Returns whether a message can be delivered to the specified host
	 * or not, according to the EpidemicRouter policy. Said policy requires
	 * that Epidemic Routers keep a list of messages recently sent to each
	 * neighbor, and that they exchange the list of the messages they have
	 * with other nodes before they proceed with the dissemination phase.
	 * This phase is called Anti-entropy session in the literature.
	 * The algorithm hereby written is a simplification, as it does not
	 * require hosts to exchange the lists described above.
	 * @param m the {@link Message} to deliver.
	 * @param to the {@link DTNHost} to which deliver the Message m.
	 * @return {@code true} if the message can be delivered to the
	 * specified host, or {@code false} otherwise.
	 */
	@Override
	protected boolean shouldDeliverMessageToHost(Message m, DTNHost to) {
		return !to.getRouter().hasReceivedMessage(m.getID());
	}
	
	/**
	 * It applies the chosen dissemination policy at the moment
	 * that the reception of a new message is complete.
	 */
	@Override
	public Message messageTransferred(String id, Connection con) {
		Integer subID = (Integer) con.getMessage().getProperty(SUBSCRIPTION_MESSAGE_PROPERTY_KEY);
		if (!getSubscriptionList().getSubscriptionList().contains(subID)) {
			if (RANDOM_GENERATOR.nextDouble() > receiveProbability) {
				// remove message from receiving interface and refuse message
				Message incoming = retrieveTransferredMessageFromInterface(id, con);
				if (incoming == null) {
					// reception was interfered --> no need to apply dissemination mode
					return null;
				}
				
				String message = null;
				switch (pubSubDisseminationMode) {
				case FLEXIBLE:
					throw new SimError("message refuse despite FLEXIBLE strategy was set");
				case STRICT:
					message = "strict dissemination mode";
					break;
				case SEMI_POROUS:
					message = "message discaded due to a semi-porous strategy. The probability" +
							  " of discarding messages is " + (1 - receiveProbability);
					break;
				}
				for (MessageListener ml : mListeners) {
					ml.messageDeleted(incoming, getHost(), true, message);
				}
				
				return null;
			}
		}
		
		return super.messageTransferred(id, con);
	}

	/**
	 * It selects all the messages available for transmission, according
	 * to the selected dissemination policy, which are not being sent.
	 */
	private List<Message> getMessagesAccordingToDisseminationPolicy(NetworkInterface idleInterface) {
		List<Message> availableMessages = new ArrayList<Message>();
		for (Message msg : getMessageList()) {
			boolean isBeingSent = false;
			for (NetworkInterface ni : getHost().getInterfaces()) {
				isBeingSent |= ni.isSendingMessage(msg.getID());
			}
			/* If no interface is sending the message and the dissemination
			 * policy chosen allows it, we add it to the list of messages
			 * available for sending. */
			if (!isBeingSent && shouldDeliverMessageToNeighbors(msg, idleInterface) &&
				(RANDOM_GENERATOR.nextDouble() <= sendProbability)) {
				availableMessages.add(msg);
			}
		}
		
		return availableMessages;
	}

	@Override
	public SubscriptionListManager getSubscriptionList() {
		return nodeSubscriptions;
	}

	@Override
	public int generateRandomSubID() {
		return nodeSubscriptions.getRandomSubscriptionFromList();
	}

	@Override
	protected boolean isMessageDestination(Message aMessage) {
		Integer messageSubID = (Integer) aMessage.getProperty(SUBSCRIPTION_MESSAGE_PROPERTY_KEY);
		
		return getSubscriptionList().containsSubscriptionID(messageSubID);
	}

	@Override
	protected boolean isMessageDestination(Message aMessage, DTNHost dest) {
		return dest.getRouter().isMessageDestination(aMessage);
	}
}
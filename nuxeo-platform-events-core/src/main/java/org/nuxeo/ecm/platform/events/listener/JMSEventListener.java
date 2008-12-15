/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: JMSEventListener.java 1274 2006-07-22 00:31:30Z janguenot $
 */

package org.nuxeo.ecm.platform.events.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.platform.events.DocumentMessageFactory;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.events.api.impl.EventMessageImpl;

/**
 * JMS Core Event Listener.
 * <p>
 * This is a bridge from Nuxeo Core events to JMS.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class JMSEventListener extends AbstractEventListener implements
        AsynchronousEventListener {

    private static final Log log = LogFactory.getLog(JMSEventListener.class);

    private static DocumentMessageProducer service;

    protected static final Map<String, List<CoreEvent>> eventsStack = new Hashtable<String, List<CoreEvent>>();

    /**
     * Returns the Nuxeo Core producer service singleton.
     *
     * <p>
     * We assume the service is on the same node as Nuxeo Core.
     * </p>
     *
     * @return the core document message producer service.
     */
    protected DocumentMessageProducer getProducerService() {
        if (service == null) {
            service = DocumentMessageProducerBusinessDelegate.getLocalDocumentMessageProducer();
        }
        return service;
    }

    /**
     * Core event notification.
     * <p>
     * Gets core events and transmits them to a JMS as a DocumentMessage.
     *
     * @param coreEvent instance fired at core layer
     */
    @Override
    public void notifyEvent(CoreEvent coreEvent) {

        // Is it a document saved session event ?
        boolean flush = DocumentEventTypes.SESSION_SAVED.equals(coreEvent.getEventId());

        // If not attached document model then not bound to a session : we
        // notify directly.

        if (coreEvent.getInfo().get(EventMessage.BLOCK_JMS_PRODUCING) != null
                && (Boolean) coreEvent.getInfo().get(EventMessage.BLOCK_JMS_PRODUCING)) {
            log.debug(
                    "JMS forwarding disabled for event " + coreEvent.getEventId());
            return;
        }

        if (!flush && !(coreEvent.getSource() instanceof DocumentModel)) {
            log.debug("Not a document centric message. Forwarding directly on JMS.");
            sendEventToJMS(coreEvent);
        } else {
            DocumentModel source = (DocumentModel) coreEvent.getSource();
            if (source != null && source.getContextData(EventMessage.BLOCK_JMS_PRODUCING) != null
                    && (Boolean) source.getContextData(EventMessage.BLOCK_JMS_PRODUCING)) {
                log.debug("JMS forwarding disabled for events on doc "
                        + source.getRef().toString() + "... skipping.");
                return;
            }
            if (flush) {
                String sid = (String) coreEvent.getInfo().get(
                        CoreEventConstants.SESSION_ID);
                List<CoreEvent> stack = getStackForSessionId(sid);
                if (stack == null) {
                    // this should never happend
                    log.error("Received a Save event without known SessionID");
                } else {
                    log.debug("Flushing event stack for session " + sid);

                    // Here, variable only used by logging. Since this is not
                    // synchronized here it might be not correct.
                    int nbEvt = stack.size();
                    long t = System.currentTimeMillis();

                    sendEventToJMS(stack);
                    /*
                     * for (CoreEvent stackedEvent : stack) {
                     * sendEventToJMS(stackedEvent); }
                     */
                    log.debug(nbEvt + " events flushed in "
                            + (System.currentTimeMillis() - t) + " ms");

                    synchronized (eventsStack) {
                        eventsStack.remove(sid);
                    }
                }
            } else {
                log.debug("Document centric event. Let's stack it until session is saved");
                stackEvent(coreEvent);
            }
        }
    }

    protected static List<CoreEvent> getStackForSessionId(String sid) {
        List<CoreEvent> stack = eventsStack.get(sid);
        if (stack == null) {
            stack = new ArrayList<CoreEvent>();
            synchronized (eventsStack) {
                eventsStack.put(sid, stack);
            }
        }
        return stack;
    }

    private void stackEvent(CoreEvent coreEvent) {
        String sid = (String) coreEvent.getInfo().get(CoreEventConstants.SESSION_ID);

        if (sid == null) {
            log.error("received an Document related event witout session id");
            sendEventToJMS(coreEvent);
        } else {
            List<CoreEvent> stack = getStackForSessionId(sid);
            // Stack is a copy then no need to synchronized.
            stack.add(coreEvent);
        }
    }

    private static EventMessage getJMSMessage(CoreEvent coreEvent) {
        Object source = coreEvent.getSource();

        EventMessage message = null;
        if (source instanceof Document) {
            try {
                message = DocumentMessageFactory.createDocumentMessage(
                        (Document) source, coreEvent);
            } catch (DocumentException e) {
                log.error("An error occurred trying to notify", e);
            }
        } else if (source instanceof DocumentModel) {
            message = new DocumentMessageImpl((DocumentModel) source, coreEvent);
        } else {
            message = new EventMessageImpl(coreEvent);
        }
        return message;
    }

    private static void markDuplicatedMessages(List<EventMessage> eventMessages) {
        Map<String, List<DocumentMessage>> messagesToCheck = new HashMap<String, List<DocumentMessage>>();

        for (EventMessage message : eventMessages) {
            // If there are several messages relative to the same document
            // withing the transaction,
            // all (n-1) messages are marked as duplicated
            // this avoids doing indexing several times for nothing
            // for now only CREATE and UPDATE messages are taken into account
            if (DocumentEventTypes.DOCUMENT_CREATED.equals(message.getEventId())
                    || DocumentEventTypes.DOCUMENT_UPDATED.equals(message.getEventId())) {
                if (message instanceof DocumentMessage) {
                    DocumentMessage docMessage = (DocumentMessage) message;
                    if (docMessage.getRef() != null) {
                        String docRef = docMessage.getRef().toString();
                        if (!messagesToCheck.containsKey(docRef)) {
                            messagesToCheck.put(docRef,
                                    new ArrayList<DocumentMessage>());
                            messagesToCheck.get(docRef).add(docMessage);
                        } else {
                            List<DocumentMessage> stackedMessages = messagesToCheck.get(docRef);
                            stackedMessages.get(stackedMessages.size() - 1).getEventInfo().put(
                                    EventMessage.DUPLICATED, true);
                            stackedMessages.add(docMessage);
                        }
                    }
                }
            }
        }
    }

    protected void sendEventToJMS(List<CoreEvent> coreEvents) {
        List<EventMessage> eventMessages = new ArrayList<EventMessage>();

        for (CoreEvent coreEvent : coreEvents) {
            eventMessages.add(getJMSMessage(coreEvent));
        }

        // mark duplicated messages before sending
        markDuplicatedMessages(eventMessages);

        DocumentMessageProducer service = getProducerService();
        if (service != null) {
            service.produceEventMessages(eventMessages);
        } else {
            log.error("JMSDocumentMessageProducer service not found !");
        }
    }

    private void sendEventToJMS(CoreEvent coreEvent) {
        EventMessage evt = getJMSMessage(coreEvent);
        DocumentMessageProducer service = getProducerService();
        if (service != null) {
            service.produce(evt);
        } else {
            log.error("JMSDocumentMessageProducer service not found !");
        }
    }

}

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
 * $Id: JMSEventListener.java 1274 2006-07-22 00:31:30Z bstefanescu $
 */

package org.nuxeo.ecm.core.jms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.api.operation.OperationEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.core.listener.OperationEventFactory;

/**
 * JMS Core Event Listener.
 * <p>
 * This is a listener designed to notify client applications like Apogee.
 *
 * @author <a href="mailto:ja@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JMSEventListener extends AbstractEventListener implements
        AsynchronousEventListener {

    private static final Log log = LogFactory.getLog(JMSEventListener.class);

    protected static final Map<String, List<CoreEvent>> eventsStack = new Hashtable<String, List<CoreEvent>>();

    /**
     * @deprecated this is only used for compatibility
     */
    @Deprecated
    static final String BLOCK_JMS_PRODUCING = "BLOCK_JMS_PRODUCING";


    @Override
    public void operationStarted(Operation<?> cmd) throws Exception {
        // do nothing
    }

    @Override
    public void operationTerminated(Operation<?> cmd) throws Exception {
        OperationEvent event = OperationEventFactory.createEvent(cmd);
        if (event != null) {
            CoreEventPublisher.getInstance().publish(event, event.getId());
        }
    }

    /**
     * Core event notification.
     * <p>
     * Gets core events and transmits them to a JMS as a DocumentMessage.
     *
     * @param coreEvent instance fired at core layer
     */
    @Override
    public void handleEvent(CoreEvent coreEvent) {
        //TODO: this should be refactored after operation are integrated into core
        Operation<?> cmd = Operation.getCurrent();
        if (cmd != null) { // we ignore events inside a command
            return;
        }

        Map<String,?> info  =coreEvent.getInfo();
        // avoid to send blocked events
        if (info != null && coreEvent.getInfo().get(BLOCK_JMS_PRODUCING) != null
                && (Boolean) coreEvent.getInfo().get(BLOCK_JMS_PRODUCING)) {
            log.debug(
                    "JMS forwarding disabled for event " + coreEvent.getEventId());
            return;
        }

        // Is it a document saved session event ? - this should be refactored to use commands
        boolean flush = DocumentEventTypes.SESSION_SAVED.equals(coreEvent.getEventId());

        if (!flush && !(coreEvent.getSource() instanceof DocumentModel)) {
            log.debug("Not a document centric message. Avoid fwd.");
            //sendEventToJMS(coreEvent);
        } else {
            DocumentModel source = (DocumentModel) coreEvent.getSource();
            if (source!=null && source.getContextData(BLOCK_JMS_PRODUCING) != null
                    && (Boolean) source.getContextData(BLOCK_JMS_PRODUCING)) {
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

                    try {
                        sendEventToJMS(stack);
                        /*
                         * for (CoreEvent stackedEvent : stack) {
                         * sendEventToJMS(stackedEvent); }
                         */
                        log.debug(nbEvt + " events flushed in "
                                + (System.currentTimeMillis() - t) + " ms");
                    } catch (JMSException e) {
                        e.printStackTrace();
                    } finally {
                        synchronized (eventsStack) {
                            eventsStack.remove(sid);
                        }
                    }
                }
            } else {
                log.debug("Document centric event. Let's stack it until session is saved");
                stackEvent(coreEvent);
            }
        }
    }

    //TODO this code should be refactored -> the synchronized block does nothing because of hashtable!
    // Should synchronize both get and set
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

    //TODO this code should be refactored and synchronized -> Stack is not a copy and need to be synchronized...
    private static void stackEvent(CoreEvent coreEvent) {
        String sid = (String) coreEvent.getInfo().get(
                CoreEventConstants.SESSION_ID);

        if (sid == null) {
            log.error("received an Document related event witout session id");
            sendEventToJMS(coreEvent);
        } else {
            List<CoreEvent> stack = getStackForSessionId(sid);
            // Stack is a copy then no need to synchronized.
            stack.add(coreEvent);
        }
    }

    protected static void sendEventToJMS(List<CoreEvent> coreEvents) throws JMSException {
        // Must be declared as Serializable
        ArrayList<OperationEvent> cmdEvents = new ArrayList<OperationEvent>();
        Set<DocumentRef> checkedDocs = new HashSet<DocumentRef>();

        for (CoreEvent coreEvent : coreEvents) {
            // remove duplicates TODO improve this
            String id = coreEvent.getEventId();
            if (DocumentEventTypes.DOCUMENT_CREATED.equals(id)
                    || DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(id)
                    || DocumentEventTypes.DOCUMENT_UPDATED.equals(id)) {
                // stacked events are doc centric events
                DocumentModel doc = (DocumentModel) coreEvent.getSource();
                if (checkedDocs.contains(doc.getRef())) {
                    continue;
                }
                checkedDocs.add(doc.getRef());
            }
            OperationEvent event = OperationEventFactory.createEvent(coreEvent);
            if(event != null) {
                CoreEventPublisher.getInstance().publish(event, event.getId());
            }
        }

    }

    private static void sendEventToJMS(CoreEvent coreEvent) {
        OperationEvent event = OperationEventFactory.createEvent(coreEvent);
        if (event != null) {
            try {
                CoreEventPublisher.getInstance().publish(event, event.getId());
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

}

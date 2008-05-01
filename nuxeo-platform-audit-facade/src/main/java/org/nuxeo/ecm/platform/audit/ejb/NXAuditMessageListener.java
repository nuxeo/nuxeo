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
 * $Id:NXAuditMessageListener.java 1583 2006-08-04 10:26:40Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.ejb;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.NXAudit;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.runtime.api.Framework;

/**
 * Message Driven Bean listening for events, responsible for logs persistency.
 * <p>
 * It does:
 * <ul>
 * <li>Get the message from the topic/NXCoreMessages</li>
 * <li>Filter out the event based on events we are interested in</li>
 * <li>Create and persist a LogEntryImpl entity bean</li>
 * </ul>
 *
 * @author <a mailto="ja@nuxeo.com">Julien Anguenot</a>
 */

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NXPMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue = "java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@TransactionManagement(TransactionManagementType.CONTAINER)
public class NXAuditMessageListener implements MessageListener {

    private static final Log log = LogFactory.getLog(NXAuditMessageListener.class);

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        log.debug("onMessage");
        try {

            final Serializable obj = ((ObjectMessage) message).getObject();
            if (!(obj instanceof DocumentMessage)) {
                log.debug("Not a DocumentMessage instance embedded ignoring.");
                return;
            }

            DocumentMessage doc = (DocumentMessage) obj;

            String eventId = doc.getEventId();
            log.debug("Received a message with eventId : " + eventId);

            if (isInterestedInEvent(eventId)) {
                createLogEntry(doc);
            } else {
                log.debug("Not interested about event with id=" + eventId);
            }
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    /**
     * Creates a log entry from the DocumentMessage instance.
     * <p>
     * It creates an entity bean which will be persisted using the
     * EntityManager.
     *
     * @param doc the DocumentMessage instance
     * @return the new generated log entry id
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private static long createLogEntry(DocumentMessage doc) throws AuditException {
        NXAuditEvents service = NXAudit.getNXAuditEventsService();
        LogEntry logEntry = service.computeLogEntry(doc);
        Logs logService;
        try {
            logService = Framework.getService(Logs.class);
        } catch (Exception e) {
            throw new AuditException(e);
        }
        logService.addLogEntries(Arrays.asList(logEntry));
        return logEntry.getId();
    }

    /**
     * Checks if we are interested about logging this event.
     *
     * @param eventId the actual event identifier
     * @return true / false whether or not we are interested in this event
     */
    private boolean isInterestedInEvent(String eventId) {
        NXAuditEvents service = NXAudit.getNXAuditEventsService();
        Set<String> eventIds = service.getAuditableEventNames();
        return eventIds.contains(eventId);
    }

}

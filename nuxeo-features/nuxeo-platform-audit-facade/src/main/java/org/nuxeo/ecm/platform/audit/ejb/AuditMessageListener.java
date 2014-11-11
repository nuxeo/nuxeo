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

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.audit.AuditMessageHandler;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.JMSConstant;

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
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = JMSConstant.NUXEO_MESSAGE_TYPE
                + " IN ('"
                + JMSConstant.DOCUMENT_MESSAGE
                + "','"
                + JMSConstant.EVENT_MESSAGE + "')") })
public class AuditMessageListener implements MessageListener {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(AuditMessageListener.class);

    @PersistenceContext(unitName = "NXAudit")
    private EntityManager em;

    private static String repositoryName(DocumentMessage message) {
        String repositoryName = message.getRepositoryName();
        return repositoryName == null ? "default" : repositoryName;
    }

    protected class MessageLogger extends UnrestrictedSessionRunner {

        protected final DocumentMessage message;

        MessageLogger(DocumentMessage message) {
            super(repositoryName(message));
            this.message = message;
        }

        @Override
        public void run() throws ClientException {
            AuditMessageHandler handler = new AuditMessageHandler();
            handler.onDocumentMessage(em, session, message);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        try {
            Object obj = ((ObjectMessage) message).getObject();
            if (!(obj instanceof DocumentMessage)) {
                return;
            }
            new MessageLogger((DocumentMessage)obj).runUnrestricted();

        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

}

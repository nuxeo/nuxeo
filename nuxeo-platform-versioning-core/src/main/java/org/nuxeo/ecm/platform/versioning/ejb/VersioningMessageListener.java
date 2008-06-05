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

package org.nuxeo.ecm.platform.versioning.ejb;

import java.io.Serializable;
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
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.JMSConstant;
import org.nuxeo.ecm.platform.versioning.api.WFDocVersioning;
import org.nuxeo.ecm.platform.versioning.wfintf.WFVersioningPolicyProvider;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowEventTypes;
import org.nuxeo.runtime.api.Framework;

/**
 * Message Driven Bean listening for events, taking appropriate action regarding
 * versioning info for the document.
 * <p>
 * It does:
 * <ul>
 * <li>Get the message from the topic/NXCoreMessages</li>
 * <li>Filter out the event based on events we are interested in</li>
 * <li>Set versioning info (like WF versioning policy)</li>
 * </ul>
 *
 * @author <a mailto="dm@nuxeo.com">Dragos Mihalache</a>
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
                + JMSConstant.EVENT_MESSAGE
                + "') AND "
                + JMSConstant.NUXEO_EVENT_ID
                + " IN ('"
                + WorkflowEventTypes.WORKFLOW_STARTED
                + "','"
                + WorkflowEventTypes.WORKFLOW_ABANDONED
                + "','"
                + WorkflowEventTypes.WORKFLOW_ENDED + "')") })
@TransactionManagement(TransactionManagementType.CONTAINER)
public class VersioningMessageListener implements MessageListener {

    private static final Log log = LogFactory.getLog(VersioningMessageListener.class);

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {
        try {
            Object obj = ((ObjectMessage) message).getObject();
            if (!(obj instanceof DocumentMessage))
                return;
            DocumentMessage doc = (DocumentMessage) obj;

            String eventId = doc.getEventId();
            log.debug("Received a message with event id: " + eventId);

            boolean wfInProgress = isWfInProgress(eventId);
            setWFVersioningPolicy(doc, wfInProgress);
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    /**
     * Retrieves the versioning policy from the workflow for the given document
     * and set it as a system property.
     * <p>
     *
     * @param wfInProgress
     *
     * @param doc the DocumentMessage instance
     * @throws LoginException
     */
    private void setWFVersioningPolicy(DocumentMessage doc, boolean wfInProgress)
            throws Exception {

        final DocumentRef docRef = doc.getRef();
        if (null == docRef) {
            // DocumentModel has not yet been given any Ref, ignore it
            log.debug(String.format(
                    "document '%s' has null reference (on event %s): ignored",
                    doc.getTitle(), doc.getEventId()));
            return;
        }
        String repositoryName = doc.getRepositoryName();
        if (repositoryName == null) {
            // DocumentModel has not yet been persisted to any repository,
            // ignore it
            log.debug(String.format(
                    "document '%s' has null repositoryName (on event %s): ignored",
                    doc.getTitle(), doc.getEventId()));
            return;
        }
        String versioningPolicy = WFVersioningPolicyProvider.getVersioningPolicyFor(doc.getRef());

        log.debug("versioning policy: " + versioningPolicy);

        // open a new core session: this bean is a managed bean with is own
        // core context to work asynchronously and should not interfere with the
        // original session context (that furthermore might have been closed in
        // the mean time)
        LoginContext loginContext = Framework.login();
        RepositoryManager repositoryMgr = Framework.getService(RepositoryManager.class);
        Repository repository = repositoryMgr.getRepository(repositoryName);
        CoreSession coreSession = repository.open();

        try {
            coreSession.setDocumentSystemProp(doc.getRef(),
                    WFDocVersioning.SYSTEM_PROPERTY_WF_IN_PROGRESS,
                    wfInProgress);
            coreSession.setDocumentSystemProp(doc.getRef(),
                    WFDocVersioning.SYSTEM_PROPERTY_NAME_WF_OPTION,
                    versioningPolicy);
        } catch (Exception e) {
            log.error("Cannot set versioning policy: " + e.getMessage(), e);
            // TODO maybe throw exception
        } finally {
            CoreInstance.getInstance().close(coreSession);
            loginContext.logout();
        }
    }

    private boolean isWfInProgress(String eventId) {
        return eventId.equals(WorkflowEventTypes.WORKFLOW_STARTED);
    }
}

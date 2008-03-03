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
 * $Id:DocumentEventListenerBean.java 1583 2006-08-04 10:26:40Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentRelationBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;

/**
 * Message Driven Bean listening for events from the core.
 *
 * @author <a mailto="ja@nuxeo.com">Julien Anguenot</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NXPMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue="java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")})
@TransactionManagement(TransactionManagementType.CONTAINER)
public class DocumentEventListenerBean implements MessageListener {

    private static final Log log = LogFactory.getLog(DocumentEventListenerBean.class);

    public static final String LIFECYCLE_TRANSITION_EVENT = "lifecycle_transition_event";
    public static final String LIFECYCLE_OPTION_NAME_TO = "to";

    private final WorkflowDocumentRelationBusinessDelegate wDocBusinessDelegate;

    public DocumentEventListenerBean() {
        wDocBusinessDelegate = new WorkflowDocumentRelationBusinessDelegate();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void onMessage(Message message) {

        try {

            final Serializable obj = ((ObjectMessage) message).getObject();
            if (!(obj instanceof DocumentMessage)) {
                log.debug("Not a DocumentMessage instance embedded ignoring.");
                return;
            }

            DocumentMessage doc = (DocumentMessage) obj;
            DocumentRef docRef = doc.getRef();

            String eventId = doc.getEventId();
            log.debug("Received a message with eventId : " + eventId);

            WorkflowDocumentRelationManager wDoc = getWDocBean();
            WAPI wapi = getWAPIBean();

            // When a document is removed lets cleanup the references.
            if (eventId.equals(DocumentEventTypes.ABOUT_TO_REMOVE) || (eventId.equals(LIFECYCLE_TRANSITION_EVENT) && "deleted".equals(doc.getEventInfo().get(LIFECYCLE_OPTION_NAME_TO)))) {

                for (String pid : wDoc.getWorkflowInstanceIdsFor(docRef)) {
                    log.debug("Remove id=" + docRef + "and pid=" + pid
                            + " reference");

                    // :XXX: We need to find a way here to know if we should
                    // shoot the process when no documents are bound to it
                    // anymore.
                    // For the moment, we shoot the process instances hooked up
                    // to the document if the process doesn't have any other
                    // associated
                    // process

                    DocumentRef[] docRefs;
                    try {
                        docRefs = wDoc.getDocumentRefsFor(pid);
                    } catch (Exception e) {
                        docRefs = new DocumentRef[0];
                    }

                    // Current doc to be remove +1
                    if (docRefs.length < 2) {

                        boolean exist;

                        try {
                            exist = wapi.getProcessInstanceById(pid, null) != null;
                        } catch (WMWorkflowException we) {
                            exist = false;
                        }

                        if (exist) {
                            wapi.terminateProcessInstance(pid);
                            log.debug("End workflow instance=" + pid
                                    + " because no document are bound with "
                                    + "it anymore => Nuxeo5 builtin...");
                        }

                    }

                    wDoc.deleteDocumentWorkflowRef(docRef, pid);
                }
            }

        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    private WorkflowDocumentRelationManager getWDocBean()
            throws Exception {
        return wDocBusinessDelegate.getWorkflowDocument();
    }

    private WAPI getWAPIBean() throws Exception {
        return WAPIBusinessDelegate.getWAPI();
    }

}

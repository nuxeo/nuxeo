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
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager.listener;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.JMSConstant;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NXPMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue = "java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "messageSelector",
                propertyValue = JMSConstant.NUXEO_MESSAGE_TYPE + " IN ('" + JMSConstant.DOCUMENT_MESSAGE + "','"
                    + JMSConstant.EVENT_MESSAGE + "') AND " + JMSConstant.NUXEO_EVENT_ID + " IN ('"
                    + DocumentEventTypes.DOCUMENT_CREATED +"','" + DocumentEventTypes.DOCUMENT_UPDATED + "')") })
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class UploadFileListener implements MessageListener {

    private static final Log log = LogFactory.getLog(UploadFileListener.class);

    public static final String DUPLICATED_FILE = "duplicatedFile";

    private transient CoreSession session;

    private transient FileManager fileManager;

    private LoginContext loginCtx;

    private String currentRepositoryName;

    protected DocumentMessageProducer getMessageProducer() throws Exception {
        return DocumentMessageProducerBusinessDelegate.getRemoteDocumentMessageProducer();
    }

    private void login() throws Exception {
        loginCtx = Framework.login();
    }

    private void logout() throws Exception {
        if (loginCtx != null) {
            loginCtx.logout();
        }
    }

    private FileManager getFileManagerService() throws ClientException {
        if (fileManager == null) {
            fileManager = Framework.getRuntime().getService(FileManager.class);
        }
        if (fileManager == null) {
            log.error("Unable to get FileManager runtime service");
            throw new ClientException(
                    "Unable to get FileManager runtime service");
        }
        return fileManager;
    }

    protected CoreSession getRepositorySession(String repositoryName) {
        // return cached session
        if (currentRepositoryName != null
                && repositoryName.equals(currentRepositoryName)
                && session != null) {
            return session;
        }

        try {
            session = Framework.getService(RepositoryManager.class).getRepository(
                    repositoryName).open();
            log.debug("CommentManager connected to ECM");
        } catch (Exception e) {
            log.error("failed to connect to ECM platform", e);
            throw new RuntimeException(e);
        }
        return session;
    }

    /**
     *
     * @param message
     */
    public void onMessage(Message message) {

        try {
            Object obj = ((ObjectMessage)message).getObject();
            if(!(obj instanceof DocumentMessage)) {
                return;
            }
            DocumentMessage doc = (DocumentMessage) obj;
            login();

            // Check if unicity Service is enabled
            fileManager = getFileManagerService();
            if (!fileManager.isUnicityEnabled()) {
                return;
            }
            Boolean duplicatedMessage = (Boolean) doc.getEventInfo().get(
                    EventMessage.DUPLICATED);
            if (duplicatedMessage != null && duplicatedMessage == true) {
                return;
            }

            currentRepositoryName = doc.getRepositoryName();
            session = getRepositorySession(currentRepositoryName);

            DocumentModel newDoc = session.getDocument(doc.getRef());

            if (newDoc.isProxy()) {
                return;
            }
            List<String> xpathFields = fileManager.getFields();
            for (String field : xpathFields) {

                Blob blob = (Blob) newDoc.getPropertyValue(field);
                if (blob == null) {
                    continue;
                }
                String digest = blob.getDigest();

                List<DocumentLocation> existingDocuments = fileManager.findExistingDocumentWithFile(
                        newDoc.getPathAsString(), digest,
                        session.getPrincipal());

                if (!existingDocuments.isEmpty()) {
                    Iterator<DocumentLocation> existingDocumentsIterator = existingDocuments.iterator();
                    while (existingDocumentsIterator.hasNext()) {
                        if (existingDocumentsIterator.next().getDocRef() == newDoc.getRef()) {
                            existingDocumentsIterator.remove();
                        }
                    }
                    log.debug("Existing Documents[" + existingDocuments.size()
                            + "]");
                    raiseDuplicatedFileEvent(session.getPrincipal(), doc,
                            existingDocuments.toArray());
                }
            }
        } catch (PropertyException pe) {
            log.debug("Requested Field isn't on the Document.");
            log.debug(pe.getClass().toString(), pe);
        } catch (Exception e) {
            log.error(e.getClass().toString(), e);

        } finally {
            try {
                if (session!=null)
                {
                    CoreInstance.getInstance().close(session);
                }
                logout();
            } catch (Throwable e) {
                log.error("Error during cleanup", e);
            }
        }
    }

    private void raiseDuplicatedFileEvent(Principal principal,
            DocumentModel newDoc, Object[] existingDocuments) {
        Map<String, Serializable> options = new HashMap<String, Serializable>();

        // Name of the current repository
        options.put(CoreEventConstants.REPOSITORY_NAME,
                newDoc.getRepositoryName());

        // Add the session ID
        options.put(CoreEventConstants.SESSION_ID, newDoc.getSessionId());

        // Duplicated File list
        options.put("duplicatedDocLocation", existingDocuments);

        // Default category
        String category = DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY;

        CoreEvent event = new CoreEventImpl(DUPLICATED_FILE, newDoc, options,
                principal, category, null);

        DocumentMessage msg = new DocumentMessageImpl(newDoc, event);
        msg.feed(event);
        DocumentMessageProducer producer = null;
        try {
            producer = getMessageProducer();
        } catch (Exception e) {
            log.error("Unable to get MessageProducer : " + e.getMessage());
        }

        if (producer != null) {
            log.debug("Send JMS message for event="
                    + DocumentEventTypes.SUBSCRIPTION_ASSIGNED);
            producer.produce(msg);
        } else {
            log.error("Impossible to notify core events !");
        }
    }

}

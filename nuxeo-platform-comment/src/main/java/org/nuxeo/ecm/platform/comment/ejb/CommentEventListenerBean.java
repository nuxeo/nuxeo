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

package org.nuxeo.ecm.platform.comment.ejb;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.security.SecurityDomain;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.JMSConstant;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Message-Driven Bean listening for events from the core. This class listens
 * for documents removals and when a document is deleted, also remove associated
 * relations and comments. If the document is a comment, only remove the
 * relation.
 *
 * @author <a mailto="glefter@nuxeo.com">George Lefter</a>
 */

@SecurityDomain("nuxeo-ecm")
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
                + " = '"
                + DocumentEventTypes.ABOUT_TO_REMOVE + "'") })
public class CommentEventListenerBean implements MessageListener {

    private static final Log log = LogFactory.getLog(CommentEventListenerBean.class);

    RelationManager relationManager;

    CommentServiceConfig config;

    private CoreSession session;

    private final String currentRepositoryName = null;

    protected CoreSession getRepositorySession(String repositoryName) {
        // return cached session
        if (currentRepositoryName != null
                && repositoryName.equals(currentRepositoryName)
                && session != null) {
            return session;
        }

        try {
            log.debug("trying to connect to ECM platform");
            Framework.login();
            session = Framework.getService(RepositoryManager.class).getRepository(
                    repositoryName).open();
            log.debug("CommentManager connected to ECM");
        } catch (Exception e) {
            log.error("failed to connect to ECM platform", e);
            throw new RuntimeException(e);
        }
        return session;
    }

    @PostConstruct
    public void init() {
        config = CommentServiceHelper.getCommentService().getConfig();
        try {
            relationManager = Framework.getService(RelationManager.class);
        } catch (Exception e) {
            log.error("Could not get relation manager");
        }
    }

    public void onMessage(Message message) {
        try {
            Object obj = ((ObjectMessage) message).getObject();
            if (!(obj instanceof DocumentMessage)) {
                return;
            }
            DocumentMessage doc = (DocumentMessage) obj;
            onDocumentRemoved(doc);
            onCommentRemoved(doc);

        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    private void onDocumentRemoved(DocumentMessage docMessage)
            throws ClientException {

        Resource documentRes = relationManager.getResource(
                config.documentNamespace, docMessage);
        if (documentRes == null) {
            log.error("Could not adapt document model to relation resource ; "
                    + "check the service relation adapters configuration");
            return;
        }
        Statement pattern = new StatementImpl(null, null, documentRes);
        List<Statement> statementList = relationManager.getStatements(
                config.graphName, pattern);

        CoreSession mySession = getRepositorySession(docMessage.getRepositoryName());

        // remove comments
        for (Statement stmt : statementList) {

            QNameResource resource = (QNameResource) stmt.getSubject();
            String commentId = resource.getLocalName();
            DocumentModel docModel = (DocumentModel) relationManager.getResourceRepresentation(
                    config.commentNamespace, resource);

            if (docModel != null) {
                try {
                    mySession.removeDocument(docModel.getRef());
                    log.debug("comment removal succeded for id: " + commentId);
                } catch (Exception e) {
                    log.error("comment removal failed", e);
                }
            } else {
                log.warn("comment not found: id=" + commentId);
            }
        }
        mySession.save();
        // remove relations
        relationManager.remove(config.graphName, statementList);
    }

    private void onCommentRemoved(DocumentMessage docModel)
            throws ClientException {
        Resource commentRes = relationManager.getResource(
                config.commentNamespace, docModel);
        if (commentRes == null) {
            log.error("Could not adapt document model to relation resource ; "
                    + "check the service relation adapters configuration");
            return;
        }
        Statement pattern = new StatementImpl(commentRes, null, null);
        List<Statement> statementList = relationManager.getStatements(
                config.graphName, pattern);
        relationManager.remove(config.graphName, statementList);
    }

}

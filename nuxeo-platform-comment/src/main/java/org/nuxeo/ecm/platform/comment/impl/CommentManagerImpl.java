/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.comment.impl;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.comment.api.CommentConstants;
import org.nuxeo.ecm.platform.comment.api.CommentConverter;
import org.nuxeo.ecm.platform.comment.api.CommentEvents;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducerException;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class CommentManagerImpl implements CommentManager {

    private static final Log log = LogFactory.getLog(CommentManagerImpl.class);

    final SimpleDateFormat timeFormat = new SimpleDateFormat("dd-HHmmss.S");

    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");

    final CommentServiceConfig config;

    final CommentConverter commentConverter;

    private CoreSession session;

    private String currentRepositoryName;

    public CommentManagerImpl(CommentServiceConfig config) {
        this.config = config;
        commentConverter = config.getCommentConverter();
    }

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
            RepositoryManager manager = Framework.getService(RepositoryManager.class);
            session = manager.getRepository(repositoryName).open();
            log.debug("CommentManager connected to ECM");
            currentRepositoryName = repositoryName;
        } catch (Exception e) {
            log.error("failed to connect to ECM platform", e);
            throw new RuntimeException(e);
        }
        return session;
    }

    private static RelationManager getRelationManager() throws Exception {
        return Framework.getService(RelationManager.class);
    }

    @SuppressWarnings("unchecked")
    public List<DocumentModel> getComments(DocumentModel docModel)
            throws ClientException {
        RelationManager relationManager;
        try {
            relationManager = getRelationManager();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        Resource docResource = relationManager.getResource(
                config.documentNamespace, docModel);
        if (docResource == null) {
            throw new ClientException(
                    "Could not adapt document model to relation resource ; "
                            + "check the service relation adapters configuration");
        }

        // FIXME AT: why no filter on the predicate?
        Statement pattern = new StatementImpl(null, null, docResource);
        List<Statement> statementList = relationManager.getStatements(
                config.graphName, pattern);
        // XXX AT: BBB for when repository name was not included in the resource
        // uri
        Resource oldDocResource = new QNameResourceImpl(
                config.documentNamespace, docModel.getId());
        Statement oldPattern = new StatementImpl(null, null, oldDocResource);
        statementList.addAll(relationManager.getStatements(config.graphName,
                oldPattern));

        List<DocumentModel> commentList = new ArrayList<DocumentModel>();
        for (Statement stmt : statementList) {
            QNameResourceImpl subject = (QNameResourceImpl) stmt.getSubject();

            DocumentModel commentDocModel = null;
            try {
                commentDocModel = (DocumentModel) relationManager.getResourceRepresentation(
                        config.commentNamespace, subject);
            } catch (Exception e) {
                log.error("failed to retrieve commentDocModel from relations");
            }
            if (commentDocModel == null) {
                // XXX AT: maybe user cannot see the comment
                log.error("Could not adapt comment relation subject to a document "
                        + "model; check the service relation adapters configuration");
                continue;
            }
            commentList.add(commentDocModel);
        }
        return commentList;
    }

    public DocumentModel createComment(DocumentModel docModel, String comment,
            String author) throws ClientException {
        DocumentModel commentDM = getRepositorySession(
                docModel.getRepositoryName()).createDocumentModel("Comment");
        commentDM.setProperty("comment", "text", comment);
        commentDM.setProperty("comment", "author", author);
        commentDM.setProperty("comment", "creationDate", Calendar.getInstance());

        return createComment(docModel, commentDM);
    }

    public DocumentModel createComment(DocumentModel docModel, String comment)
            throws ClientException {
        String author = getCurrentUser(docModel);
        return createComment(docModel, comment, author);
    }

    /**
     * If the author property on comment is not set, retrieve the author name
     * from the session
     *
     * @param docModel The document model that holds the session id
     * @param comment The comment to update
     * @throws ClientException
     */
    private static String updateAuthor(DocumentModel docModel, DocumentModel comment)
            throws ClientException {
        // update the author if not set
        String author = (String) comment.getProperty("comment", "author");
        if (author == null) {
            log.debug("deprecated use of createComment: the client should set the author property on document");
            author = getCurrentUser(docModel);
            comment.setProperty("comment", "author", author);
        }
        return author;
    }

    public DocumentModel createComment(DocumentModel docModel,
            DocumentModel comment) throws ClientException {
        String author = updateAuthor(docModel, comment);
        DocumentModel createdComment;
        try {

            createdComment = createCommentDocModel(docModel, comment);

            RelationManager relationManager = getRelationManager();
            List<Statement> statementList = new ArrayList<Statement>();

            Resource commentRes = relationManager.getResource(
                    config.commentNamespace, createdComment);

            Resource documentRes = relationManager.getResource(
                    config.documentNamespace, docModel);

            if (commentRes == null || documentRes == null) {
                throw new ClientException(
                        "Could not adapt document model to relation resource ; "
                                + "check the service relation adapters configuration");
            }

            Resource predicateRes = new ResourceImpl(config.predicateNamespace);

            Statement stmt = new StatementImpl(commentRes, predicateRes,
                    documentRes);
            statementList.add(stmt);
            relationManager.add(config.graphName, statementList);
        } catch (Exception e) {
            log.error("failed to create comment", e);
            throw new ClientException("failed to create comment", e);
        }

        NuxeoPrincipal principal = new NuxeoPrincipalImpl(author);
        notifyEvent(docModel, CommentEvents.COMMENT_ADDED, null,
                createdComment, principal);

        return createdComment;
    }

    private DocumentModel createCommentDocModel(DocumentModel docModel,
            DocumentModel comment) throws ClientException {

        updateAuthor(docModel, comment);

        String[] pathList = getCommentPathList(comment);

        String domainPath = docModel.getPath().segment(0);
        CoreSession mySession = getRepositorySession(docModel.getRepositoryName());
        if (mySession == null) {
            return null;
        }

        // TODO GR upgrade this code. It can't work if current user
        // doesn't have admin rights
        DocumentModel parent = mySession.getDocument(new PathRef(domainPath));
        for (String name : pathList) {
            String pathStr = parent.getPathAsString();
            DocumentRef ref = new PathRef(pathStr, name);
            if (mySession.exists(ref)) {
                parent = mySession.getDocument(ref);
            } else {
                DocumentModel dm = mySession.createDocumentModel(pathStr, name,
                        "HiddenFolder");
                dm.setProperty("dublincore", "title", name);
                dm.setProperty("dublincore", "description", "");
                dm.setProperty("dublincore", "created", Calendar.getInstance());
                dm = mySession.createDocument(dm);
                setFolderPermissions(dm);

                mySession.save();
                parent = dm;
            }
        }

        String pathStr = parent.getPathAsString();
        String commentName = getCommentName(docModel, comment);

        CommentConverter converter = config.getCommentConverter();
        DocumentModel commentDocModel = mySession.createDocumentModel(pathStr,
                IdUtils.generateId(commentName), comment.getType());
        converter.updateDocumentModel(commentDocModel, comment);
        commentDocModel.setProperty("dublincore", "title", commentName);
        commentDocModel = mySession.createDocument(commentDocModel);
        setCommentPermissions(commentDocModel);
        log.debug("created comment with id=" + commentDocModel.getId());

        mySession.save();

        return commentDocModel;
    }

    private static void notifyEvent(DocumentModel docModel, String eventType,
            DocumentModel parent, DocumentModel child, NuxeoPrincipal principal)
            throws ClientException {
        DocumentMessageProducer producer;
        try {
            producer = DocumentMessageProducerBusinessDelegate.getRemoteDocumentMessageProducer();
        } catch (DocumentMessageProducerException e) {
            throw new ClientException("notify event failed", e);
        }

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        if (parent != null) {
            props.put(CommentConstants.PARENT_COMMENT, parent);
        }
        props.put(CommentConstants.COMMENT, child);
        props.put(CommentConstants.COMMENT_TEXT, (String) child.getProperty(
                "comment", "text"));

        CoreEvent event = new CoreEventImpl(eventType, docModel, props,
                principal, CommentConstants.EVENT_COMMENT_CATEGORY, eventType);

        DocumentMessage msg = new DocumentMessageImpl(docModel, event);
        producer.produce(msg);

        // send also a synchronous Seam message so the CommentManagerActionBean
        // can rebuild its list
        // Events.instance().raiseEvent(eventType, docModel);
    }

    private static void setFolderPermissions(DocumentModel dm) {
        ACP acp = new ACPImpl();
        ACE grantAddChildren = new ACE("members",
                SecurityConstants.ADD_CHILDREN, true);
        ACE grantRemoveChildren = new ACE("members",
                SecurityConstants.REMOVE_CHILDREN, true);
        ACE grantRemove = new ACE("members", SecurityConstants.REMOVE, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { grantAddChildren, grantRemoveChildren,
                grantRemove });
        acp.addACL(acl);
        dm.setACP(acp, true);
    }

    private static void setCommentPermissions(DocumentModel dm) {
        ACP acp = new ACPImpl();
        ACE grantRead = new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ, true);
        ACE grantRemove = new ACE("members", SecurityConstants.REMOVE, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { grantRead,grantRemove });
        acp.addACL(acl);
        dm.setACP(acp, true);
    }

    private String[] getCommentPathList(DocumentModel comment) {
        String[] pathList = new String[2];
        pathList[0] = "Comments";
        pathList[1] = dateFormat.format(getCommentTimeStamp(comment));
        return pathList;
    }

    private static CoreSession getUserSession(String sid) {
        return CoreInstance.getInstance().getSession(sid);
    }

    /**
     *
     * @deprecated if the caller is remote, we cannot obtain the session
     */
    @Deprecated
    private static String getCurrentUser(DocumentModel target) throws ClientException {
        String sid = target.getSessionId();
        CoreSession userSession = getUserSession(sid);
        if (userSession == null) {
            throw new ClientException(
                    "userSession is null, do not invoke this method when the user is not local");
        }
        return userSession.getPrincipal().getName();
    }

    private String getCommentName(DocumentModel target, DocumentModel comment)
            throws ClientException {
        String author = (String) comment.getProperty("comment", "author");
        if (author == null) {
            author = getCurrentUser(target);
        }
        Date creationDate = getCommentTimeStamp(comment);
        return "COMMENT-" + author + '-'
                + timeFormat.format(creationDate.getTime());
    }

    private static Date getCommentTimeStamp(DocumentModel comment) {
        Calendar creationDate = (Calendar) comment.getProperty("dublincore",
                "created");
        if (creationDate == null) {
            creationDate = Calendar.getInstance();
        }
        return creationDate.getTime();
    }

    public void deleteComment(DocumentModel docModel, DocumentModel comment)
            throws ClientException {
        try {

            CoreSession mySession = getRepositorySession(docModel.getRepositoryName());
            if (mySession == null) {
                throw new ClientException(
                        "Unable to acess repository for comment: "
                                + comment.getId());
            }
            DocumentRef ref = new IdRef(comment.getId());
            if (!mySession.exists(ref)) {
                throw new ClientException("Comment Document does not exist: "
                        + comment.getId());
            }

            NuxeoPrincipal author = getAuthor(comment);
            mySession.removeDocument(ref);
            mySession.save();

            notifyEvent(docModel, CommentEvents.COMMENT_REMOVED, null, comment,
                    author);

        } catch (Throwable e) {
            log.error("failed to delete comment", e);
            throw new ClientException("failed to delete comment", e);
        }
    }

    public DocumentModel createComment(DocumentModel docModel,
            DocumentModel parent, DocumentModel child) throws ClientException {
        String author = updateAuthor(docModel, child);
        String commentId = parent.getId();
        DocumentModel parentDocModel = getRepositorySession(
                docModel.getRepositoryName()).getDocument(new IdRef(commentId));
        DocumentModel newComment = createComment(parentDocModel, child);

        NuxeoPrincipal principal = new NuxeoPrincipalImpl(author);
        notifyEvent(docModel, CommentEvents.COMMENT_ADDED, parent, newComment,
                principal);
        return newComment;
    }

    private static NuxeoPrincipal getAuthor(DocumentModel docModel)
            throws ClientException {
        String[] contributors = (String[]) docModel.getProperty("dublincore",
                "contributors");
        return new NuxeoPrincipalImpl(contributors[0]);
    }

    public List<DocumentModel> getComments(DocumentModel docModel,
            DocumentModel parent) throws ClientException {
        String commentId = parent.getId();
        DocumentModel parentDocModel = getRepositorySession(
                docModel.getRepositoryName()).getDocument(new IdRef(commentId));
        return getComments(parentDocModel);
    }

}

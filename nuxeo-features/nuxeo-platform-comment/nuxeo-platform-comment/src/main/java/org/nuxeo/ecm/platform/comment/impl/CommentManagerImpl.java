/*
 * (C) Copyright 2007-2018 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.impl;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ANCESTOR_IDS;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentConverter;
import org.nuxeo.ecm.platform.comment.api.CommentEvents;
import org.nuxeo.ecm.platform.comment.api.Comments;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.jena.JenaGraph;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @deprecated since 10.3, use {@link PropertyCommentManager} instead.
 */
@Deprecated
public class CommentManagerImpl extends AbstractCommentManager {

    private static final Log log = LogFactory.getLog(CommentManagerImpl.class);

    final SimpleDateFormat timeFormat = new SimpleDateFormat("dd-HHmmss.S");

    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");

    final CommentServiceConfig config;

    final CommentConverter commentConverter;

    public CommentManagerImpl(CommentServiceConfig config) {
        this.config = config;
        commentConverter = config.getCommentConverter();
    }

    @Override
    public List<DocumentModel> getComments(CoreSession session, DocumentModel docModel) {
        Map<String, Object> ctxMap = Collections.<String, Object> singletonMap(ResourceAdapter.CORE_SESSION_CONTEXT_KEY,
                session);
        RelationManager relationManager = Framework.getService(RelationManager.class);
        Graph graph = relationManager.getGraph(config.graphName, session);
        Resource docResource = relationManager.getResource(config.documentNamespace, docModel, ctxMap);
        if (docResource == null) {
            throw new NuxeoException("Could not adapt document model to relation resource ; "
                    + "check the service relation adapters configuration");
        }

        // FIXME AT: why no filter on the predicate?
        List<Statement> statementList = graph.getStatements(null, null, docResource);
        if (graph instanceof JenaGraph) {
            // XXX AT: BBB for when repository name was not included in the
            // resource uri
            Resource oldDocResource = new QNameResourceImpl(config.documentNamespace, docModel.getId());
            statementList.addAll(graph.getStatements(null, null, oldDocResource));
        }

        List<DocumentModel> commentList = new ArrayList<DocumentModel>();
        for (Statement stmt : statementList) {
            QNameResourceImpl subject = (QNameResourceImpl) stmt.getSubject();

            DocumentModel commentDocModel = (DocumentModel) relationManager.getResourceRepresentation(
                    config.commentNamespace, subject, ctxMap);
            if (commentDocModel == null) {
                // XXX AT: maybe user cannot see the comment
                log.warn("Could not adapt comment relation subject to a document "
                        + "model; check the service relation adapters configur  ation");
                continue;
            }
            commentList.add(commentDocModel);
        }

        CommentSorter sorter = new CommentSorter(true);
        Collections.sort(commentList, sorter);

        return commentList;
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String comment, String author) {
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(docModel.getRepositoryName())) {
            DocumentModel commentDM = session.createDocumentModel(COMMENT_DOC_TYPE);
            commentDM.setPropertyValue(CommentsConstants.COMMENT_TEXT, comment);
            commentDM.setPropertyValue(CommentsConstants.COMMENT_AUTHOR, author);
            commentDM.setPropertyValue(CommentsConstants.COMMENT_CREATION_DATE, Calendar.getInstance());
            commentDM = internalCreateComment(session, docModel, commentDM, null);
            session.save();

            return commentDM;
        }
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String comment) {
        String author = getCurrentUser(docModel);
        return createComment(docModel, comment, author);
    }

    /**
     * If the author property on comment is not set, retrieve the author name from the session
     *
     * @param docModel The document model that holds the session id
     * @param comment The comment to update
     */
    private static String updateAuthor(DocumentModel docModel, DocumentModel comment) {
        // update the author if not set
        String author = (String) comment.getProperty("comment", "author");
        if (author == null) {
            log.debug("deprecated use of createComment: the client should set the author property on document");
            author = getCurrentUser(docModel);
            comment.setProperty("comment", "author", author);
        }
        return author;
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel comment) {
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(docModel.getRepositoryName())) {
            comment.setPropertyValue(COMMENT_ANCESTOR_IDS,
                    (Serializable) computeAncestorIds(session, docModel.getId()));
            DocumentModel doc = internalCreateComment(session, docModel, comment, null);
            session.save();
            doc.detach(true);
            return doc;
        }
    }

    protected DocumentModel internalCreateComment(CoreSession session, DocumentModel docModel, DocumentModel comment,
            String path) {
        DocumentModel createdComment;

        createdComment = createCommentDocModel(session, docModel, comment, path);

        RelationManager relationManager = Framework.getService(RelationManager.class);

        Resource commentRes = relationManager.getResource(config.commentNamespace, createdComment, null);

        Resource documentRes = relationManager.getResource(config.documentNamespace, docModel, null);

        if (commentRes == null || documentRes == null) {
            throw new NuxeoException("Could not adapt document model to relation resource ; "
                    + "check the service relation adapters configuration");
        }

        Resource predicateRes = new ResourceImpl(config.predicateNamespace);

        Statement stmt = new StatementImpl(commentRes, predicateRes, documentRes);
        relationManager.getGraph(config.graphName, session).add(stmt);

        notifyEvent(session, CommentEvents.COMMENT_ADDED, docModel, createdComment);

        return createdComment;
    }

    private DocumentModel createCommentDocModel(CoreSession mySession, DocumentModel docModel, DocumentModel comment,
            String path) {

        String domainPath;
        updateAuthor(docModel, comment);

        String[] pathList = getCommentPathList(comment);

        if (path == null) {
            domainPath = "/" + docModel.getPath().segment(0);
        } else {
            domainPath = path;
        }
        if (mySession == null) {
            return null;
        }

        // TODO GR upgrade this code. It can't work if current user
        // doesn't have admin rights

        DocumentModel parent = mySession.getDocument(new PathRef(domainPath));
        for (String name : pathList) {
            String pathStr = parent.getPathAsString();

            PathRef ref = new PathRef(pathStr, name);
            if (mySession.exists(ref)) {
                parent = mySession.getDocument(ref);
                if (!parent.isFolder()) {
                    throw new NuxeoException(parent.getPathAsString() + " is not folderish");
                }
            } else {
                DocumentModel dm = mySession.createDocumentModel(pathStr, name, "HiddenFolder");
                dm.setProperty("dublincore", "title", name);
                dm.setProperty("dublincore", "description", "");
                dm.setProperty("dublincore", "created", Calendar.getInstance());
                dm = mySession.createDocument(dm);
                setFolderPermissions(mySession, dm);
                parent = dm;
            }
        }

        String pathStr = parent.getPathAsString();
        String commentName = getCommentName(docModel, comment);
        CommentConverter converter = config.getCommentConverter();
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        DocumentModel commentDocModel = mySession.createDocumentModel(comment.getType());
        commentDocModel.setProperty("dublincore", "title", commentName);
        converter.updateDocumentModel(commentDocModel, comment);
        commentDocModel.setPathInfo(pathStr, pss.generatePathSegment(commentDocModel));
        commentDocModel = mySession.createDocument(commentDocModel);
        setCommentPermissions(mySession, commentDocModel);
        log.debug("created comment with id=" + commentDocModel.getId());

        return commentDocModel;
    }

    private String[] getCommentPathList(DocumentModel comment) {
        String[] pathList = new String[2];
        pathList[0] = COMMENTS_DIRECTORY;

        pathList[1] = dateFormat.format(getCommentTimeStamp(comment));
        return pathList;
    }

    /**
     * @deprecated if the caller is remote, we cannot obtain the session
     */
    @Deprecated
    private static String getCurrentUser(DocumentModel target) {
        CoreSession userSession = target.getCoreSession();
        if (userSession == null) {
            throw new NuxeoException("userSession is null, do not invoke this method when the user is not local");
        }
        return userSession.getPrincipal().getName();
    }

    private String getCommentName(DocumentModel target, DocumentModel comment) {
        String author = (String) comment.getProperty("comment", "author");
        if (author == null) {
            author = getCurrentUser(target);
        }
        Date creationDate = getCommentTimeStamp(comment);
        return "COMMENT-" + author + '-' + timeFormat.format(creationDate.getTime());
    }

    private static Date getCommentTimeStamp(DocumentModel comment) {
        Calendar creationDate;
        try {
            creationDate = (Calendar) comment.getProperty("dublincore", "created");
        } catch (PropertyException e) {
            creationDate = null;
        }
        if (creationDate == null) {
            creationDate = Calendar.getInstance();
        }
        return creationDate.getTime();
    }

    @Override
    public void deleteComment(DocumentModel docModel, DocumentModel comment) {
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(docModel.getRepositoryName())) {
            DocumentRef ref = comment.getRef();
            if (!session.exists(ref)) {
                throw new NuxeoException("Comment Document does not exist: " + comment.getId());
            }

            session.removeDocument(ref);

            notifyEvent(session, CommentEvents.COMMENT_REMOVED, docModel, comment);

            session.save();
        }
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel parent, DocumentModel child) {
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(docModel.getRepositoryName())) {
            DocumentModel parentDocModel = session.getDocument(parent.getRef());
            String containerPath = parent.getPath().removeLastSegments(1).toString();
            DocumentModel newComment = internalCreateComment(session, parentDocModel, child, containerPath);

            session.save();
            return newComment;
        }
    }

    @Override
    public List<DocumentModel> getDocumentsForComment(DocumentModel comment) {
        Map<String, Object> ctxMap = Collections.<String, Object> singletonMap(ResourceAdapter.CORE_SESSION_CONTEXT_KEY,
                comment.getCoreSession());
        RelationManager relationManager = Framework.getService(RelationManager.class);
        Graph graph = relationManager.getGraph(config.graphName, comment.getCoreSession());
        Resource commentResource = relationManager.getResource(config.commentNamespace, comment, ctxMap);
        if (commentResource == null) {
            throw new NuxeoException("Could not adapt document model to relation resource ; "
                    + "check the service relation adapters configuration");
        }
        Resource predicate = new ResourceImpl(config.predicateNamespace);

        List<Statement> statementList = graph.getStatements(commentResource, predicate, null);
        if (graph instanceof JenaGraph) {
            // XXX AT: BBB for when repository name was not included in the
            // resource uri
            Resource oldDocResource = new QNameResourceImpl(config.commentNamespace, comment.getId());
            statementList.addAll(graph.getStatements(oldDocResource, predicate, null));
        }

        List<DocumentModel> docList = new ArrayList<DocumentModel>();
        for (Statement stmt : statementList) {
            QNameResourceImpl subject = (QNameResourceImpl) stmt.getObject();
            DocumentModel docModel = (DocumentModel) relationManager.getResourceRepresentation(config.documentNamespace,
                    subject, ctxMap);
            if (docModel == null) {
                log.warn("Could not adapt comment relation subject to a document "
                        + "model; check the service relation adapters configuration");
                continue;
            }
            docList.add(docModel);
        }
        return docList;

    }

    @Override
    public DocumentModel createLocatedComment(DocumentModel docModel, DocumentModel comment, String path) {
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(docModel.getRepositoryName())) {
            DocumentModel createdComment = internalCreateComment(session, docModel, comment, path);
            session.save();
            return createdComment;
        }
    }

    @Override
    public DocumentModel getThreadForComment(DocumentModel comment) {
        List<DocumentModel> threads = getDocumentsForComment(comment);
        if (threads.size() > 0) {
            DocumentModel thread = threads.get(0);
            while (thread.getType().equals("Post") || thread.getType().equals(COMMENT_DOC_TYPE)) {
                thread = getThreadForComment(thread);
            }
            return thread;
        }
        return null;
    }

    @Override
    public Comment createComment(CoreSession session, Comment comment) throws CommentNotFoundException {
        DocumentRef commentRef = new IdRef(comment.getParentId());
        if (!session.exists(commentRef)) {
            throw new CommentNotFoundException("The document " + comment.getParentId() + " does not exist.");
        }
        DocumentModel docToComment = session.getDocument(commentRef);
        DocumentModel commentModel = session.createDocumentModel(COMMENT_DOC_TYPE);
        commentModel.setPropertyValue("dc:created", Calendar.getInstance());

        Comments.commentToDocumentModel(comment, commentModel);
        if (comment instanceof ExternalEntity) {
            commentModel.addFacet(EXTERNAL_ENTITY_FACET);
            Comments.externalEntityToDocumentModel((ExternalEntity) comment, commentModel);
        }

        DocumentModel createdCommentModel = createComment(docToComment, commentModel);
        return Comments.newComment(createdCommentModel);
    }

    @Override
    public Comment getComment(CoreSession session, String commentId) throws CommentNotFoundException {
        DocumentRef commentRef = new IdRef(commentId);
        if (!session.exists(commentRef)) {
            throw new CommentNotFoundException("The document " + commentId + " does not exist.");
        }
        DocumentModel commentModel = session.getDocument(commentRef);
        return Comments.newComment(commentModel);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize,
            Long currentPageIndex, boolean sortAscending) {
        DocumentRef docRef = new IdRef(documentId);
        if (!session.exists(docRef)) {
            return new PartialList<>(Collections.emptyList(), 0);
        }
        DocumentModel commentedDoc = session.getDocument(docRef);
        // do a dummy implementation of pagination for former comment manager implementation
        List<DocumentModel> comments = getComments(commentedDoc);
        long maxSize = pageSize == null || pageSize <= 0 ? comments.size() : pageSize;
        long offset = currentPageIndex == null || currentPageIndex <= 0 ? 0 : currentPageIndex * pageSize;
        return comments.stream()
                       .sorted(Comparator.comparing(doc -> (Calendar) doc.getPropertyValue("dc:created")))
                       .skip(offset)
                       .limit(maxSize)
                       .map(Comments::newComment)
                       .collect(collectingAndThen(toList(), list -> new PartialList<>(list, comments.size())));
    }

    @Override
    public void updateComment(CoreSession session, String commentId, Comment comment) throws NuxeoException {
        throw new UnsupportedOperationException("Update a comment is not possible through this implementation");
    }

    @Override
    public void deleteComment(CoreSession session, String commentId) throws CommentNotFoundException {
        DocumentRef commentRef = new IdRef(commentId);
        if (!session.exists(commentRef)) {
            throw new CommentNotFoundException("The comment " + commentId + " does not exist.");
        }
        DocumentModel comment = session.getDocument(commentRef);
        DocumentModel commentedDoc = session.getDocument(
                new IdRef((String) comment.getPropertyValue(COMMENT_PARENT_ID)));
        deleteComment(commentedDoc, comment);
    }

    @Override
    public Comment getExternalComment(CoreSession session, String entityId) throws NuxeoException {
        throw new UnsupportedOperationException(
                "Get a comment from its external entity id is not possible through this implementation");
    }

    @Override
    public void updateExternalComment(CoreSession session, String entityId, Comment comment) throws NuxeoException {
        throw new UnsupportedOperationException(
                "Update a comment from its external entity id is not possible through this implementation");
    }

    @Override
    public void deleteExternalComment(CoreSession session, String entityId) throws NuxeoException {
        throw new UnsupportedOperationException(
                "Delete a comment from its external entity id is not possible through this implementation");
    }

    @Override
    public boolean hasFeature(Feature feature) {
        switch (feature) {
        case COMMENTS_LINKED_WITH_PROPERTY:
            return false;
        default:
            throw new UnsupportedOperationException(feature.name());
        }
    }
}

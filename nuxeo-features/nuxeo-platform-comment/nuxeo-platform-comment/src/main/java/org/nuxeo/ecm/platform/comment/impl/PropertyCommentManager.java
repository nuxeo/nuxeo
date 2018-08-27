/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.platform.comment.impl;

import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_PROPERTY;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOCUMENT_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.Comments;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Comment service implementation. The comments are linked together thanks to a parent document id property.
 * 
 * @since 10.3
 */
public class PropertyCommentManager implements CommentManager {

    private static final Log log = LogFactory.getLog(PropertyCommentManager.class);

    protected static final String GET_COMMENTS_FOR_DOC_PAGEPROVIDER_NAME = "GET_COMMENTS_FOR_DOCUMENT";

    @Override
    public List<DocumentModel> getComments(DocumentModel docModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DocumentModel> getComments(DocumentModel docModel, DocumentModel parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String comment) {
        return createComment(docModel, comment, docModel.getCoreSession().getPrincipal().getName());
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String text, String author) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel parent, DocumentModel child) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteComment(DocumentModel docModel, DocumentModel comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DocumentModel> getDocumentsForComment(DocumentModel comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel getThreadForComment(DocumentModel comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createLocatedComment(DocumentModel docModel, DocumentModel comment, String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Comment createComment(CoreSession session, Comment comment) throws IllegalArgumentException {
        if (!session.exists(new IdRef(comment.getDocumentId()))) {
            throw new IllegalArgumentException("The document " + comment.getDocumentId() + " does not exist.");
        }
        return CoreInstance.doPrivileged(session, s -> {
            // TODO create comments container folder
            DocumentModel commentModel = s.createDocumentModel(COMMENT_DOC_TYPE);
            commentModel.setPropertyValue(COMMENT_AUTHOR, comment.getAuthor());
            commentModel.setPropertyValue(COMMENT_TEXT, comment.getText());
            commentModel.setPropertyValue(COMMENT_DOCUMENT_ID, comment.getDocumentId());
            commentModel.setPropertyValue(COMMENT_CREATION_DATE, comment.getCreationDate());
            commentModel.setPropertyValue(COMMENT_MODIFICATION_DATE, comment.getModificationDate());
            if (comment instanceof ExternalEntity) {
                commentModel.setPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY, ((ExternalEntity) comment).getEntityId());
                commentModel.setPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY, ((ExternalEntity) comment).getOrigin());
                commentModel.setPropertyValue(EXTERNAL_ENTITY_PROPERTY, ((ExternalEntity) comment).getEntity());
                commentModel = s.createDocument(commentModel);
                Comment createdComment = new CommentImpl();
                Comments.documentModelToExternalComment().accept(commentModel, createdComment);
                return createdComment;
            }
            commentModel = s.createDocument(commentModel);
            Comment createdComment = new CommentImpl();
            Comments.documentModelToComment().accept(commentModel, createdComment);
            return createdComment;
        });
    }

    @Override
    public Comment getComment(CoreSession session, String commentId) throws IllegalArgumentException {
        return CoreInstance.doPrivileged(session, s -> {
            DocumentRef commentRef = new IdRef(commentId);
            if (!s.exists(commentRef)) {
                throw new IllegalArgumentException("The document " + commentId + " does not exist.");
            }
            DocumentModel commentModel = s.getDocument(commentRef);
            Comment comment = new CommentImpl();
            comment.setAuthor((String) commentModel.getPropertyValue(COMMENT_AUTHOR));
            comment.setText((String) commentModel.getPropertyValue(COMMENT_TEXT));
            comment.setDocumentId((String) commentModel.getPropertyValue(COMMENT_DOCUMENT_ID));
            comment.setCreationDate((Instant) commentModel.getPropertyValue(COMMENT_CREATION_DATE));
            comment.setModificationDate((Instant) commentModel.getPropertyValue(COMMENT_MODIFICATION_DATE));
            if (commentModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
                ((ExternalEntity) comment).setEntityId(
                        (String) commentModel.getPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY));
                ((ExternalEntity) comment).setOrigin(
                        (String) commentModel.getPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY));
                ((ExternalEntity) comment).setEntity((String) commentModel.getPropertyValue(EXTERNAL_ENTITY_PROPERTY));
            }
            return comment;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Comment> getComments(CoreSession session, String documentId) throws IllegalArgumentException {
        if (!session.exists(new IdRef(documentId))) {
            throw new IllegalArgumentException("The document " + documentId + " does not exist.");
        }
        return CoreInstance.doPrivileged(session, s -> {
            PageProviderService ppService = Framework.getService(PageProviderService.class);
            Map<String, Serializable> props = Collections.singletonMap(
                    CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY, (Serializable) s);
            List<DocumentModel> commentList = ((PageProvider<DocumentModel>) ppService.getPageProvider(
                    GET_COMMENTS_FOR_DOC_PAGEPROVIDER_NAME, null, null, null, props, documentId)).getCurrentPage();
            return commentList.stream().map(commentModel -> {
                Comment comment = new CommentImpl();
                comment.setAuthor((String) commentModel.getPropertyValue(COMMENT_AUTHOR));
                comment.setText((String) commentModel.getPropertyValue(COMMENT_TEXT));
                comment.setDocumentId(documentId);
                comment.setCreationDate((Instant) commentModel.getPropertyValue(COMMENT_CREATION_DATE));
                comment.setModificationDate((Instant) commentModel.getPropertyValue(COMMENT_MODIFICATION_DATE));
                if (commentModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
                    ((ExternalEntity) comment).setEntityId(
                            (String) commentModel.getPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY));
                    ((ExternalEntity) comment).setOrigin(
                            (String) commentModel.getPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY));
                    ((ExternalEntity) comment).setEntity(
                            (String) commentModel.getPropertyValue(EXTERNAL_ENTITY_PROPERTY));
                }
                return comment;
            }).collect(Collectors.toList());
        });
    }

    @Override
    public void updateComment(CoreSession session, String commentId, Comment comment) throws IllegalArgumentException {
        CoreInstance.doPrivileged(session, s -> {
            IdRef commentRef = new IdRef(commentId);
            if (!s.exists(commentRef)) {
                throw new IllegalArgumentException("The comment " + commentId + " does not exist.");
            }
            DocumentModel commentModel = s.getDocument(commentRef);
            commentModel.setPropertyValue(COMMENT_TEXT, comment.getText());
            commentModel.setPropertyValue(COMMENT_DOCUMENT_ID, comment.getDocumentId());
            commentModel.setPropertyValue(COMMENT_CREATION_DATE, comment.getCreationDate());
            commentModel.setPropertyValue(COMMENT_MODIFICATION_DATE, comment.getModificationDate());
            if (comment instanceof ExternalEntity) {
                commentModel.setPropertyValue(EXTERNAL_ENTITY_ID, ((ExternalEntity) comment).getEntityId());
                commentModel.setPropertyValue(EXTERNAL_ENTITY_ORIGIN, ((ExternalEntity) comment).getOrigin());
                commentModel.setPropertyValue(EXTERNAL_ENTITY, ((ExternalEntity) comment).getEntity());
            }
            s.saveDocument(commentModel);
        });
    }

    @Override
    public void deleteComment(CoreSession session, String commentId) throws IllegalArgumentException {
        CoreInstance.doPrivileged(session, s -> {
            IdRef commentRef = new IdRef(commentId);
            if (!s.exists(commentRef)) {
                throw new IllegalArgumentException("The comment " + commentId + " does not exist.");
            }
            s.removeDocument(commentRef);
        });
    }
}

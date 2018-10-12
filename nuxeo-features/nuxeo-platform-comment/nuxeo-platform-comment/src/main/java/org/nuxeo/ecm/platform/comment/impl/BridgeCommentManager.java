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
 *     Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment.impl;

import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;

/**
 * @since 10.3
 */
public class BridgeCommentManager extends AbstractCommentManager {

    protected final CommentManager first;

    protected final CommentManager second;

    public BridgeCommentManager(CommentManager first, CommentManager second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public List<DocumentModel> getComments(DocumentModel docModel) {
        return Stream.concat(first.getComments(docModel).stream(), second.getComments(docModel).stream())
                     .distinct()
                     .collect(Collectors.toList());
    }

    @Override
    public List<DocumentModel> getComments(CoreSession session, DocumentModel docModel) {
        return Stream.concat(first.getComments(session, docModel).stream(),
                second.getComments(session, docModel).stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String comment) {
        return second.createComment(docModel, comment);
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String comment, String author) {
        return second.createComment(docModel, comment, author);
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel comment) {
        return second.createComment(docModel, comment);
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel parent, DocumentModel child) {
        return second.createComment(docModel, parent, child);
    }

    @Override
    public void deleteComment(DocumentModel docModel, DocumentModel comment) {
        if (comment.getPropertyValue(COMMENT_PARENT_ID) != null) {
            second.deleteComment(docModel, comment);
        } else {
            first.deleteComment(docModel, comment);
        }
    }

    @Override
    public List<DocumentModel> getDocumentsForComment(DocumentModel comment) {
        return Stream.concat(first.getDocumentsForComment(comment).stream(),
                second.getDocumentsForComment(comment).stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public DocumentModel getThreadForComment(DocumentModel comment) {
        if (comment.getPropertyValue(COMMENT_PARENT_ID) != null) {
            return second.getThreadForComment(comment);
        }
        return first.getThreadForComment(comment);
    }

    @Override
    public DocumentModel createLocatedComment(DocumentModel docModel, DocumentModel comment, String path) {
        return second.createLocatedComment(docModel, comment, path);
    }

    @Override
    public Comment createComment(CoreSession session, Comment comment) throws CommentNotFoundException {
        return second.createComment(session, comment);
    }

    @Override
    public Comment getComment(CoreSession session, String commentId) throws CommentNotFoundException {
        return second.getComment(session, commentId);
    }

    @Override
    public List<Comment> getComments(CoreSession session, String documentId) {
        return Stream.concat(first.getComments(session, documentId).stream(),
                second.getComments(session, documentId).stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize,
            Long currentPageIndex, boolean sortAscending) {
        List<Comment> firstComments = first.getComments(session, documentId, pageSize, currentPageIndex, sortAscending);
        List<Comment> secondComments = second.getComments(session, documentId, pageSize, currentPageIndex,
                sortAscending);
        List<Comment> allComments = Stream.concat(firstComments.stream(), secondComments.stream())
                                          .distinct()
                                          .collect(Collectors.toList());
        return new PartialList<>(allComments, allComments.size());
    }

    @Override
    public void updateComment(CoreSession session, String commentId, Comment comment) throws CommentNotFoundException {
        DocumentRef commentRef = new IdRef(commentId);
        if (!session.exists(commentRef)) {
            throw new CommentNotFoundException("The comment " + commentId + " does not exist");
        }
        if (session.getDocument(commentRef).getPropertyValue(COMMENT_PARENT_ID) != null) {
            second.updateComment(session, commentId, comment);
        } else {
            first.updateComment(session, commentId, comment);
        }
    }

    @Override
    public void deleteComment(CoreSession session, String commentId) throws CommentNotFoundException {
        DocumentRef commentRef = new IdRef(commentId);
        if (!session.exists(commentRef)) {
            throw new CommentNotFoundException("The comment " + commentId + " does not exist");
        }
        if (session.getDocument(commentRef).getPropertyValue(COMMENT_PARENT_ID) != null) {
            second.deleteComment(session, commentId);
        } else {
            first.deleteComment(session, commentId);
        }
    }

    @Override
    public Comment getExternalComment(CoreSession session, String entityId) throws NuxeoException {
        return second.getExternalComment(session, entityId);
    }

    @Override
    public void updateExternalComment(CoreSession session, String entityId, Comment comment) throws NuxeoException {
        second.updateExternalComment(session, entityId, comment);
    }

    @Override
    public void deleteExternalComment(CoreSession session, String entityId) throws NuxeoException {
        second.deleteExternalComment(session, entityId);
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

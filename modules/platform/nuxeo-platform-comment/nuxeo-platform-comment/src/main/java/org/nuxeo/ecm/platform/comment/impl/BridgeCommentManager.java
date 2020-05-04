/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_PROPERTY;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentConstants;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;

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

    /**
     * @since 11.1
     */
    public CommentManager getFirst() {
        return first;
    }

    /**
     * @since 11.1
     */
    public CommentManager getSecond() {
        return second;
    }

    @Override
    public List<DocumentModel> getComments(DocumentModel docModel) {
        return Stream.concat(first.getComments(docModel).stream(), second.getComments(docModel).stream())
                     .distinct()
                     .collect(Collectors.toList());
    }

    @Override
    public List<DocumentModel> getComments(CoreSession session, DocumentModel docModel)
            throws CommentSecurityException {
        return Stream.concat(first.getComments(session, docModel).stream(),
                second.getComments(session, docModel).stream()).distinct().collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("deprecation")
    public DocumentModel createComment(DocumentModel docModel, String comment) {
        return second.createComment(docModel, comment);
    }

    @Override
    @SuppressWarnings("removal")
    public DocumentModel createComment(DocumentModel docModel, String comment, String author) {
        return second.createComment(docModel, comment, author);
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel comment) throws CommentSecurityException {
        return second.createComment(docModel, comment);
    }

    @Override
    @SuppressWarnings("removal")
    public DocumentModel createComment(DocumentModel docModel, DocumentModel parent, DocumentModel child) {
        return second.createComment(docModel, parent, child);
    }

    @Override
    @SuppressWarnings("removal")
    public void deleteComment(DocumentModel docModel, DocumentModel comment) {
        if (comment.getPropertyValue(COMMENT_PARENT_ID_PROPERTY) != null) {
            second.deleteComment(docModel, comment);
        } else {
            first.deleteComment(docModel, comment);
        }
    }

    @Override
    @SuppressWarnings("removal")
    public List<DocumentModel> getDocumentsForComment(DocumentModel comment) {
        return Stream.concat(first.getDocumentsForComment(comment).stream(),
                second.getDocumentsForComment(comment).stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public DocumentModel getThreadForComment(DocumentModel comment) throws CommentSecurityException {
        // handle only Relation to Property bridge, this method should have been deprecated in 10.10 due to unused
        if (comment.getPropertyValue(COMMENT_PARENT_ID_PROPERTY) != null) {
            if (second instanceof TreeCommentManager) {
                return first.getThreadForComment(comment);
            } else {
                return second.getThreadForComment(comment);
            }
        }
        return first.getThreadForComment(comment);
    }

    @Override
    public DocumentModel createLocatedComment(DocumentModel docModel, DocumentModel comment, String path)
            throws CommentSecurityException {
        return second.createLocatedComment(docModel, comment, path);
    }

    @Override
    public Comment createComment(CoreSession session, Comment comment)
            throws CommentNotFoundException, CommentSecurityException {
        return second.createComment(session, comment);
    }

    @Override
    public Comment getComment(CoreSession session, String commentId)
            throws CommentNotFoundException, CommentSecurityException {
        return second.getComment(session, commentId);
    }

    @Override
    public List<Comment> getComments(CoreSession session, String documentId) {
        return Stream.concat(first.getComments(session, documentId).stream(),
                second.getComments(session, documentId).stream()).distinct().collect(Collectors.toList());
    }

    @Override
    public PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize,
            Long currentPageIndex, boolean sortAscending) throws CommentSecurityException {
        List<Comment> firstComments = first.getComments(session, documentId, pageSize, currentPageIndex, sortAscending);
        List<Comment> secondComments = second.getComments(session, documentId, pageSize, currentPageIndex,
                sortAscending);
        List<Comment> allComments = Stream.concat(firstComments.stream(), secondComments.stream())
                                          .distinct()
                                          .collect(Collectors.toList());
        return new PartialList<>(allComments, allComments.size());
    }

    @Override
    public Comment updateComment(CoreSession session, String commentId, Comment comment)
            throws CommentNotFoundException, CommentSecurityException {
        DocumentRef commentRef = new IdRef(commentId);
        return CoreInstance.doPrivileged(session, s -> {
            // retrieve comment to check which service handles it
            if (!s.exists(commentRef)) {
                throw new CommentNotFoundException("The comment " + commentId + " does not exist");
            }
            if (s.getDocument(commentRef).getPropertyValue(COMMENT_PARENT_ID_PROPERTY) != null) {
                return second.updateComment(session, commentId, comment);
            } else {
                return first.updateComment(session, commentId, comment);
            }
        });
    }

    @Override
    public void deleteComment(CoreSession session, String commentId)
            throws CommentNotFoundException, CommentSecurityException {
        DocumentRef commentRef = new IdRef(commentId);
        CoreInstance.doPrivileged(session, s -> {
            // retrieve comment to check which service handles it
            if (!s.exists(commentRef)) {
                throw new CommentNotFoundException("The comment " + commentId + " does not exist");
            }
            if (s.getDocument(commentRef).getPropertyValue(COMMENT_PARENT_ID_PROPERTY) != null) {
                second.deleteComment(session, commentId);
            } else {
                first.deleteComment(session, commentId);
            }
        });
    }

    @Override
    public Comment getExternalComment(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException {
        return second.getExternalComment(session, entityId);
    }

    @Override
    public Comment updateExternalComment(CoreSession session, String entityId, Comment comment)
            throws CommentNotFoundException, CommentSecurityException {
        return second.updateExternalComment(session, entityId, comment);
    }

    @Override
    public void deleteExternalComment(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException {
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

    @Override
    public DocumentRef getTopLevelDocumentRef(CoreSession session, DocumentRef commentIdRef) {
        return execute(session, commentIdRef, cm -> cm.getTopLevelDocumentRef(session, commentIdRef));
    }

    @Override
    protected DocumentModel getTopLevelDocument(CoreSession session, DocumentModel commentDoc) {
        // this abstract method should be called within an implementation - no need to implement it in the bridge
        throw new UnsupportedOperationException();
    }

    @Override
    protected DocumentModel getCommentedDocument(CoreSession session, DocumentModel commentDoc) {
        // this abstract method should be called within an implementation - no need to implement it in the bridge
        throw new UnsupportedOperationException();
    }

    /**
     * Executes the given function for a comment document ref, depending on the types of comment managers.
     * <p>
     * In some cases, leveraging {@link CommentConstants#COMMENT_PARENT_ID_PROPERTY} is not enough, this is the case
     * when bridge is used with {@link PropertyCommentManager} and {@link TreeCommentManager}.
     * <ul>
     * <li>{@link CommentManagerImpl} (or RelationCommentManager): Comments structures are stored in jenaGraph</li>
     * <li>{@link PropertyCommentManager}: All comments are stored under a hidden folder and each comment stores its
     * parent id in {@code comment:parentId} property</li>
     * <li>{@link TreeCommentManager}: A {@link CommentConstants#COMMENT_ROOT_DOC_TYPE} document is created under the
     * top level document to store the comments. Replies are then stored directly under their parent (which is a
     * comment)</li>
     * </ul>
     *
     * @since 11.1
     */
    @SuppressWarnings("deprecation")
    protected <T> T execute(CoreSession s, DocumentRef documentRef, Function<CommentManager, T> function) {
        return CoreInstance.doPrivileged(s, session -> {
            DocumentModel documentModel = session.getDocument(documentRef);

            // From `Relation` to `Property`
            if (first instanceof CommentManagerImpl && second instanceof PropertyCommentManager) {
                if (documentModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY) != null) {
                    // Comment is in property model
                    return function.apply(second);
                }
                // Comment still in relation model
                return function.apply(first);
            }

            // From `Property` to `Tree`
            if (first instanceof PropertyCommentManager && second instanceof TreeCommentManager) {
                // In this case we cannot just rely on `comment:parentId` but on the type of doc that contains the
                // comment
                DocumentRef parentRef = documentModel.getParentRef();
                // Comment is under property model
                if (session.getDocument(parentRef).getType().equals("HiddenFolder")) {
                    return function.apply(first);
                }
                // Comment is under tree model
                return function.apply(second);
            }

            throw new IllegalArgumentException(String.format(
                    "Undefined behaviour for document ref: %s, first: %s, second: %s ", documentModel, first, second));
        });
    }

}

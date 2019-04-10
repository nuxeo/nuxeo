/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.comment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.dam.webapp.contentbrowser.DamDocumentActions;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.webapp.helpers.EventNames;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

/**
 * Comments related actions on DAM.
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
@Name("commentActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class CommentActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CommentActions.class);

    @In(create = true)
    private transient NuxeoPrincipal currentNuxeoPrincipal;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient DamDocumentActions damDocumentActions;

    protected String newContent;

    protected boolean showCreateForm;

    protected boolean showCommentsArea;

    protected CommentableDocument commentableDoc;

    protected List<DocumentModel> comments;

    public void addComment() throws ClientException {
        if (!StringUtils.isBlank(newContent)) {
            DocumentModel myComment = documentManager.createDocumentModel("Comment");

            myComment.setProperty("comment", "author",
                    currentNuxeoPrincipal.getName());
            myComment.setProperty("comment", "text", newContent);
            myComment.setProperty("comment", "creationDate",
                    Calendar.getInstance());
            myComment = addComment(myComment);

            // Reset comments list and 'add comment' field & flag
            showCreateForm = false;
            newContent = null;
            comments = null;
        }
    }

    protected DocumentModel initializeComment(DocumentModel comment) {
        if (comment != null) {
            try {
                if (comment.getProperty("dublincore", "contributors") == null) {
                    String[] contributors = new String[1];
                    contributors[0] = currentNuxeoPrincipal.getName();
                    comment.setProperty("dublincore", "contributors",
                            contributors);
                }
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
            try {
                if (comment.getProperty("dublincore", "created") == null) {
                    comment.setProperty("dublincore", "created",
                            Calendar.getInstance());
                }
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
        }
        return comment;
    }

    public static class FollowTransitionUnrestricted extends
            UnrestrictedSessionRunner {

        public final DocumentRef docRef;

        public final String transition;

        public FollowTransitionUnrestricted(CoreSession session,
                DocumentRef docRef, String transition) {
            super(session);
            this.docRef = docRef;
            this.transition = transition;
        }

        @Override
        public void run() throws ClientException {
            session.followTransition(docRef, transition);
            session.save();
        }
    }

    public DocumentModel addComment(DocumentModel comment)
            throws ClientException {
        try {
            comment = initializeComment(comment);

            commentableDoc = getCommentableDoc();

            DocumentModel newComment = commentableDoc.addComment(comment);

            // automatically validate the comments
            if (CommentsConstants.COMMENT_LIFECYCLE.equals(newComment.getLifeCyclePolicy())) {
                new FollowTransitionUnrestricted(documentManager,
                        newComment.getRef(),
                        CommentsConstants.TRANSITION_TO_PUBLISHED_STATE).runUnrestricted();
            }

            return newComment;

        } catch (Throwable t) {
            log.error("failed to add comment", t);
            throw ClientException.wrap(t);
        }
    }

    protected CommentableDocument getCommentableDoc() {
        DocumentModel currentDocument = damDocumentActions.getCurrentSelection();
        commentableDoc = currentDocument.getAdapter(CommentableDocument.class);

        return commentableDoc;
    }

    public List<DocumentModel> getComments() throws ClientException {
        if (comments != null) {
            return comments;
        }

        commentableDoc = getCommentableDoc();

        if (commentableDoc != null) {
            comments = commentableDoc.getComments();
            return comments;
        } else {
            return new ArrayList<DocumentModel>();
        }
    }

    public void deleteComment(DocumentModel comment) throws ClientException {
        if (comment == null) {
            log.error("No comment to delete");
            return;
        }
        try {
            commentableDoc = getCommentableDoc();
            if (commentableDoc != null) {
                commentableDoc.removeComment(comment);
            } else {
                log.error("No commentable document fetched.");
            }
        } catch (Throwable t) {
            log.error("failed to delete comment", t);
            throw ClientException.wrap(t);
        }

        // Reset comments list
        comments = null;
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void cleanContextVariable() {
        showCreateForm = false;
        newContent = null;
        comments = null;
    }

    public String getNewContent() {
        return newContent;
    }

    public void setNewContent(String newContent) {
        this.newContent = newContent;
    }

    public boolean isShowCreateForm() {
        return showCreateForm;
    }

    public void setShowCreateForm(boolean showCreateForm) {
        this.showCreateForm = showCreateForm;
    }

    public void toggleCreateForm(ActionEvent event) {
        showCreateForm = !showCreateForm;
        showCommentsArea = true;
    }

    public boolean isShowCommentsArea() {
        return showCommentsArea;
    }

    public void setShowCommentsArea(boolean showCommentsArea) {
        this.showCommentsArea = showCommentsArea;
    }

    public void toggleCommentsArea(ActionEvent event) {
        showCommentsArea = !showCommentsArea;
        if (!showCommentsArea) {
            showCreateForm = false;
        }
    }

}

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

package org.nuxeo.ecm.platform.comment.web;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.security.UserSession;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public abstract class AbstractCommentManagerActionsBean implements
        CommentManagerActions {

    protected static final String COMMENTS_ACTIONS = "COMMENT_ACTIONS";

    private static final Log log = LogFactory.getLog(AbstractCommentManagerActionsBean.class);

    protected NuxeoPrincipal principal;

    protected boolean principalIsAdmin;

    protected boolean showCreateForm;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient WebActions webActions;

    protected String newContent;

    protected CommentableDocument commentableDoc;

    protected List<UIComment> uiComments;

    protected List<ThreadEntry> commentThread;

    // the id of the comment to delete
    @RequestParameter
    protected String deleteCommentId;

    // the id of the comment to reply to
    @RequestParameter
    protected String replyCommentId;

    protected String savedReplyCommentId;

    protected Map<String, UIComment> commentMap;

    protected boolean commentStarted;

    protected List<UIComment> flatComments;

    @In(create = true)
    protected UserSession userSession;

    @In(create = true)
    protected NavigationContext navigationContext;

    @Create
    public void initialize() throws Exception {
        log.debug("Initializing...");
        commentMap = new HashMap<String, UIComment>();
        showCreateForm = false;

        principal = userSession.getCurrentNuxeoPrincipal();
        principalIsAdmin = principal.isAdministrator();
    }

    @Destroy
    public void destroy() {
        commentMap = null;
        log.debug("Removing Seam action listener...");
    }

    public String getPrincipalName() {
        return principal.getName();
    }

    public boolean getPrincipalIsAdmin() {
        return principalIsAdmin;
    }

    protected DocumentModel initializeComment(DocumentModel comment) {
        if (comment != null) {
            try {
                if (comment.getProperty("dublincore", "contributors") == null) {
                    String[] contributors = new String[1];
                    contributors[0] = getPrincipalName();
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

    public DocumentModel addComment(DocumentModel comment,
            DocumentModel docToComment) throws ClientException {
        try {
            comment = initializeComment(comment);
            UIComment parentComment = null;
            if (savedReplyCommentId != null) {
                parentComment = commentMap.get(savedReplyCommentId);
            }
            if (docToComment != null) {
                commentableDoc = getCommentableDoc(docToComment);
            }
            if (commentableDoc == null) {
                commentableDoc = getCommentableDoc();
            }
            // what if commentableDoc is still null? shouldn't, but...
            if (commentableDoc == null) {
                throw new ClientException("Can't comment on null document");
            }
            DocumentModel newComment;
            if (parentComment != null) {
                newComment = commentableDoc.addComment(
                        parentComment.getComment(), comment);
            } else {
                newComment = commentableDoc.addComment(comment);
            }

            // automatically validate the comments
            if (CommentsConstants.COMMENT_LIFECYCLE.equals(newComment.getLifeCyclePolicy())) {
                new FollowTransitionUnrestricted(documentManager,
                        newComment.getRef(),
                        CommentsConstants.TRANSITION_TO_PUBLISHED_STATE).runUnrestricted();
            }

            // Events.instance().raiseEvent(CommentEvents.COMMENT_ADDED, null,
            // newComment);
            cleanContextVariable();

            return newComment;

        } catch (Throwable t) {
            log.error("failed to add comment", t);
            throw ClientException.wrap(t);
        }
    }

    public DocumentModel addComment(DocumentModel comment)
            throws ClientException {
        return addComment(comment, null);
    }

    public String addComment() throws ClientException {
        DocumentModel myComment = documentManager.createDocumentModel("Comment");

        myComment.setProperty("comment", "author", principal.getName());
        myComment.setProperty("comment", "text", newContent);
        myComment.setProperty("comment", "creationDate", Calendar.getInstance());
        myComment = addComment(myComment);

        // do not navigate to newly-created comment, they are hidden documents
        return null;
    }

    public String createComment(DocumentModel docToComment)
            throws ClientException {
        DocumentModel myComment = documentManager.createDocumentModel("Comment");

        myComment.setProperty("comment", "author", principal.getName());
        myComment.setProperty("comment", "text", newContent);
        myComment.setProperty("comment", "creationDate", Calendar.getInstance());
        myComment = addComment(myComment, docToComment);

        // do not navigate to newly-created comment, they are hidden documents
        return null;
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.CONTENT_ROOT_SELECTION_CHANGED,
            EventNames.DOCUMENT_CHANGED }, create = false)
    @BypassInterceptors
    public void documentChanged() {
        cleanContextVariable();
    }

    protected CommentableDocument getCommentableDoc() {
        if (commentableDoc == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            commentableDoc = currentDocument.getAdapter(CommentableDocument.class);
        }
        return commentableDoc;
    }

    protected CommentableDocument getCommentableDoc(DocumentModel doc) {
        if (doc == null) {
            doc = navigationContext.getCurrentDocument();
        }
        commentableDoc = doc.getAdapter(CommentableDocument.class);
        return commentableDoc;
    }

    /**
     * Initializes uiComments with Comments of current document.
     */
    public void initComments() throws ClientException {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            throw new ClientException("Unable to find current Document");
        }
        initComments(currentDoc);
    }

    /**
     * Initializes uiComments with Comments of current document.
     */
    public void initComments(DocumentModel commentedDoc) throws ClientException {
        commentableDoc = getCommentableDoc(commentedDoc);
        if (uiComments == null) {
            uiComments = new ArrayList<UIComment>();
            if (commentableDoc != null) {
                List<DocumentModel> comments = commentableDoc.getComments();
                for (DocumentModel comment : comments) {
                    UIComment uiComment = createUIComment(null, comment);
                    uiComments.add(uiComment);
                }
            }
        }
    }

    public List<UIComment> getComments(DocumentModel doc)
            throws ClientException {
        List<UIComment> allComments = new ArrayList<UIComment>();
        commentableDoc = doc.getAdapter(CommentableDocument.class);
        if (commentableDoc != null) {
            List<DocumentModel> comments = commentableDoc.getComments();
            for (DocumentModel comment : comments) {
                UIComment uiComment = createUIComment(null, comment);
                allComments.add(uiComment);
            }
        }
        return allComments;
    }

    /**
     * Recursively retrieves all comments of a doc.
     */
    public List<ThreadEntry> getCommentsAsThreadOnDoc(DocumentModel doc)
            throws ClientException {
        List<ThreadEntry> allComments = new ArrayList<ThreadEntry>();
        List<UIComment> allUIComments = getComments(doc);

        for (UIComment uiComment : allUIComments) {
            allComments.add(new ThreadEntry(uiComment.getComment(), 0));
            if (uiComment.getChildren() != null) {
                flattenTree(allComments, uiComment, 0);
            }
        }
        return allComments;
    }

    public List<ThreadEntry> getCommentsAsThread(DocumentModel commentedDoc)
            throws ClientException {
        if (commentThread != null) {
            return commentThread;
        }
        commentThread = new ArrayList<ThreadEntry>();
        if (uiComments == null) {
            initComments(commentedDoc); // Fetches all the comments associated
            // with the
            // document into uiComments (a list of comment
            // roots).
        }
        for (UIComment uiComment : uiComments) {
            commentThread.add(new ThreadEntry(uiComment.getComment(), 0));
            if (uiComment.getChildren() != null) {
                flattenTree(commentThread, uiComment, 0);
            }
        }
        return commentThread;
    }

    /**
     * Visits a list of comment trees and puts them into a list of
     * "ThreadEntry"s.
     */
    public void flattenTree(List<ThreadEntry> commentThread,
            UIComment uiComment, int depth) {
        List<UIComment> uiChildren = uiComment.getChildren();
        for (UIComment uiChild : uiChildren) {
            commentThread.add(new ThreadEntry(uiChild.getComment(), depth + 1));
            if (uiChild.getChildren() != null) {
                flattenTree(commentThread, uiChild, depth + 1);
            }
        }
    }

    /**
     * Creates a UIComment wrapping "comment", having "parent" as parent.
     */
    protected UIComment createUIComment(UIComment parent, DocumentModel comment)
            throws ClientException {
        UIComment wrapper = new UIComment(parent, comment);
        commentMap.put(wrapper.getId(), wrapper);
        List<DocumentModel> children = commentableDoc.getComments(comment);
        for (DocumentModel child : children) {
            UIComment uiChild = createUIComment(wrapper, child);
            wrapper.addChild(uiChild);
        }
        return wrapper;
    }

    public String deleteComment(String commentId) throws ClientException {
        if ("".equals(commentId)) {
            log.error("No comment id to delete");
            return null;
        }
        if (commentableDoc == null) {
            log.error("Can't delete comments of null document");
            return null;
        }
        try {
            UIComment selectedComment = commentMap.get(commentId);
            commentableDoc.removeComment(selectedComment.getComment());
            cleanContextVariable();
            // Events.instance().raiseEvent(CommentEvents.COMMENT_REMOVED, null,
            // selectedComment.getComment());
            return null;
        } catch (Throwable t) {
            log.error("failed to delete comment", t);
            throw ClientException.wrap(t);
        }
    }

    public String deleteComment() throws ClientException {
        return deleteComment(deleteCommentId);
    }

    public String getNewContent() {
        return newContent;
    }

    public void setNewContent(String newContent) {
        this.newContent = newContent;
    }

    public String beginComment() {
        commentStarted = true;
        savedReplyCommentId = replyCommentId;
        showCreateForm = false;
        return null;
    }

    public String cancelComment() {
        cleanContextVariable();
        return null;
    }

    public boolean getCommentStarted() {
        return commentStarted;
    }

    /**
     * Retrieves children for a given comment.
     */
    public void getChildren(UIComment comment) {
        assert comment != null;

        List<UIComment> children = comment.getChildren();

        if (!children.isEmpty()) {
            for (UIComment childComment : children) {
                getChildren(childComment);
            }
        }
        flatComments.add(comment);
    }

    @SuppressWarnings("unchecked")
    public List<UIComment> getLastCommentsByDate(String commentNumber,
            DocumentModel commentedDoc) throws ClientException {
        int number = Integer.parseInt(commentNumber);
        List<UIComment> comments = new ArrayList<UIComment>();
        flatComments = new ArrayList<UIComment>();

        // Initialize uiComments
        initComments(commentedDoc);

        if (number < 0 || uiComments.isEmpty()) {
            return null;
        }
        for (UIComment comment : uiComments) {
            getChildren(comment);
        }
        if (!flatComments.isEmpty()) {
            Collections.sort(flatComments);
        }
        if (number > flatComments.size()) {
            number = flatComments.size();
        }
        for (int i = 0; i < number; i++) {
            comments.add(flatComments.get(flatComments.size() - 1 - i));
        }
        return comments;
    }

    public List<UIComment> getLastCommentsByDate(String commentNumber)
            throws ClientException {
        return getLastCommentsByDate(commentNumber, null);
    }

    public String getSavedReplyCommentId() {
        return savedReplyCommentId;
    }

    public void setSavedReplyCommentId(String savedReplyCommentId) {
        this.savedReplyCommentId = savedReplyCommentId;
    }

    public List<Action> getActionsForComment() {
        return webActions.getActionsList(COMMENTS_ACTIONS);
    }

    public List<Action> getActionsForComment(String category) {
        return webActions.getActionsList(category);
    }

    public boolean getShowCreateForm() {
        return showCreateForm;
    }

    public void setShowCreateForm(boolean flag) {
        showCreateForm = flag;
    }

    public void toggleCreateForm(ActionEvent event) {
        showCreateForm = !showCreateForm;
    }

    public void cleanContextVariable() {
        commentableDoc = null;
        uiComments = null;
        commentThread = null;
        showCreateForm = false;
        commentStarted = false;
        savedReplyCommentId = null;
        newContent = null;
    }

}

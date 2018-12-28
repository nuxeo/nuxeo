/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.comment.workflow.utils.FollowTransitionUnrestricted;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.security.UserSession;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public abstract class AbstractCommentManagerActionsBean implements CommentManagerActions {

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

    @Override
    @Create
    public void initialize() {
        log.debug("Initializing...");
        commentMap = new HashMap<>();
        showCreateForm = false;

        principal = userSession.getCurrentNuxeoPrincipal();
        principalIsAdmin = principal.isAdministrator();
    }

    @Override
    @Destroy
    public void destroy() {
        commentMap = null;
        log.debug("Removing Seam action listener...");
    }

    @Override
    public String getPrincipalName() {
        return principal.getName();
    }

    @Override
    public boolean getPrincipalIsAdmin() {
        return principalIsAdmin;
    }

    protected DocumentModel initializeComment(DocumentModel comment) {
        if (comment != null) {
            if (comment.getProperty("dublincore", "contributors") == null) {
                String[] contributors = new String[1];
                contributors[0] = getPrincipalName();
                comment.setProperty("dublincore", "contributors", contributors);
            }
            if (comment.getProperty("dublincore", "created") == null) {
                comment.setProperty("dublincore", "created", Calendar.getInstance());
            }
        }
        return comment;
    }

    public DocumentModel addComment(DocumentModel comment, DocumentModel docToComment) {
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
            throw new NuxeoException("Can't comment on null document");
        }
        DocumentModel newComment;
        if (parentComment != null) {
            newComment = commentableDoc.addComment(parentComment.getComment(), comment);
        } else {
            newComment = commentableDoc.addComment(comment);
        }

        // automatically validate the comments
        if (CommentsConstants.COMMENT_LIFECYCLE.equals(newComment.getLifeCyclePolicy())) {
            new FollowTransitionUnrestricted(documentManager, newComment.getRef(),
                    CommentsConstants.TRANSITION_TO_PUBLISHED_STATE).runUnrestricted();
        }

        // Events.instance().raiseEvent(CommentEvents.COMMENT_ADDED, null,
        // newComment);
        cleanContextVariable();

        return newComment;
    }

    @Override
    public DocumentModel addComment(DocumentModel comment) {
        return addComment(comment, null);
    }

    @Override
    public String addComment() {
        DocumentModel myComment = documentManager.createDocumentModel(CommentsConstants.COMMENT_DOC_TYPE);

        myComment.setPropertyValue(CommentsConstants.COMMENT_AUTHOR, principal.getName());
        myComment.setPropertyValue(CommentsConstants.COMMENT_TEXT, newContent);
        myComment.setPropertyValue(CommentsConstants.COMMENT_CREATION_DATE, Calendar.getInstance());
        myComment = addComment(myComment);

        // do not navigate to newly-created comment, they are hidden documents
        return null;
    }

    @Override
    public String createComment(DocumentModel docToComment) {
        DocumentModel myComment = documentManager.createDocumentModel(CommentsConstants.COMMENT_DOC_TYPE);

        myComment.setProperty("comment", "author", principal.getName());
        myComment.setProperty("comment", "text", newContent);
        myComment.setProperty("comment", "creationDate", Calendar.getInstance());
        myComment = addComment(myComment, docToComment);

        // do not navigate to newly-created comment, they are hidden documents
        return null;
    }

    @Override
    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED, EventNames.CONTENT_ROOT_SELECTION_CHANGED,
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
    @Override
    public void initComments() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            throw new NuxeoException("Unable to find current Document");
        }
        initComments(currentDoc);
    }

    /**
     * Initializes uiComments with Comments of current document.
     */
    @Override
    public void initComments(DocumentModel commentedDoc) {
        commentableDoc = getCommentableDoc(commentedDoc);
        if (uiComments == null) {
            uiComments = new ArrayList<>();
            if (commentableDoc != null) {
                List<DocumentModel> comments = commentableDoc.getComments();
                for (DocumentModel comment : comments) {
                    UIComment uiComment = createUIComment(null, comment);
                    uiComments.add(uiComment);
                }
            }
        }
    }

    public List<UIComment> getComments(DocumentModel doc) {
        List<UIComment> allComments = new ArrayList<>();
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
    @Override
    public List<ThreadEntry> getCommentsAsThreadOnDoc(DocumentModel doc) {
        List<ThreadEntry> allComments = new ArrayList<>();
        List<UIComment> allUIComments = getComments(doc);

        for (UIComment uiComment : allUIComments) {
            allComments.add(new ThreadEntry(uiComment.getComment(), 0));
            if (uiComment.getChildren() != null) {
                flattenTree(allComments, uiComment, 0);
            }
        }
        return allComments;
    }

    @Override
    public List<ThreadEntry> getCommentsAsThread(DocumentModel commentedDoc) {
        List<ThreadEntry> commentThread = new ArrayList<>();
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
     * Visits a list of comment trees and puts them into a list of "ThreadEntry"s.
     */
    public void flattenTree(List<ThreadEntry> commentThread, UIComment uiComment, int depth) {
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
    protected UIComment createUIComment(UIComment parent, DocumentModel comment) {
        UIComment wrapper = new UIComment(parent, comment);
        commentMap.put(wrapper.getId(), wrapper);
        List<DocumentModel> children = commentableDoc.getComments(comment);
        for (DocumentModel child : children) {
            UIComment uiChild = createUIComment(wrapper, child);
            wrapper.addChild(uiChild);
        }
        return wrapper;
    }

    @Override
    public String deleteComment(String commentId) {
        if ("".equals(commentId)) {
            log.error("No comment id to delete");
            return null;
        }
        if (commentableDoc == null) {
            log.error("Can't delete comments of null document");
            return null;
        }
        UIComment selectedComment = commentMap.get(commentId);
        commentableDoc.removeComment(selectedComment.getComment());
        cleanContextVariable();
        // Events.instance().raiseEvent(CommentEvents.COMMENT_REMOVED, null,
        // selectedComment.getComment());
        return null;
    }

    @Override
    public String deleteComment() {
        return deleteComment(deleteCommentId);
    }

    @Override
    public String getNewContent() {
        return newContent;
    }

    @Override
    public void setNewContent(String newContent) {
        this.newContent = newContent;
    }

    @Override
    public String beginComment() {
        commentStarted = true;
        savedReplyCommentId = replyCommentId;
        showCreateForm = false;
        return null;
    }

    @Override
    public String cancelComment() {
        cleanContextVariable();
        return null;
    }

    @Override
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

    @Override
    @SuppressWarnings("unchecked")
    public List<UIComment> getLastCommentsByDate(String commentNumber, DocumentModel commentedDoc)
            {
        int number = Integer.parseInt(commentNumber);
        List<UIComment> comments = new ArrayList<>();
        flatComments = new ArrayList<>();

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

    @Override
    public List<UIComment> getLastCommentsByDate(String commentNumber) {
        return getLastCommentsByDate(commentNumber, null);
    }

    @Override
    public String getSavedReplyCommentId() {
        return savedReplyCommentId;
    }

    @Override
    public void setSavedReplyCommentId(String savedReplyCommentId) {
        this.savedReplyCommentId = savedReplyCommentId;
    }

    @Override
    public List<Action> getActionsForComment() {
        return webActions.getActionsList(COMMENTS_ACTIONS);
    }

    @Override
    public List<Action> getActionsForComment(String category) {
        return webActions.getActionsList(category);
    }

    @Override
    public boolean getShowCreateForm() {
        return showCreateForm;
    }

    @Override
    public void setShowCreateForm(boolean flag) {
        showCreateForm = flag;
    }

    @Override
    public void toggleCreateForm(ActionEvent event) {
        showCreateForm = !showCreateForm;
    }

    public void cleanContextVariable() {
        commentableDoc = null;
        uiComments = null;
        showCreateForm = false;
        commentStarted = false;
        savedReplyCommentId = null;
        newContent = null;
        // NXP-11462: reset factory to force comment fetching after the new
        // comment is added
        Contexts.getEventContext().remove("documentThreadedComments");
    }

}

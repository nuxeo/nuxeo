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
 *     ${user}
 *
 * $Id
 */
package org.nuxeo.ecm.platform.forum.web;

import static org.jboss.seam.ScopeType.EVENT;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import javax.ejb.Remove;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.comment.web.CommentManagerActions;
import org.nuxeo.ecm.platform.comment.web.ThreadEntry;
import org.nuxeo.ecm.platform.forum.web.api.PostAction;
import org.nuxeo.ecm.platform.forum.web.api.ThreadAction;
import org.nuxeo.ecm.platform.forum.web.api.ThreadAdapter;
import org.nuxeo.ecm.platform.forum.workflow.ForumConstants;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;

/**
 * This Action Listener represents a Thread inside a forum.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
@Name("threadAction")
@Scope(ScopeType.CONVERSATION)
public class ThreadActionBean extends InputController implements ThreadAction {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ThreadActionBean.class);

    protected final String schema = "thread";

    protected final String type = "Thread";

    protected boolean principalIsAdmin;

    @In(create = true)
    protected Principal currentUser;

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(create = true)
    protected DocumentActions documentActions;

    @In(create = true)
    protected PrincipalListManager principalListManager;

    @In(create = true)
    protected CommentManagerActions commentManagerActions;

    @In(create = true)
    protected PostAction postAction;

    private String title;

    private String description;

    private boolean moderated;

    private NuxeoPrincipal principal;

    public String addThread() throws ClientException {

        // The thread to be created
        DocumentModel docThread = getThreadModel();

        docThread = documentManager.createDocument(docThread);
        documentManager.save();

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                currentDocument);
        clean();
        return navigationContext.navigateToDocument(docThread, "after-create");
    }

    /**
     * Clean variables.
     */
    private void clean() {
        title = null;
        description = null;
        moderated = false;
    }

    /**
     * Get the Thread to create as a DocumentModel.
     *
     * @return
     * @throws ClientException
     */
    private DocumentModel getThreadModel() throws ClientException {

        String path = currentDocument.getPathAsString();
        String docId = IdUtils.generateId(title);

        List<String> moderators = principalListManager.getSelectedUsers();

        final DocumentModel docThread = documentManager.createDocumentModel(
                path, docId, type);
        docThread.setProperty("dublincore", "title", title);
        docThread.setProperty("dublincore", "description", description);
        docThread.setProperty(schema, "moderated", moderated);

        if (moderated) {
            // We automatically add administrators as moderators
            if (!moderators.contains("administrators")) {
                moderators.add("administrators");
            }
            // We can also remove Administrator() since his group is added
            if (moderators.contains("Administrator()")) {
                moderators.remove("Administrator()");
            }
            docThread.setProperty(schema, "moderators", moderators);
        }

        return docThread;
    }

    public List<String> getModerators() {
        DocumentModel currentThread = navigationContext.getCurrentDocument();

        return (List<String>) currentThread.getProperty("thread","moderators");
    }

    public boolean isPrincipalModerator() {
        principal = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        List<String> moderators = getModerators();

        boolean moderator = false;
        if (isPrincipalGroupModerator()
                || (moderators != null && moderators.contains(principal.getName()))) {
            moderator = true;
        }
        return moderator;
    }

    public boolean isPrincipalGroupModerator() {

        List<String> moderators = getModerators();
        List<String> principalGroups = principal.getAllGroups();

        for (String principalGroup : principalGroups) {
            if (moderators != null && moderators.contains(principalGroup)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCurrentThreadModerated() throws ClientException {

        DocumentModel currentThread = navigationContext.getCurrentDocument();
        return isThreadModerated(currentThread);
    }

    @Factory(value = "currentThreadPosts", scope = EVENT)
    public List<ThreadEntry> getPostsAsThread() throws ClientException {
        List<ThreadEntry> basicCommentList = null;

        // Thread is not moderated, we return all Posts
        if (!isCurrentThreadModerated()) {
            basicCommentList = commentManagerActions.getCommentsAsThread();
        } else {
            // Here we clean the list according the rights of principal.
            basicCommentList = new ArrayList<ThreadEntry>();
            List<ThreadEntry> allThreadEntry = commentManagerActions.getCommentsAsThread();

            for (ThreadEntry threadEntry : allThreadEntry) {
                // if current user is not moderator and post is not published,
                // we remove it
                DocumentModel dm = threadEntry.getComment();

                String[] contributorsArray = (String[]) dm.getProperty(
                        "dublincore", "contributors");
                if (contributorsArray == null) {
                    contributorsArray = new String[0];
                }
                List<String> cs = Arrays.asList(contributorsArray);

                if (postAction.isPostPublished(threadEntry.getComment())
                        || isPrincipalModerator()
                        || cs.contains(currentUser.getName())) {
                    basicCommentList.add(threadEntry);
                }
            }
        }
        return basicCommentList;
    }


    public ThreadAdapter getAdapter(DocumentModel thread)
    {
        return thread.getAdapter(ThreadAdapter.class);
    }

    public List<DocumentModel> getAllPosts(DocumentModel thread, String state)
            throws ClientException {

        thread = getDocumentThreadModel(thread.getRef());
        List<DocumentModel> allPosts = Collections.emptyList();
        List<ThreadEntry> allThreadEntry = Collections.emptyList();

        if (thread != null) {
            allThreadEntry = commentManagerActions.getCommentsAsThreadOnDoc(thread);
        }
        if (allThreadEntry != null && !allThreadEntry.isEmpty()) {
            allPosts = new ArrayList<DocumentModel>();
            for (ThreadEntry entry : allThreadEntry) {
                if (!"".equals(state)
                        && state.equals(entry.getComment().getCurrentLifeCycleState())) {
                    allPosts.add(entry.getComment());
                } else if ("".equals(state)) {
                    allPosts.add(entry.getComment());
                }
            }

        }

        return allPosts;
    }

    public List<DocumentModel> getPostsPublished(DocumentModel thread)
            throws ClientException {
        return getAllPosts(thread, ForumConstants.PUBLISHED_STATE);
    }

    public List<DocumentModel> getPostsPending(DocumentModel thread)
            throws ClientException {
        return getAllPosts(thread, ForumConstants.PENDING_STATE);
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSchema() {
        return schema;
    }

    public String getType() {
        return type;
    }

    public void saveState() {
        log.info("PrePassivate");
    }

    public void readState() {
        log.info("PostActivate");
    }

    @Remove
    public void destroy() {
        log.debug("Removing SEAM action listener...");
    }

    public boolean isModerated() {
        return moderated;
    }

    public void setModerated(boolean moderated) {
        this.moderated = moderated;
    }

    public DocumentModel getLastPostPublished(DocumentModel thread)
            throws ClientException {

        thread = getDocumentThreadModel(thread.getRef());
        List<DocumentModel> posts = getPostsPublished(thread);
        DocumentModel lastPost = null;
        if (!posts.isEmpty()) {
            lastPost = posts.get(0);
            for (DocumentModel post : posts) {
                GregorianCalendar lastPostDate = (GregorianCalendar) lastPost.getProperty(
                        "post", "creationDate");
                GregorianCalendar postDate = (GregorianCalendar) post.getProperty(
                        "post", "creationDate");
                if (postDate != null && postDate.after(lastPostDate)) {
                    lastPost = post;
                }
            }

        }
        return lastPost;
    }

    public String getModerationAsString(DocumentModel thread) throws ClientException{
        if (isThreadModerated(thread)) {
            return resourcesAccessor.getMessages().get(
                    "label.forum.thread.moderated.yes");
        }
        return resourcesAccessor.getMessages().get(
                "label.forum.thread.moderated.no");
    }

    public boolean isThreadModerated(DocumentModel thread) throws ClientException{
        thread = getDocumentThreadModel(thread.getRef());
        if (thread != null) {
            Boolean moderation = (Boolean) thread.getProperty("thread",
                    "moderated");
            if (moderation != null) {
                return moderation.booleanValue();
            }
        }

        return false;
    }

    public DocumentModel getParentPost(int post) throws ClientException {

        DocumentModel parentPost = null;

        List<ThreadEntry> posts = getPostsAsThread();
        if (post > 0 && post <= posts.size()) {
            ThreadEntry parent = posts.get(post - 1);
            ThreadEntry currentPost = posts.get(post);
            if (currentPost.getDepth() == parent.getDepth()+1) {
                parentPost = parent.getComment();
            }
        }
        return parentPost;
    }
    public boolean isParentPostPublished(int post) throws ClientException {

       DocumentModel parent = getParentPost(post);
       if (parent == null) {
           return true;

       }
       else if(ForumConstants.PUBLISHED_STATE.equals(parent.getCurrentLifeCycleState())) {
           return true;
        }
       return false;
    }

    /**
     * Get the thread for a given document reference
     * @param threadRef
     * @return
     */
    private DocumentModel getDocumentThreadModel(DocumentRef threadRef) throws ClientException{
        DocumentModel thread = null;
        if (threadRef != null){
            thread = documentManager.getDocument(threadRef);
        }
        return thread;
    }

}

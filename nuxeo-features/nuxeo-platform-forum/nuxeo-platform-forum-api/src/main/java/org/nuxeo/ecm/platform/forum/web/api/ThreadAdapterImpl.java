/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.forum.web.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.forum.workflow.ForumConstants;

public class ThreadAdapterImpl implements ThreadAdapter, Serializable {

    private static final long serialVersionUID = 1876878787587L;

    private final DocumentModel threadDoc;

    private List<DocumentModel> posts;

    private List<DocumentModel> publishedPosts;

    private List<DocumentModel> pendingPosts;

    private DocumentModel lastPublishedPost;

    public ThreadAdapterImpl(DocumentModel threadDoc) {
        this.threadDoc = threadDoc;
    }

    private void fetchAllPosts() {
        posts = getSubComments(threadDoc);
    }

    public List<DocumentModel> getAllPosts() {
        if (posts == null) {
            fetchAllPosts();
        }
        return posts;
    }

    public List<DocumentModel> getPublishedPosts() {
        if (publishedPosts == null) {
            publishedPosts = new ArrayList<DocumentModel>();
            for (DocumentModel doc : getAllPosts()) {
                if (ForumConstants.PUBLISHED_STATE.equals(doc.getCurrentLifeCycleState())) {
                    publishedPosts.add(doc);
                }
            }
        }
        return publishedPosts;
    }

    public List<DocumentModel> getPendingPosts() {
        if (pendingPosts == null) {
            pendingPosts = new ArrayList<DocumentModel>();
            for (DocumentModel doc : getAllPosts()) {
                if (ForumConstants.PENDING_STATE.equals(doc.getCurrentLifeCycleState())) {
                    pendingPosts.add(doc);
                }
            }
        }
        return pendingPosts;
    }

    public DocumentModel getLastPublishedPost() {
        if (lastPublishedPost == null) {
            GregorianCalendar lastPostDate = null;
            for (DocumentModel post : getPublishedPosts()) {
                GregorianCalendar postDate = (GregorianCalendar) post.getProperty("post", "creationDate");

                if (lastPostDate == null || postDate.after(lastPostDate)) {
                    lastPostDate = postDate;
                    lastPublishedPost = post;
                }
            }
        }
        return lastPublishedPost;
    }

    protected List<DocumentModel> getSubComments(DocumentModel doc) {
        List<DocumentModel> allSubPosts = new ArrayList<DocumentModel>();
        CommentableDocument commentDoc = doc.getAdapter(CommentableDocument.class);

        if (commentDoc != null) {
            List<DocumentModel> childComments = commentDoc.getComments();
            for (DocumentModel childComment : childComments) {
                allSubPosts.add(childComment);
                allSubPosts.addAll(getSubComments(childComment));
            }
        }
        return allSubPosts;
    }

    public DocumentModel getThreadDoc() {
        return threadDoc;
    }

}

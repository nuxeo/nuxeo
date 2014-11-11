/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.forum.web.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
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

    private void fetchAllPosts() throws ClientException {
        posts = getSubComments(threadDoc);
    }

    public List<DocumentModel> getAllPosts() throws ClientException {
        if (posts == null) {
            fetchAllPosts();
        }
        return posts;
    }

    public List<DocumentModel> getPublishedPosts() throws ClientException {
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

    public List<DocumentModel> getPendingPosts() throws ClientException {
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

    public DocumentModel getLastPublishedPost() throws ClientException {
        if (lastPublishedPost == null) {
            GregorianCalendar lastPostDate = null;
            for (DocumentModel post : getPublishedPosts()) {
                GregorianCalendar postDate = (GregorianCalendar) post.getProperty(
                        "post", "creationDate");

                if (lastPostDate == null || postDate.after(lastPostDate)) {
                    lastPostDate = postDate;
                    lastPublishedPost = post;
                }
            }
        }
        return lastPublishedPost;
    }

    protected List<DocumentModel> getSubComments(DocumentModel doc)
            throws ClientException {
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

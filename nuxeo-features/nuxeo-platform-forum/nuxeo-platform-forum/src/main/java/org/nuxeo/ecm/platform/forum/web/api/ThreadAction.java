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
package org.nuxeo.ecm.platform.forum.web.api;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.web.ThreadEntry;

/**
 * This Action Listener represents a Thread inside a forum.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public interface ThreadAction extends Serializable {

    /**
     * Gets the title of the Thread to be created.
     */
    String getTitle();

    /**
     * Sets the title of the Thread.
     */
    void setTitle(String title);

    /**
     * Gets the description of the Thread.
     */
    String getDescription();

    /**
     * Sets the description of the Thread.
     */
    void setDescription(String description);

    /**
     * Adds the thread inside the forum.
     *
     * @throws ClientException if the Thread can't be created
     */
    String addThread() throws ClientException;

    /**
     * Returns true if the thread is moderated, false otherwise. Just used at
     * creation time.
     */
    boolean isModerated();

    /**
     * Return the label of the moderation state of the thread
     *
     * @param thread is the thread we want
     */
    String getModerationAsString(DocumentModel thread) throws ClientException;

    /**
     * Sets the moderation on a thread.
     */
    void setModerated(boolean moderated);

    /**
     * Get all moderators on the thread.
     */
    List<String> getModerators();

    /**
     * Returns true if the principal (logged user) is a moderator, else
     * otherwise.
     */
    boolean isPrincipalModerator();

    /**
     * Returns true if the principal(s group is a moderator group
     */
    boolean isPrincipalGroupModerator();

    /**
     * Returns true if the thread is moderated, false otherwise. Intends to be
     * used by a Post.
     */
    boolean isCurrentThreadModerated() throws ClientException;

    /**
     * Returns true if the given thread is moderated, false otherwise.
     *
     * @param thread is the thread to test
     */
    boolean isThreadModerated(DocumentModel thread) throws ClientException;

    /**
     * Gets the latest post published in given thread.
     */
    DocumentModel getLastPostPublished(DocumentModel thread)
            throws ClientException;

    /**
     * Gets all available posts in the thread according the Post state and
     * principal rights. I.e., Post that are not published won't be visible for
     * non-moderators.
     *
     * @return a list of ThreadEntry, directly usable for display with
     *         indentation
     */
    List<ThreadEntry> getPostsAsThread() throws ClientException;

    /**
     * Gets all Posts in the Thread with the specified state. Return all posts
     * if state is null.
     */
    List<DocumentModel> getAllPosts(DocumentModel thread, String state)
            throws ClientException;

    /**
     * Gets published posts in a thread.
     */
    List<DocumentModel> getPostsPublished(DocumentModel thread)
            throws ClientException;

    /**
     * Gets pending posts in a thread.
     */
    List<DocumentModel> getPostsPending(DocumentModel thread)
            throws ClientException;

    /**
     * Return the parent post of the specified index of the post in the
     * getPostsAsThread() list.
     */
    DocumentModel getParentPost(int post) throws ClientException;

    /**
     * Return true if the parent post identified by it's number in the
     * getPostsAsThread list is published.
     */
    boolean isParentPostPublished(int post) throws ClientException;

    ThreadAdapter getAdapter(DocumentModel thread);

}

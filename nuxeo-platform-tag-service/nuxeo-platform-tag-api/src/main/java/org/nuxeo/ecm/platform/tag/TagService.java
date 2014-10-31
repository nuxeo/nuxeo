/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Radu Darlea
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * The Tag Service interface. It gathers the entire service API. The available
 * capabilities are:
 * <ul>
 * <li>list the tags, either related or not to a document
 * <li>create tags and taggings
 * <li>obtain tag cloud
 * </ul>
 */
public interface TagService {

    String ID = "org.nuxeo.ecm.platform.tag.TagService";

    /**
     * Defines if tag service is enable.
     *
     * @return true if the underlying repository supports the tag feature
     */
    boolean isEnabled() throws ClientException;

    /**
     * Tags a document with a given tag.
     *
     * @param session the session
     * @param docId the document id
     * @param label the tag
     * @param username the user associated to the tagging
     */
    void tag(CoreSession session, String docId, String label, String username)
            throws ClientException;

    /**
     * Untags a document of the given tag
     *
     * @param session the session
     * @param docId the document id
     * @param label the tag, or {@code null} for all tags
     * @param username the user associated to the tagging
     */
    void untag(CoreSession session, String docId, String label, String username)
            throws ClientException;

    /**
     * Gets the tags applied to a document by a given user, or by all users.
     *
     * @param session the session
     * @param docId the document id
     * @param username the user name, or {@code null} for all users
     * @return the list of tags
     */
    List<Tag> getDocumentTags(CoreSession session, String docId, String username)
            throws ClientException;

    /**
     * Gets the tags applied to a document by a given user, or by all users.
     * <p>
     * Alternative method allowing to specify whether the core should be used
     * for this query.
     *
     * @param session the session
     * @param docId the document id
     * @param username the user name, or {@code null} for all users
     * @param useCore if true, the core should be used to retrieve tags.
     * @return the list of tags
     * @since 6.0
     */
    List<Tag> getDocumentTags(CoreSession session, String docId,
            String username, boolean useCore) throws ClientException;

    /**
     * Removes all the tags applied to a document.
     *
     * @since 5.7.3
     */
    void removeTags(CoreSession session, String docId) throws ClientException;

    /**
     * Copy all the tags applied to the source document to the destination
     * document.
     * <p>
     * The tags are merged.
     *
     * @param srcDocId the source document id
     * @param dstDocId the destination document id
     * @since 5.7.3
     */
    void copyTags(CoreSession session, String srcDocId, String dstDocId)
            throws ClientException;

    /**
     * Replace all the existing tags applied on the destination document by the
     * ones applied on the source document.
     *
     * @param srcDocId the source document id
     * @param dstDocId the destination document id
     * @since 5.7.3
     */
    void replaceTags(CoreSession session, String srcDocId, String dstDocId)
            throws ClientException;

    /**
     * Gets the documents to which a tag is applied.
     *
     * @param session the session
     * @param label the tag
     * @param username the user name, or {@code null} for all users
     * @return the set of document ids
     */
    List<String> getTagDocumentIds(CoreSession session, String label,
            String username) throws ClientException;

    /**
     * Gets the tag cloud for a set of documents (tags with weight
     * corresponding to their popularity).
     * <p>
     * If a docId is passed, only documents under it are considered, otherwise
     * all documents in the database are used.
     * <p>
     * The cloud is returned unsorted.
     *
     * @param session the session
     * @param docId the document id under which to look, or {@code null} for
     *            all documents
     * @param username the user name, or {@code null} for all users
     * @param normalize null for no weight normalization (a count is returned),
     *            {@code FALSE} for 0-100 normalization, {@code TRUE} for
     *            logarithmic 0-100 normalization
     * @return the cloud (a list of weighted tags)
     */
    List<Tag> getTagCloud(CoreSession session, String docId, String username,
            Boolean normalize) throws ClientException;

    /**
     * Gets suggestions for a given tag label prefix.
     *
     * @param session the session
     * @param label the tag label prefix
     * @param username the user name, or {@code null} for all users
     * @return a list of tags
     */
    List<Tag> getSuggestions(CoreSession session, String label, String username)
            throws ClientException;

}

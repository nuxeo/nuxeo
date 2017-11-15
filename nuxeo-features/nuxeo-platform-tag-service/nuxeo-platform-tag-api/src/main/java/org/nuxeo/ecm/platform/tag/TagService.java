/*
 * (C) Copyright 2009-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Radu Darlea
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.tag;

import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * The Tag Service interface. It gathers the entire service API. The available capabilities are:
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
    boolean isEnabled();

    /**
     * Tags a document with a given tag.
     *
     * @param session the session
     * @param docId the document id
     * @param label the tag
     * @since 9.3
     */
    void tag(CoreSession session, String docId, String label);

    /**
     * Tags a document with a given tag.
     *
     * @param session the session
     * @param docId the document id
     * @param label the tag
     * @param username the user associated to the tagging
     * @deprecated since 9.3, username is not needed anymore
     */
    @Deprecated
    void tag(CoreSession session, String docId, String label, String username);

    /**
     * Untags a document of the given tag
     *
     * @param session the session
     * @param docId the document id
     * @param label the tag, or {@code null} for all tags
     */
    void untag(CoreSession session, String docId, String label);

    /**
     * Untags a document of the given tag
     *
     * @param session the session
     * @param docId the document id
     * @param label the tag, or {@code null} for all tags
     * @param username the user associated to the tagging
     * @deprecated since 9.3, username is not needed anymore
     */
    @Deprecated
    void untag(CoreSession session, String docId, String label, String username);

    /**
     * Returns whether or not the current session can untag tag on provided document.
     *
     * @param session the session
     * @param docId the document id
     * @param label the tag, or {@code null} for all tags
     * @return whether or not the current session can untag provided document
     * @since 8.4
     */
    boolean canUntag(CoreSession session, String docId, String label);

    /**
     * Gets the tags applied to a document.
     *
     * @param session the session
     * @param docId the document id
     * @return the list of tags
     * @since 9.3
     */
    Set<String> getTags(CoreSession session, String docId);

    /**
     * Gets the tags applied to a document by a given user, or by all users.
     *
     * @param session the session
     * @param docId the document id
     * @param username the user name, or {@code null} for all users
     * @return the list of tags
     * @deprecated since 9.3, username is not needed anymore
     */
    @Deprecated
    List<Tag> getDocumentTags(CoreSession session, String docId, String username);

    /**
     * Gets the tags applied to a document by a given user, or by all users.
     * <p>
     * Alternative method allowing to specify whether the core should be used for this query.
     *
     * @param session the session
     * @param docId the document id
     * @param username the user name, or {@code null} for all users
     * @param useCore if true, the core should be used to retrieve tags.
     * @return the list of tags
     * @since 6.0
     * @deprecated since 9.3, username and useCore are not needed anymore
     */
    @Deprecated
    List<Tag> getDocumentTags(CoreSession session, String docId, String username, boolean useCore);

    /**
     * Removes all the tags applied to a document.
     *
     * @since 5.7.3
     */
    void removeTags(CoreSession session, String docId);

    /**
     * Copy all the tags applied to the source document to the destination document.
     * <p>
     * The tags are merged.
     *
     * @param srcDocId the source document id
     * @param dstDocId the destination document id
     * @since 5.7.3
     */
    void copyTags(CoreSession session, String srcDocId, String dstDocId);

    /**
     * Replace all the existing tags applied on the destination document by the ones applied on the source document.
     *
     * @param srcDocId the source document id
     * @param dstDocId the destination document id
     * @since 5.7.3
     */
    void replaceTags(CoreSession session, String srcDocId, String dstDocId);

    /**
     * Gets the documents to which a tag is applied.
     *
     * @param session the session
     * @param label the tag
     * @return the set of document ids
     * @since 9.3
     */
    List<String> getTagDocumentIds(CoreSession session, String label);

    /**
     * Gets the documents to which a tag is applied.
     *
     * @param session the session
     * @param label the tag
     * @param username the user name, or {@code null} for all users
     * @return the set of document ids
     * @deprecated since 9.3, username is not needed anymore
     */
    @Deprecated
    List<String> getTagDocumentIds(CoreSession session, String label, String username);

    /**
     * Gets the tag cloud for a set of documents (tags with weight corresponding to their popularity).
     * <p>
     * If a docId is passed, only documents under it are considered, otherwise all documents in the database are used.
     * <p>
     * The cloud is returned unsorted.
     *
     * @param session the session
     * @param docId the document id under which to look, or {@code null} for all documents
     * @param username the user name, or {@code null} for all users
     * @param normalize null for no weight normalization (a count is returned), {@code FALSE} for 0-100 normalization,
     *            {@code TRUE} for logarithmic 0-100 normalization
     * @return the cloud (a list of weighted tags)
     * @deprecated since 9.3, seems unused
     */
    @Deprecated
    List<Tag> getTagCloud(CoreSession session, String docId, String username, Boolean normalize);

    /**
     * Gets suggestions for a given tag label prefix.
     *
     * @param session the session
     * @param label the tag label prefix
     * @return a list of tags
     * @since 9.3
     */
    Set<String> getSuggestions(CoreSession session, String label);

    /**
     * Gets suggestions for a given tag label prefix.
     *
     * @param session the session
     * @param label the tag label prefix
     * @param username the user name, or {@code null} for all users
     * @return a list of tags
     * @deprecated since 9.3, username is not needed anymore
     */
    @Deprecated
    List<Tag> getSuggestions(CoreSession session, String label, String username);

    /**
     * Features of the implementation of the service.
     *
     * @see TagService#hasFeature
     * @since 9.3
     */
    enum Feature {
        /** Tags are properties of the document itself. */
        TAGS_BELONG_TO_DOCUMENT,
    }

    /**
     * Checks if a feature is available.
     *
     * @since 9.3
     */
    boolean hasFeature(Feature feature);

    /**
     * Checks if document support tag.
     *
     * @since 9.3
     */
    boolean supportsTag(CoreSession session, String docId);

}

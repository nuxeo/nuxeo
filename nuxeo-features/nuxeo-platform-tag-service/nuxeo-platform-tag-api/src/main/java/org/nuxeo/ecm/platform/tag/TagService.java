/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.tag;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * The Tag Service interface. It gathers the entire service API. The available
 * capabilities are:
 * <ul>
 * <li>list the tags, either related or not to a document
 * <li>create tags and taggings <li>obtain tag cloud
 * </ul>
 * <p>
 * As general rule, the flag private / public is always applied. It could be
 * ignored in the future: just simplify the queries. The service is using super
 * user, allowing anyone see / creates tags. The service retrieve the user name
 * from the attached document session. If you're using a detached document, you
 * should provide the core session yourself providing it as parameter.
 *
 * @author rux
 */
public interface TagService {

    String ID = "org.nuxeo.ecm.platform.tag.TagService";

    /**
     * Gets (and creates if not existing) the RootTag in the selected
     * repository.
     * <p>
     * The current functionality is to keep all tags inside
     * "/repository/default-domain/Tags". If this will be changed this method is
     * simply obsoleted, anyway the API requests to provide the parents of the
     * tags.
     *
     * @param session the user session
     * @return the possible newly created tag root.
     * @throws ClientException
     */
    DocumentModel getRootTag(CoreSession session) throws ClientException;

    /**
     * Lists all tags in the specified tag group.
     * <p>
     * Tags are saved in a tree structure, any tag can actually contains other
     * tags. This way it is possible to have for instance categories of tags, or
     * any other organization the user would want (e.g a tag "car" could contain
     * other tags like "family", "sport", "classic" which obvious would have the
     * meaning related to automobiles and not the more usual meanings). The
     * private tags are not selected, but the ones owned by the current
     * principal. It goes recursively and collects all tags underneath. To list
     * all tags in a repository use as parent {@link #getRootTag(String)} or
     * simply the root domain.
     *
     * @param session the user session
     * @param tag group targeted
     * @return
     * @throws ClientException
     */
    DocumentModelList listTagsInGroup(CoreSession session, DocumentModel tag)
            throws ClientException;

    /**
     * Lists tags applied on a document.
     * <p>
     * The private tags or tagging are not selected, but the ones owned by the
     * current principal.
     *
     * @param session the user session
     * @param document
     * @return
     * @throws ClientException
     */
    List<Tag> listTagsAppliedOnDocument(CoreSession session,
            DocumentModel document) throws ClientException;

    /**
     * Returns author of a tagging based on docId and the tagLabel.
     *
     * @param session the user session
     * @param docId
     * @param tagLabel
     * @param author
     * @return
     * @throws ClientException
     */
    String getTaggingId(CoreSession session, String docId, String tagLabel,
            String author) throws ClientException;

    /**
     * Lists tags applied on a document by the current principal.
     *
     * @param session the user session
     * @param document
     * @return
     * @throws ClientException
     */
    List<Tag> listTagsAppliedOnDocumentByUser(CoreSession session,
            DocumentModel document) throws ClientException;

    /**
     * Gets and creates if needed a tag in the provided tag group (or in tag
     * root).
     * <p>
     * Interprets label as sequence of labels '/' separated.
     *
     * @param session the user session
     * @param parent must not be null
     * @param label the tag label. It will also be the title.
     * @param privateFlag
     * @return
     * @throws ClientException
     */
    DocumentModel getOrCreateTag(CoreSession session, DocumentModel parent,
            String label, boolean privateFlag) throws ClientException;

    /**
     * Retrieves the "vote" weight of tag.
     * <p>
     * More about Vote Tag Cloud {@link WeightedTag}. The private taggings are
     * not selected, but the ones owned by the current principal.
     *
     * @param session the user session
     * @param document the tagged document
     * @param tagId
     * @return
     * @throws ClientException
     */
    WeightedTag getVoteTag(CoreSession session, DocumentModel document,
            String tagId) throws ClientException;

    /**
     * Retrieves the "popular" weight of tag.
     * <p>
     * More about Vote Tag Cloud {@link WeightedTag}. The private tagging are
     * not selected, but the ones owned by the current principal.
     *
     * @param session the user session
     * @param document the domain
     * @param tagId
     * @return
     * @throws ClientException
     */
    WeightedTag getPopularTag(CoreSession session, DocumentModel document,
            String tagId) throws ClientException;

    /**
     * Retrieves the "vote" tag cloud. More about Vote Tag Cloud
     * {@link WeightedTag}. The private tags or tagging are not selected, but
     * the ones owned by the current principal.
     *
     * @param session the user session
     * @param document tagged document
     * @return
     * @throws ClientException
     */
    List<WeightedTag> getVoteCloud(CoreSession session, DocumentModel document)
            throws ClientException;

    /**
     * Retrieves the "popular" tag cloud.
     * <p>
     * More about Popular Tag Cloud {@link WeightedTag}. The private tags or
     * tagging are not selected, but the ones owned by the current principal.
     *
     * @param session the user session
     * @param document
     * @return
     * @throws ClientException
     */
    List<WeightedTag> getPopularCloud(CoreSession session,
            DocumentModel document) throws ClientException;


    /**
     * Retrieves the "popular" tag cloud without Document context.
     * <p>
     * Compared to getPopularCloud, this method does not filter on documents
     * accessible by the user.
     * This makes this feature usable on big doc DB.
     *
     * @param session
     * @return
     * @throws ClientException
     */
    List<WeightedTag> getPopularCloudOnAllDocuments(CoreSession session) throws ClientException;

    /**
     * Tags a document.
     * <p>
     * It only creates the tagging entry, if not already a tagging exists for
     * the set document / tag / current principal. Otherwise silently ignore.
     *
     * @param session the user session
     * @param document document to tag
     * @param tagId tag to be applied
     * @throws ClientException
     */
    void tagDocument(CoreSession session, DocumentModel document, String tagId,
            boolean privateFlag) throws ClientException;

    /**
     * Removes a tagging from a document.
     *
     * @param session the user session
     * @param document
     * @param tagId
     * @throws ClientException
     */
    void untagDocument(CoreSession session, DocumentModel document, String tagId)
            throws ClientException;

    /**
     * Removes all taggings from a document.
     *
     * @param session the user session
     * @param document
     * @param tagId
     * @throws ClientException
     */
    void completeUntagDocument(CoreSession session, DocumentModel document,
            String tagId) throws ClientException;

    /**
     * Retrieves the list of documents tagged with a particular tag visible by
     * the current principal.
     *
     * @param session the user session
     * @param tagId
     * @param user
     * @return the list of document id
     * @throws ClientException
     */
    List<String> listDocumentsForTag(CoreSession session, String tagId,
            String user) throws ClientException;

    /**
     * Defines if tag service is enable.
     *
     * @return true is the underlying repository supports tag feature.
     */
    boolean isEnabled() throws ClientException;

}

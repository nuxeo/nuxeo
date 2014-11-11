/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.tag.persistence;

import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.WeightedTag;
import org.nuxeo.ecm.platform.tag.entity.DublincoreEntity;
import org.nuxeo.ecm.platform.tag.entity.TagEntity;
import org.nuxeo.ecm.platform.tag.entity.TaggingEntity;

/**
 * Provider of almost all queries required for Tag service. Implemented as
 * singleton. Stores the EntityManager during service life.
 *
 * @author rux
 *
 */
public class TaggingProvider {

    private static final Log log = LogFactory.getLog(TaggingProvider.class);

    private final EntityManager em;

    private final TagPersistenceProvider tagPersistenceProvider = TagPersistenceProvider.getInstance();

    private TaggingProvider() {
        this.em = tagPersistenceProvider.getEntityManager(null);
        tagPersistenceProvider.createTableTagging(em);
    }

    private TaggingProvider(EntityManager em) {
        this.em = em;
        tagPersistenceProvider.createTableTagging(em);
    }

    public static TaggingProvider createProvider(EntityManager em) {
        return new TaggingProvider(em);
    }

    public static TaggingProvider createProvider() {
        return new TaggingProvider();
    }

    /**
     * Persists to the 'NXP_TAGGING' table the information contained in the
     * received parameter.
     *
     * @param tagging - the information about the 'tagging' that will be
     *            persisted
     */
    public void addTagging(TaggingEntity tagging) {
        if (log.isDebugEnabled()) {
            log.debug("addTagging() with tagging " + tagging.toString());
        }
        try {
            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
                em.persist(tagging);
                tagPersistenceProvider.doCommit(em);
            } else {
                em.persist(tagging);
            }
        } catch (Exception e) {
            tagPersistenceProvider.doRollback(em);
        }
    }

    @SuppressWarnings("unchecked")
    protected List doQuery(String query, List params) {
        Query q = em.createQuery(query);
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }
        return q.getResultList();
    }

    protected List doQuery(String query, Object... params) {
        Query q = em.createQuery(query);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    protected List doNamedQuery(String namedQuery, Map<String, Object> params) {
        Query query = em.createNamedQuery(namedQuery);
        for (String key : params.keySet()) {
            query.setParameter(key, params.get(key));
        }
        return query.getResultList();
    }

    protected Object doNamedQuerySingle(String namedQuery,
            Map<String, Object> params) {
        Query query = em.createNamedQuery(namedQuery);
        for (String key : params.keySet()) {
            query.setParameter(key, params.get(key));
        }
        return query.getSingleResult();
    }

    /**
     * Persists to the 'NXP_TAGGING' table the information contained in the list
     * that is received as parameter.
     *
     * @param taggings - the list with the information about the 'tagging'
     *            entries that will be persisted
     */
    public void addTaggingEntries(List<TaggingEntity> taggings) {
        if (log.isDebugEnabled()) {
            log.debug("addTaggingEntries() for " + taggings.size() + " entries");
        }
        for (TaggingEntity taggin : taggings) {
            addTagging(taggin);
        }
    }

    /**
     * Lists distinct the public tags (or owned by user) that are applied on
     * document.
     *
     * @param docId - the UUID of the tagged document
     * @param userName - the user name of the current logged user
     * @return tags applied as list of simple tags
     */
    @SuppressWarnings("unchecked")
    public List<Tag> listTagsForDocument(String docId, String userName) {
        if (log.isDebugEnabled()) {
            log.debug("listTagsForDocument() with Id " + docId);
        }
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("targetId", docId);
        params.put("userName", userName);
        List<Object[]> queryResults = doNamedQuery(LIST_TAGS_FOR_DOCUMENT,
                params);
        List<Tag> listTagsForDocument = new ArrayList<Tag>();
        for (Object[] queryResult : queryResults) {
            listTagsForDocument.add(new Tag(queryResult[0].toString(),
                    queryResult[1].toString()));
        }
        return listTagsForDocument;
    }

    /**
     * Returns author of a tagging based on docId and the tagLabel.
     *
     * @param docId
     * @param tagLabel
     * @return
     */
    public String getTaggingId(String docId, String tagLabel, String author) {
        final String query = "SELECT tg.id FROM Tagging tg JOIN tg.targetDocument doc JOIN tg.tag tag"
                + " WHERE doc.id = ?1 AND tag.label = ?2 AND tg.author = ?3";

        List<String> authors = doQuery(query, docId, tagLabel, author);

        return authors.size() > 0 ? authors.get(0) : null;
    }

    /**
     * Lists distinct the public tags (or owned by user) that are applied on
     * document by the user only.
     *
     * @param docId - the UUID of the tagged document
     * @param userName - the user name of the current logged user
     * @return tags applied as list of simple tags
     */
    @SuppressWarnings("unchecked")
    public List<Tag> listTagsForDocumentAndUser(String docId, String userName) {
        if (log.isDebugEnabled()) {
            log.debug("listTagsForDocumentAndUser() with Id " + docId);
        }
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("targetId", docId);
        params.put("userName", userName);
        List<Object[]> queryResults = doNamedQuery(
                LIST_TAGS_FOR_DOCUMENT_AND_USER, params);
        List<Tag> listTagsForDocument = new ArrayList<Tag>();
        for (Object[] queryResult : queryResults) {
            listTagsForDocument.add(new Tag(queryResult[0].toString(),
                    queryResult[1].toString()));
        }
        return listTagsForDocument;
    }

    /**
     * Gets vote tag (basically count of how many times the tag was applied on a
     * document).
     *
     * @param docId - the UUID of the tagged document
     * @param tagId - the UUID of the tag document
     * @param userName - the user name of the current logged user
     * @return how many times a tag was applied on a document by different users
     */
    public Long getVoteTag(String docId, String tagId, String userName) {
        if (log.isDebugEnabled()) {
            log.debug("getVoteTag() for " + docId + " and " + tagId);
        }
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("targetId", docId);
        params.put("userName", userName);
        params.put("tagId", tagId);
        return (Long) doNamedQuerySingle(GET_VOTE_TAG, params);
    }

    /**
     * Removes an entry from the 'NXP_TAGGING' table that has the targetId
     * equals to the <b>docId</b> parameter and the tagId equals to the
     * <b>tagId</b> parameter.The author of the entry must be the
     * <b>userName</b> received parameter. The method returns true in case the
     * deleting was successful or false otherwise.
     *
     * @param docId - the UUID of the tagged document
     * @param tagId - the UUID of the tag document
     * @param userName - the user name of the current logged user
     * @return true in case the deleting was successful or false otherwise.
     */
    public boolean removeTagging(String docId, String tagId, String userName) {
        if (log.isDebugEnabled()) {
            log.debug("removeTagging() with targetId " + docId + " and tagId "
                    + tagId);
        }
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
            Query query = em.createNamedQuery(REMOVE_TAGGING);
            query.setParameter("targetId", docId);
            query.setParameter("tagId", tagId);
            query.setParameter("userName", userName);
            int result = query.executeUpdate();
            tagPersistenceProvider.doCommit(em);
            return result == 1;
        }
        return false;
    }

    /**
     * Removes an entry from the 'NXP_TAGGING' table that has the targetId
     * equals to the <b>docId</b> parameter and the tagId equals to the
     * <b>tagId</b> parameter.The author of the entry must be the
     * <b>userName</b> received parameter. The method returns true in case the
     * deleting was successful or false otherwise.
     *
     * @param docId - the UUID of the tagged document
     * @param tagId - the UUID of the tag document
     * @param userName - the user name of the current logged user
     * @return true in case the deleting was successful or false otherwise.
     */
    public void removeAllTagging(String docId, String tagId) {
        if (log.isDebugEnabled()) {
            log.debug("removeTagging() with targetId " + docId + " and tagId "
                    + tagId);
        }
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
            try {
                Query query = em.createQuery("DELETE FROM Tagging tagging "
                        + "WHERE tagging.targetDocument.id=:targetId AND  tagging.tag.id=:tagId");
                query.setParameter("targetId", docId);
                query.setParameter("tagId", tagId);
                query.executeUpdate();
                tagPersistenceProvider.doCommit(em);
            } catch (Exception e) {
                tagPersistenceProvider.doRollback(em);
            }
        }
    }

    public TagEntity getTagById(String tagId) {
        if (log.isDebugEnabled()) {
            log.debug("getTagById() with id " + tagId);
        }
        return em.find(TagEntity.class, tagId);
    }

    public DublincoreEntity getDcById(String docId) {
        if (log.isDebugEnabled()) {
            log.debug("getDcById() with id " + docId);
        }
        return em.find(DublincoreEntity.class, docId);
    }

    /**
     * Retrieves the "popular" tag cloud. More about Vote Tag Cloud
     * {@link WeightedTag}. The private tags or tagging are not selected, but
     * the ones owned by the user. It gets the tags and the number of documents
     * they are applied on the list of documents received as argument.
     *
     * @param documents
     * @param userName
     * @return
     * @throws ClientException
     */
    @SuppressWarnings("unchecked")
    public List<WeightedTag> getPopularCloud(DocumentModelList documents,
            String userName) {
        if (log.isDebugEnabled()) {
            log.debug("getPopularTag() for " + documents.size() + " documents");
        }
        int count = 1;
        StringBuilder sb = new StringBuilder(
                "SELECT tag.id, tag.label, COUNT(DISTINCT tg.targetDocument.id) FROM Tagging tg "
                        + "JOIN tg.tag tag JOIN tag.hierarchy h JOIN h.dublincore dc "
                        + "WHERE tg.targetDocument.id IN ( ");
        List<String> params = new LinkedList<String>();
        for (DocumentModel document : documents) {
            params.add(document.getId());
            sb.append('?').append(count).append(
                    count < documents.size() ? ',' : "");
            count++;
        }
        sb.append(") AND (tg.isPrivate=false OR tg.author=");
        sb.append('?');
        sb.append(count);
        count++;
        sb.append(") AND ");
        sb.append("(tg.tag.private1 = false OR dc.creator = ");
        sb.append('?');
        sb.append(count);
        count++;
        sb.append(") GROUP BY tag.id , tag.label");
        params.add(userName);
        params.add(userName);
        List<Object[]> queryResults = (List<Object[]>) doQuery(sb.toString(),
                params);
        List<WeightedTag> ret = new ArrayList<WeightedTag>();
        for (Object[] queryResult : queryResults) {
            WeightedTag weightedTag = new WeightedTag((String) queryResult[0],
                    (String) queryResult[1], ((Long) queryResult[2]).intValue());
            ret.add(weightedTag);
        }
        return ret;
    }

    /**
     * Lists distinct the documents tagged with specified tag.
     *
     * @param tagId the tag applied
     * @param userName user
     * @return a map document ID - document title
     */
    @SuppressWarnings("unchecked")
    public List<String> getDocumentsForTag(String tagId, String userName) {
        if (log.isDebugEnabled()) {
            log.debug("getDocumentsForTag() with Id " + tagId);
        }
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userName", userName);
        params.put("tagId", tagId);
        List<Object> queryResults = doNamedQuery(LIST_DOCUMENTS_FOR_TAG, params);
        List<String> ret = new ArrayList<String>();
        for (Object queryResult : queryResults) {
            ret.add(queryResult.toString());
        }
        return ret;
    }

    /**
     * Checks if a particular tag was applied on specified daocument by user.
     *
     * @param tagId
     * @param docId
     * @param userName
     * @return
     */
    public boolean existTagging(String tagId, String docId, String userName) {
        if (log.isDebugEnabled()) {
            log.debug("existTagging() with " + tagId + ", " + docId + ", "
                    + userName);
        }
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("docId", docId);
        params.put("userName", userName);
        params.put("tagId", tagId);
        Long result = (Long) doNamedQuerySingle(GET_TAGGING, params);
        return result > 0;
    }
}

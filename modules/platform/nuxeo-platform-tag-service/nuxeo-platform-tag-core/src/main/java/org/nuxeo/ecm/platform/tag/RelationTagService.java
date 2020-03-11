/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *  *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.tag;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * Implementation of tag service based on SQL relations
 *
 * @deprecated since 9.3, use {@link FacetedTagService} instead
 */
@Deprecated
public class RelationTagService extends AbstractTagService {

    @Override
    public boolean hasFeature(Feature feature) {
        switch (feature) {
        case TAGS_BELONG_TO_DOCUMENT:
            return false;
        default:
            throw new UnsupportedOperationException(feature.name());
        }
    }

    @Override
    public boolean supportsTag(CoreSession session, String docId) {
        return true;
    }

    @Override
    public void doTag(CoreSession session, String docId, String label, String username) {
        if (session.getDocument(new IdRef(docId)).isProxy()) { // Tags are disabled on proxies
            throw new NuxeoException("Adding tags is not allowed on proxies");
        }
        // Find tag
        List<Map<String, Serializable>> res = getItems(PAGE_PROVIDERS.GET_DOCUMENT_IDS_FOR_TAG.name(), session, label);
        String tagId = (res != null && !res.isEmpty()) ? (String) res.get(0).get(NXQL.ECM_UUID) : null;
        Calendar date = Calendar.getInstance();
        if (tagId == null) {
            // no tag found, create it
            DocumentModel tag = session.createDocumentModel(null, label, TagConstants.TAG_DOCUMENT_TYPE);
            tag.setPropertyValue("dc:created", date);
            tag.setPropertyValue(TagConstants.TAG_LABEL_FIELD, label);
            tag = session.createDocument(tag);
            tagId = tag.getId();
        }
        // Check if tagging already exists for user.
        res = getItems(PAGE_PROVIDERS.GET_FIRST_TAGGING_FOR_DOC_AND_TAG.name(), session, docId, tagId);

        if (res != null && !res.isEmpty()) {
            // tagging already exists
            return;
        }
        // Add tagging to the document.
        DocumentModel tagging = session.createDocumentModel(null, label, TagConstants.TAGGING_DOCUMENT_TYPE);
        tagging.setPropertyValue("dc:created", date);
        tagging.setPropertyValue("dc:creator", username);

        tagging.setPropertyValue(TagConstants.TAGGING_SOURCE_FIELD, docId);
        tagging.setPropertyValue(TagConstants.TAGGING_TARGET_FIELD, tagId);
        session.createDocument(tagging);
        session.save();
    }

    @Override
    public void doUntag(CoreSession session, String docId, String label) {
        IdRef ref = new IdRef(docId);
        if (session.exists(ref) && session.getDocument(ref).isProxy()) { // Tags are disabled on proxies
            throw new NuxeoException("Removing tags is not allowed on proxies");
        }
        String tagId = null;
        if (label != null) {
            // Find tag
            List<Map<String, Serializable>> res = getItems(PAGE_PROVIDERS.GET_DOCUMENT_IDS_FOR_TAG.name(), session,
                    label);
            tagId = (res != null && !res.isEmpty()) ? (String) res.get(0).get(NXQL.ECM_UUID) : null;
            if (tagId == null) {
                // tag not found
                return;
            }
        }
        // Find taggings for user.
        Set<String> taggingIds = new HashSet<>();
        String query = String.format("SELECT ecm:uuid FROM Tagging WHERE relation:source = '%s'", docId);
        if (tagId != null) {
            query += String.format(" AND relation:target = '%s'", tagId);
        }
        try (IterableQueryResult res = session.queryAndFetch(query, NXQL.NXQL)) {
            for (Map<String, Serializable> map : res) {
                taggingIds.add((String) map.get(NXQL.ECM_UUID));
            }
        }
        // Remove taggings
        for (String taggingId : taggingIds) {
            session.removeDocument(new IdRef(taggingId));
        }
        if (!taggingIds.isEmpty()) {
            session.save();
        }
    }

    @Override
    public Set<String> doGetTags(CoreSession session, String docId) {
        List<Map<String, Serializable>> res = getItems(PAGE_PROVIDERS.GET_TAGS_FOR_DOCUMENT.name(), session, docId);
        if (res == null) {
            return Collections.emptySet();
        }
        return res.stream().map(m -> (String) m.get(TagConstants.TAG_LABEL_FIELD)).collect(Collectors.toSet());
    }

    @Override
    public void doCopyTags(CoreSession session, String srcDocId, String dstDocId, boolean removeExistingTags) {
        if (removeExistingTags) {
            doUntag(session, dstDocId, null);
        }
        Set<String> existingTags = new HashSet<>();
        List<Map<String, Serializable>> dstTagsRes = getItems(PAGE_PROVIDERS.GET_TAGS_TO_COPY_FOR_DOCUMENT.name(),
                session, dstDocId);
        if (dstTagsRes != null) {
            for (Map<String, Serializable> map : dstTagsRes) {
                existingTags.add(String.format("%s/%s", map.get("tag:label"), map.get("dc:creator")));
            }
        }

        List<Map<String, Serializable>> srcTagsRes = getItems(PAGE_PROVIDERS.GET_TAGS_TO_COPY_FOR_DOCUMENT.name(),
                session, srcDocId);
        if (srcTagsRes != null) {
            boolean docCreated = false;
            for (Map<String, Serializable> map : srcTagsRes) {
                String key = String.format("%s/%s", map.get("tag:label"), map.get("dc:creator"));
                if (!existingTags.contains(key)) {
                    DocumentModel tagging = session.createDocumentModel(null, (String) map.get("tag:label"),
                            TagConstants.TAGGING_DOCUMENT_TYPE);
                    tagging.setPropertyValue("dc:created", map.get("dc:created"));
                    tagging.setPropertyValue("dc:creator", map.get("dc:creator"));
                    tagging.setPropertyValue(TagConstants.TAGGING_SOURCE_FIELD, dstDocId);
                    tagging.setPropertyValue(TagConstants.TAGGING_TARGET_FIELD, map.get("relation:target"));
                    session.createDocument(tagging);
                    docCreated = true;
                }
            }
            if (docCreated) {
                session.save();
            }
        }
    }

    @Override
    public List<String> doGetTagDocumentIds(CoreSession session, String label) {
        List<Map<String, Serializable>> res = getItems(PAGE_PROVIDERS.GET_DOCUMENTS_FOR_TAG.name(), session, label);
        if (res == null) {
            return Collections.emptyList();
        }
        return res.stream().map(m -> (String) m.get(TagConstants.TAGGING_SOURCE_FIELD)).collect(Collectors.toList());
    }

    @Override
    public Set<String> doGetTagSuggestions(CoreSession session, String label) {
        List<Map<String, Serializable>> res = getItems(PAGE_PROVIDERS.GET_TAG_SUGGESTIONS.name(), session, label);
        if (res == null) {
            return Collections.emptySet();
        }
        return res.stream().map(m -> (String) m.get(TagConstants.TAG_LABEL_FIELD)).collect(Collectors.toSet());
    }

    /**
     * @since 8.4
     */
    @Override
    public boolean canUntag(CoreSession session, String docId, String label) {
        boolean canUntag = super.canUntag(session, docId, label);
        if (!canUntag) {
            // Else check if desired tag was created by current user
            UnrestrictedCanRemoveTagging r = new UnrestrictedCanRemoveTagging(session, docId, label);
            r.runUnrestricted();
            canUntag = r.canUntag;
        }
        return canUntag;
    }

    protected class UnrestrictedCanRemoveTagging extends UnrestrictedSessionRunner {

        private final String docId;

        private final String label;

        private boolean canUntag;

        protected UnrestrictedCanRemoveTagging(CoreSession session, String docId, String label) {
            super(session);
            this.docId = docId;
            this.label = cleanLabel(label, true, false);
            this.canUntag = false;
        }

        @Override
        public void run() {
            String tagId = null;
            if (label != null) {
                // Find tag
                List<Map<String, Serializable>> res = getItems(PAGE_PROVIDERS.GET_DOCUMENT_IDS_FOR_TAG.name(), session,
                        label);
                tagId = (res != null && !res.isEmpty()) ? (String) res.get(0).get(NXQL.ECM_UUID) : null;
                if (tagId == null) {
                    // tag not found - so user can untag
                    canUntag = true;
                    return;
                }
            }
            // Find creators of tag(s).
            Set<String> creators = new HashSet<>();
            String query = String.format("SELECT DISTINCT dc:creator FROM Tagging WHERE relation:source = '%s'", docId);
            if (tagId != null) {
                query += String.format(" AND relation:target = '%s'", tagId);
            }
            try (IterableQueryResult res = session.queryAndFetch(query, NXQL.NXQL)) {
                for (Map<String, Serializable> map : res) {
                    creators.add((String) map.get("dc:creator"));
                }
            }
            // Check if user can untag
            // - in case of one tag, check if creators contains user
            // - in case of all tags, check if user is the only creator
            canUntag = creators.size() == 1 && creators.contains(originatingUsername);
        }
    }

    @Override
    public List<Tag> getTagCloud(CoreSession session, String docId, String username, Boolean normalize) {
        UnrestrictedGetDocumentCloud r = new UnrestrictedGetDocumentCloud(session, docId, username, normalize);
        r.runUnrestricted();
        return r.cloud;
    }

    protected static class UnrestrictedGetDocumentCloud extends UnrestrictedSessionRunner {

        protected final String docId;

        protected final String username;

        protected final List<Tag> cloud;

        protected final Boolean normalize;

        protected UnrestrictedGetDocumentCloud(CoreSession session, String docId, String username, Boolean normalize) {
            super(session);
            this.docId = docId;
            this.username = cleanUsername(username);
            this.normalize = normalize;
            this.cloud = new ArrayList<>();
        }

        @Override
        public void run() {
            List<Map<String, Serializable>> res;
            if (docId == null) {
                if (username == null) {
                    res = getItems(PAGE_PROVIDERS.GET_ALL_TAGS.name(), session);
                } else {
                    res = getItems(PAGE_PROVIDERS.GET_ALL_TAGS_FOR_USER.name(), session, username);
                }
            } else {
                // find all docs under docid
                String path = session.getDocument(new IdRef(docId)).getPathAsString();
                path = path.replace("'", "");
                List<String> docIds = new ArrayList<>();
                docIds.add(docId);
                List<Map<String, Serializable>> docRes = getItems(PAGE_PROVIDERS.GET_TAGGED_DOCUMENTS_UNDER.name(),
                        session, path);
                if (docRes != null) {
                    for (Map<String, Serializable> map : docRes) {
                        docIds.add((String) map.get(NXQL.ECM_UUID));
                    }
                }

                if (username == null) {
                    res = getItems(PAGE_PROVIDERS.GET_TAGS_FOR_DOCUMENTS.name(), session, docIds);
                } else {
                    res = getItems(PAGE_PROVIDERS.GET_TAGS_FOR_DOCUMENTS_AND_USER.name(), session, docIds, username);
                }
            }

            int min = 999999, max = 0;
            if (res != null) {
                for (Map<String, Serializable> map : res) {
                    String label = (String) map.get(TagConstants.TAG_LABEL_FIELD);
                    int weight = ((Long) map.get(TagConstants.TAGGING_SOURCE_FIELD)).intValue();
                    if (weight == 0) {
                        // shouldn't happen
                        continue;
                    }
                    if (weight > max) {
                        max = weight;
                    }
                    if (weight < min) {
                        min = weight;
                    }
                    Tag weightedTag = new Tag(label, weight);
                    cloud.add(weightedTag);
                }
            }
            if (normalize != null) {
                normalizeCloud(cloud, min, max, !normalize.booleanValue());
            }
        }

    }

    public static void normalizeCloud(List<Tag> cloud, int min, int max, boolean linear) {
        if (min == max) {
            for (Tag tag : cloud) {
                tag.setWeight(100);
            }
            return;
        }
        double nmin;
        double diff;
        if (linear) {
            nmin = min;
            diff = max - min;
        } else {
            nmin = Math.log(min);
            diff = Math.log(max) - nmin;
        }
        for (Tag tag : cloud) {
            long weight = tag.getWeight();
            double norm;
            if (linear) {
                norm = (weight - nmin) / diff;
            } else {
                norm = (Math.log(weight) - nmin) / diff;
            }
            tag.setWeight(Math.round(100 * norm));
        }
    }

}

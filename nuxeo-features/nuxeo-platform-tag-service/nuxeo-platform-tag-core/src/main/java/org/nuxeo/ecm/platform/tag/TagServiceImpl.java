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
 *     Catalin Baican
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The implementation of the tag service.
 */
public class TagServiceImpl extends DefaultComponent implements TagService {

    public static final String NXTAG = TagQueryMaker.NXTAG;

    protected enum PAGE_PROVIDERS {
        //
        GET_DOCUMENT_IDS_FOR_TAG,
        //
        GET_FIRST_TAGGING_FOR_DOC_AND_TAG_AND_USER,
        //
        GET_FIRST_TAGGING_FOR_DOC_AND_TAG,
        //
        GET_TAGS_FOR_DOCUMENT,
        // core version: should keep on querying VCS
        GET_TAGS_FOR_DOCUMENT_CORE,
        //
        GET_DOCUMENTS_FOR_TAG,
        //
        GET_TAGS_FOR_DOCUMENT_AND_USER,
        // core version: should keep on querying VCS
        GET_TAGS_FOR_DOCUMENT_AND_USER_CORE,
        //
        GET_DOCUMENTS_FOR_TAG_AND_USER,
        //
        GET_TAGS_TO_COPY_FOR_DOCUMENT,
        //
        GET_TAG_SUGGESTIONS,
        //
        GET_TAG_SUGGESTIONS_FOR_USER,
        //
        GET_TAGGED_DOCUMENTS_UNDER,
        //
        GET_ALL_TAGS,
        //
        GET_ALL_TAGS_FOR_USER,
        //
        GET_TAGS_FOR_DOCUMENTS,
        //
        GET_TAGS_FOR_DOCUMENTS_AND_USER,
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    protected static String cleanLabel(String label, boolean allowEmpty, boolean allowPercent) {
        if (label == null) {
            if (allowEmpty) {
                return null;
            }
            throw new NuxeoException("Invalid empty tag");
        }
        label = label.toLowerCase(); // lowercase
        label = label.replace(" ", ""); // no spaces
        label = label.replace("/", ""); // no slash
        label = label.replace("\\", ""); // dubious char
        label = label.replace("'", ""); // dubious char
        if (!allowPercent) {
            label = label.replace("%", ""); // dubious char
        }
        if (label.length() == 0) {
            throw new NuxeoException("Invalid empty tag");
        }
        return label;
    }

    protected static String cleanUsername(String username) {
        return username == null ? null : username.replace("'", "");
    }

    public void tag(CoreSession session, String docId, String label, String username) {
        UnrestrictedAddTagging r = new UnrestrictedAddTagging(session, docId, label, username);
        r.runUnrestricted();
        fireUpdateEvent(session, docId);
    }

    protected void fireUpdateEvent(CoreSession session, String docId) {
        DocumentRef documentRef = new IdRef(docId);
        if (session.exists(documentRef)) {
            DocumentModel documentModel = session.getDocument(documentRef);
            DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), documentModel);
            Event event = ctx.newEvent(DocumentEventTypes.DOCUMENT_TAG_UPDATED);
            Framework.getLocalService(EventService.class).fireEvent(event);
        }
    }

    protected static class UnrestrictedAddTagging extends UnrestrictedSessionRunner {
        private final String docId;

        private final String label;

        private final String username;

        protected UnrestrictedAddTagging(CoreSession session, String docId, String label, String username)
                {
            super(session);
            this.docId = docId;
            this.label = cleanLabel(label, false, false);
            this.username = cleanUsername(username);
        }

        @Override
        public void run() {
            // Find tag
            List<Map<String, Serializable>> res = getItems(PAGE_PROVIDERS.GET_DOCUMENT_IDS_FOR_TAG.name(), session,
                    label);
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
            if (username != null) {
                res = getItems(PAGE_PROVIDERS.GET_FIRST_TAGGING_FOR_DOC_AND_TAG_AND_USER.name(), session, docId, tagId,
                        username);
            } else {
                res = getItems(PAGE_PROVIDERS.GET_FIRST_TAGGING_FOR_DOC_AND_TAG.name(), session, docId, tagId);
            }
            if (res != null && !res.isEmpty()) {
                // tagging already exists
                return;
            }
            // Add tagging to the document.
            DocumentModel tagging = session.createDocumentModel(null, label, TagConstants.TAGGING_DOCUMENT_TYPE);
            tagging.setPropertyValue("dc:created", date);
            if (username != null) {
                tagging.setPropertyValue("dc:creator", username);
            }
            tagging.setPropertyValue(TagConstants.TAGGING_SOURCE_FIELD, docId);
            tagging.setPropertyValue(TagConstants.TAGGING_TARGET_FIELD, tagId);
            session.createDocument(tagging);
            session.save();
        }
    }

    public void untag(CoreSession session, String docId, String label, String username) {
        UnrestrictedRemoveTagging r = new UnrestrictedRemoveTagging(session, docId, label, username);
        r.runUnrestricted();
        if (label != null) {
            fireUpdateEvent(session, docId);
        }
    }

    protected static class UnrestrictedRemoveTagging extends UnrestrictedSessionRunner {

        private final String docId;

        private final String label;

        private final String username;

        protected UnrestrictedRemoveTagging(CoreSession session, String docId, String label, String username)
                {
            super(session);
            this.docId = docId;
            this.label = cleanLabel(label, true, false);
            this.username = cleanUsername(username);
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
                    // tag not found
                    return;
                }
            }
            // Find taggings for user.
            Set<String> taggingIds = new HashSet<String>();
            String query = String.format("SELECT ecm:uuid FROM Tagging " + "WHERE relation:source = '%s'", docId);
            if (tagId != null) {
                query += String.format(" AND relation:target = '%s'", tagId);
            }
            if (username != null) {
                query += String.format(" AND dc:creator = '%s'", username);
            }
            IterableQueryResult res = session.queryAndFetch(query, NXQL.NXQL);
            try {
                for (Map<String, Serializable> map : res) {
                    taggingIds.add((String) map.get(NXQL.ECM_UUID));
                }
            } finally {
                res.close();
            }
            // Remove taggings
            for (String taggingId : taggingIds) {
                session.removeDocument(new IdRef(taggingId));
            }
            if (!taggingIds.isEmpty()) {
                session.save();
            }
        }
    }

    public List<Tag> getDocumentTags(CoreSession session, String docId, String username) {
        return getDocumentTags(session, docId, username, true);
    }

    public List<Tag> getDocumentTags(CoreSession session, String docId, String username, boolean useCore)
            {
        UnrestrictedGetDocumentTags r = new UnrestrictedGetDocumentTags(session, docId, username, useCore);
        r.runUnrestricted();
        return r.tags;
    }

    protected static class UnrestrictedGetDocumentTags extends UnrestrictedSessionRunner {

        protected final String docId;

        protected final String username;

        protected final List<Tag> tags;

        protected final boolean useCore;

        protected UnrestrictedGetDocumentTags(CoreSession session, String docId, String username, boolean useCore)
                {
            super(session);
            this.docId = docId;
            this.username = cleanUsername(username);
            this.useCore = useCore;
            tags = new ArrayList<Tag>();
        }

        @Override
        public void run() {
            List<Map<String, Serializable>> res;
            if (username == null) {
                String ppName = PAGE_PROVIDERS.GET_TAGS_FOR_DOCUMENT.name();
                if (useCore) {
                    ppName = PAGE_PROVIDERS.GET_TAGS_FOR_DOCUMENT_CORE.name();
                }
                res = getItems(ppName, session, docId);
            } else {
                String ppName = PAGE_PROVIDERS.GET_TAGS_FOR_DOCUMENT_AND_USER.name();
                if (useCore) {
                    ppName = PAGE_PROVIDERS.GET_TAGS_FOR_DOCUMENT_AND_USER_CORE.name();
                }
                res = getItems(ppName, session, docId, username);
            }
            if (res != null) {
                for (Map<String, Serializable> map : res) {
                    String label = (String) map.get(TagConstants.TAG_LABEL_FIELD);
                    tags.add(new Tag(label, 0));
                }
            }
        }
    }

    @Override
    public void removeTags(CoreSession session, String docId) {
        untag(session, docId, null, null);
    }

    @Override
    public void copyTags(CoreSession session, String srcDocId, String dstDocId) {
        copyTags(session, srcDocId, dstDocId, false);
    }

    protected void copyTags(CoreSession session, String srcDocId, String dstDocId, boolean removeExistingTags)
            {
        if (removeExistingTags) {
            removeTags(session, dstDocId);
        }

        UnrestrictedCopyTags r = new UnrestrictedCopyTags(session, srcDocId, dstDocId);
        r.runUnrestricted();
    }

    protected static class UnrestrictedCopyTags extends UnrestrictedSessionRunner {

        protected final String srcDocId;

        protected final String dstDocId;

        protected UnrestrictedCopyTags(CoreSession session, String srcDocId, String dstDocId) {
            super(session);
            this.srcDocId = srcDocId;
            this.dstDocId = dstDocId;
        }

        @Override
        public void run() {
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
    }

    @Override
    public void replaceTags(CoreSession session, String srcDocId, String dstDocId) {
        copyTags(session, srcDocId, dstDocId, true);
    }

    public List<String> getTagDocumentIds(CoreSession session, String label, String username) {
        UnrestrictedGetTagDocumentIds r = new UnrestrictedGetTagDocumentIds(session, label, username);
        r.runUnrestricted();
        return r.docIds;
    }

    protected static class UnrestrictedGetTagDocumentIds extends UnrestrictedSessionRunner {

        protected final String label;

        protected final String username;

        protected final List<String> docIds;

        protected UnrestrictedGetTagDocumentIds(CoreSession session, String label, String username)
                {
            super(session);
            this.label = cleanLabel(label, false, false);
            this.username = cleanUsername(username);
            docIds = new ArrayList<String>();
        }

        @Override
        public void run() {
            List<Map<String, Serializable>> res;
            if (username == null) {
                res = getItems(PAGE_PROVIDERS.GET_DOCUMENTS_FOR_TAG.name(), session, label);
            } else {
                res = getItems(PAGE_PROVIDERS.GET_DOCUMENTS_FOR_TAG_AND_USER.name(), session, label, username);
            }
            if (res != null) {
                for (Map<String, Serializable> map : res) {
                    docIds.add((String) map.get(TagConstants.TAGGING_SOURCE_FIELD));
                }
            }
        }
    }

    public List<Tag> getTagCloud(CoreSession session, String docId, String username, Boolean normalize)
            {
        UnrestrictedGetDocumentCloud r = new UnrestrictedGetDocumentCloud(session, docId, username, normalize);
        r.runUnrestricted();
        return r.cloud;
    }

    protected static class UnrestrictedGetDocumentCloud extends UnrestrictedSessionRunner {

        protected final String docId;

        protected final String username;

        protected final List<Tag> cloud;

        protected final Boolean normalize;

        protected UnrestrictedGetDocumentCloud(CoreSession session, String docId, String username, Boolean normalize)
                {
            super(session);
            this.docId = docId;
            this.username = cleanUsername(username);
            this.normalize = normalize;
            cloud = new ArrayList<Tag>();
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
                List<String> docIds = new ArrayList<String>();
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

    public List<Tag> getSuggestions(CoreSession session, String label, String username) {
        UnrestrictedGetTagSuggestions r = new UnrestrictedGetTagSuggestions(session, label, username);
        r.runUnrestricted();
        return r.tags;
    }

    protected static class UnrestrictedGetTagSuggestions extends UnrestrictedSessionRunner {

        protected final String label;

        protected final String username;

        protected final List<Tag> tags;

        protected UnrestrictedGetTagSuggestions(CoreSession session, String label, String username)
                {
            super(session);
            label = cleanLabel(label, false, true);
            if (!label.contains("%")) {
                label += "%";
            }
            this.label = label;
            this.username = cleanUsername(username);
            tags = new ArrayList<Tag>();
        }

        @Override
        public void run() {
            List<Map<String, Serializable>> res;
            if (username == null) {
                res = getItems(PAGE_PROVIDERS.GET_TAG_SUGGESTIONS.name(), session, label);
            } else {
                res = getItems(PAGE_PROVIDERS.GET_TAG_SUGGESTIONS_FOR_USER.name(), session, label, username);
            }
            if (res != null) {
                for (Map<String, Serializable> map : res) {
                    String label = (String) map.get(TagConstants.TAG_LABEL_FIELD);
                    tags.add(new Tag(label, 0));
                }
            }
            // XXX should sort on tag weight
            Collections.sort(tags, Tag.LABEL_COMPARATOR);
        }
    }

    /**
     * Returns results from calls to {@link CoreSession#queryAndFetch(String, String, Object...)} using page providers.
     *
     * @since 6.0
     */
    @SuppressWarnings("unchecked")
    protected static List<Map<String, Serializable>> getItems(String pageProviderName, CoreSession session,
            Object... params) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        if (ppService == null) {
            throw new RuntimeException("Missing PageProvider service");
        }
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        // first retrieve potential props from definition
        PageProviderDefinition def = ppService.getPageProviderDefinition(pageProviderName);
        if (def != null) {
            Map<String, String> defProps = def.getProperties();
            if (defProps != null) {
                props.putAll(defProps);
            }
        }
        props.put(CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) ppService.getPageProvider(
                pageProviderName, null, null, null, props, params);
        if (pp == null) {
            throw new NuxeoException("Page provider not found: " + pageProviderName);
        }
        return pp.getCurrentPage();
    }
}

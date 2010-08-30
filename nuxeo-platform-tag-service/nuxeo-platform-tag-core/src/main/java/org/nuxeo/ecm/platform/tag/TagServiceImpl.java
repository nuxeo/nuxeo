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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The implementation of the tag service.
 */
public class TagServiceImpl extends DefaultComponent implements TagService {

    private static final Log log = LogFactory.getLog(TagServiceImpl.class);

    private Boolean enabled;

    public boolean isEnabled() {
        if (enabled == null) {
            enabled = Boolean.FALSE;
            LoginContext lc = null;
            try {
                lc = Framework.login();
                RepositoryManager rm = Framework.getService(RepositoryManager.class);
                if (rm.getDefaultRepository().supportsTags()) {
                    log.debug("Activating TagService");
                    enabled = Boolean.TRUE;
                } else {
                    log.warn("Default repository does not support Tag feature: "
                            + "Tag service won't be available.");
                }
            } catch (Exception e) {
                log.error("Unable to test repository for Tag feature.", e);
            } finally {
                if (lc != null) {
                    try {
                        lc.logout();
                    } catch (Exception e) {
                        log.error(e, e);
                    }
                }
            }
        }
        return enabled.booleanValue();
    }

    protected static String getUsername(CoreSession session)
            throws ClientException {
        if (session == null) {
            throw new ClientException("No session available");
        }
        return session.getPrincipal().getName().replace("'", "");
    }

    protected static String cleanLabel(String label, boolean allowPercent)
            throws ClientException {
        if (label == null) {
            throw new ClientException("Invalid empty tag");
        }
        label = label.toLowerCase(); // lowercase
        label = label.replace(" ", ""); // no spaces
        label = label.replace("\\", ""); // dubious char
        label = label.replace("'", ""); // dubious char
        if (!allowPercent) {
            label = label.replace("%", ""); // dubious char
        }
        if (label.length() == 0) {
            throw new ClientException("Invalid empty tag");
        }
        return label;
    }

    public void tag(CoreSession session, String docId, String label,
            String username) throws ClientException {
        UnrestrictedAddTagging r = new UnrestrictedAddTagging(session, docId,
                label, username);
        r.runUnrestricted();
    }

    protected static class UnrestrictedAddTagging extends
            UnrestrictedSessionRunner {

        private final String docId;

        private final String label;

        private final String username;

        protected UnrestrictedAddTagging(CoreSession session, String docId,
                String label, String username) throws ClientException {
            super(session);
            this.docId = docId;
            this.label = cleanLabel(label, false);
            this.username = username == null ? null : username.replace("'", "");
        }

        @Override
        public void run() throws ClientException {
            /*
             * Find tag.
             */
            String tagId = null;
            String query = String.format(
                    "SELECT ecm:uuid FROM Tag WHERE tag:label = '%s' AND ecm:isProxy = 0",
                    label);
            IterableQueryResult res = session.queryAndFetch(query, "NXQL");
            try {
                for (Map<String, Serializable> map : res) {
                    tagId = (String) map.get(NXQL.ECM_UUID);
                    break;
                }
            } finally {
                res.close();
            }
            Calendar date = Calendar.getInstance();
            if (tagId == null) {
                // no tag found, create it
                DocumentModel tag = session.createDocumentModel(null, label,
                        TagConstants.TAG_DOCUMENT_TYPE);
                tag.setPropertyValue("dc:created", date);
                tag.setPropertyValue(TagConstants.TAG_LABEL_FIELD, label);
                tag = session.createDocument(tag);
                // session.save();
                tagId = tag.getId();
            }
            /*
             * Check if tagging already exists for user.
             */
            query = String.format("SELECT ecm:uuid FROM Tagging "
                    + "WHERE relation:source = '%s'"
                    + "  AND relation:target = '%s'", docId, tagId);
            if (username != null) {
                query += String.format(" AND dc:creator = '%s'", username);
            }
            res = session.queryAndFetch(query, "NXQL");
            try {
                if (res.iterator().hasNext()) {
                    // tagging already exists
                    return;
                }
            } finally {
                res.close();
            }
            /*
             * Add tagging to the document.
             */
            DocumentModel tagging = session.createDocumentModel(null, label,
                    TagConstants.TAGGING_DOCUMENT_TYPE);
            tagging.setPropertyValue("dc:created", date);
            if (username != null) {
                tagging.setPropertyValue("dc:creator", username);
            }
            tagging.setPropertyValue(TagConstants.TAGGING_SOURCE_FIELD, docId);
            tagging.setPropertyValue(TagConstants.TAGGING_TARGET_FIELD, tagId);
            tagging = session.createDocument(tagging);
            session.save();
        }
    }

    public void untag(CoreSession session, String docId, String label,
            String username) throws ClientException {
        UnrestrictedRemoveTagging r = new UnrestrictedRemoveTagging(session,
                docId, label, username);
        r.runUnrestricted();
    }

    protected static class UnrestrictedRemoveTagging extends
            UnrestrictedSessionRunner {

        private final String docId;

        private final String label;

        private final String username;

        protected UnrestrictedRemoveTagging(CoreSession session, String docId,
                String label, String username) throws ClientException {
            super(session);
            this.docId = docId;
            this.label = label == null ? null : cleanLabel(label, false);
            this.username = username == null ? null : username.replace("'", "");
        }

        @Override
        public void run() throws ClientException {
            String tagId = null;
            if (label != null) {
                /*
                 * Find tag.
                 */
                String query = String.format(
                        "SELECT ecm:uuid FROM Tag WHERE tag:label = '%s' AND ecm:isProxy = 0",
                        label);
                IterableQueryResult res = session.queryAndFetch(query, "NXQL");
                try {
                    for (Map<String, Serializable> map : res) {
                        tagId = (String) map.get(NXQL.ECM_UUID);
                        break;
                    }
                } finally {
                    res.close();
                }
                if (tagId == null) {
                    // tag not found
                    return;
                }
            }
            /*
             * Find taggings for user.
             */
            Set<String> taggingIds = new HashSet<String>();
            String query = String.format("SELECT ecm:uuid FROM Tagging "
                    + "WHERE relation:source = '%s'", docId);
            if (tagId != null) {
                query += String.format(" AND relation:target = '%s'", tagId);
            }
            if (username != null) {
                query += String.format(" AND dc:creator = '%s'", username);
            }
            IterableQueryResult res = session.queryAndFetch(query, "NXQL");
            try {
                for (Map<String, Serializable> map : res) {
                    taggingIds.add((String) map.get(NXQL.ECM_UUID));
                }
            } finally {
                res.close();
            }
            /*
             * Remove taggings.
             */
            for (String taggingId : taggingIds) {
                session.removeDocument(new IdRef(taggingId));
            }
            if (!taggingIds.isEmpty()) {
                session.save();
            }
        }
    }

    public List<Tag> getDocumentTags(CoreSession session, String docId,
            String username) throws ClientException {
        UnrestrictedGetDocumentTags r = new UnrestrictedGetDocumentTags(
                session, docId, username);
        r.runUnrestricted();
        return r.tags;
    }

    protected static class UnrestrictedGetDocumentTags extends
            UnrestrictedSessionRunner {

        protected final String docId;

        protected final String username;

        protected final List<Tag> tags;

        protected UnrestrictedGetDocumentTags(CoreSession session, String docId,
                String username) throws ClientException {
            super(session);
            this.docId = docId;
            this.username = username == null ? null : username.replace("'", "");
            tags = new ArrayList<Tag>();
        }

        @Override
        public void run() throws ClientException {
            String query = String.format(
                    "TAGISTARGET: SELECT DISTINCT tag:label " //
                            + "FROM Tagging " //
                            + "WHERE relation:source = '%s'", //
                    docId);
            if (username != null) {
                query += String.format(" AND dc:creator = '%s'", username);
            }
            IterableQueryResult res = session.queryAndFetch(query, "NXTAG");
            try {
                for (Map<String, Serializable> map : res) {
                    String label = (String) map.get(TagConstants.TAG_LABEL_FIELD);
                    tags.add(new Tag(label, 0));
                }
            } finally {
                res.close();
            }
        }
    }

    public List<String> getTagDocumentIds(CoreSession session, String label,
            String username) throws ClientException {
        UnrestrictedGetTagDocumentIds r = new UnrestrictedGetTagDocumentIds(
                session, label, username);
        r.runUnrestricted();
        return r.docIds;
    }

    protected static class UnrestrictedGetTagDocumentIds extends
            UnrestrictedSessionRunner {

        protected final String label;

        protected final String username;

        protected final List<String> docIds;

        protected UnrestrictedGetTagDocumentIds(CoreSession session, String label,
                String username) throws ClientException {
            super(session);
            this.label = cleanLabel(label, false);
            this.username = username == null ? null : username.replace("'", "");
            docIds = new ArrayList<String>();
        }

        @Override
        public void run() throws ClientException {
            String query = String.format(
                    "TAGISTARGET: SELECT DISTINCT relation:source " //
                            + "FROM Tagging " //
                            + "WHERE tag:label = '%s'", //
                    label);
            if (username != null) {
                query += String.format(" AND dc:creator = '%s'", username);
            }
            IterableQueryResult res = session.queryAndFetch(query, "NXTAG");
            try {
                for (Map<String, Serializable> map : res) {
                    docIds.add((String) map.get(TagConstants.TAGGING_SOURCE_FIELD));
                }
            } finally {
                res.close();
            }
        }
    }

    public List<Tag> getTagCloud(CoreSession session, String docId,
            String username, Boolean normalize) throws ClientException {
        UnrestrictedGetDocumentCloud r = new UnrestrictedGetDocumentCloud(
                session, docId, username, normalize);
        r.runUnrestricted();
        return r.cloud;
    }

    protected static class UnrestrictedGetDocumentCloud extends
            UnrestrictedSessionRunner {

        protected final String docId;

        protected final String username;

        protected final List<Tag> cloud;

        protected final Boolean normalize;

        protected UnrestrictedGetDocumentCloud(CoreSession session, String docId,
                String username, Boolean normalize) throws ClientException {
            super(session);
            this.docId = docId;
            this.username = username == null ? null : username.replace("'", "");
            this.normalize = normalize;
            cloud = new ArrayList<Tag>();
        }

        @Override
        public void run() throws ClientException {
            String query = "COUNTSOURCE: " //
                    + "SELECT tag:label, relation:source " //
                    + "FROM Tagging";
            if (docId != null) {
                // find all docs under docid
                String path = session.getDocument(new IdRef(docId)).getPathAsString();
                path = path.replace("'", "");
                String q = String.format("SELECT ecm:uuid FROM Document "
                        + "WHERE ecm:path STARTSWITH '%s'", path);
                List<String> docIds = new ArrayList<String>();
                docIds.add(docId);
                IterableQueryResult r = session.queryAndFetch(q, "NXQL");
                try {
                    for (Map<String, Serializable> map : r) {
                        docIds.add((String) map.get(NXQL.ECM_UUID));
                    }
                } finally {
                    r.close();
                }
                // now used these docids for the relation source
                query += String.format(" WHERE relation:source IN ('%s')",
                        StringUtils.join(docIds, "', '"));
            }
            if (username != null) {
                if (docId == null) {
                    query += " WHERE ";
                } else {
                    query += " AND ";
                }
                query += String.format("dc:creator = '%s'", username);

            }
            int min = 999999, max = 0;
            IterableQueryResult res = session.queryAndFetch(query, "NXTAG");
            try {
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
            } finally {
                res.close();
            }
            if (normalize != null) {
                normalizeCloud(cloud, min, max, !normalize.booleanValue());
            }
        }
    }

    public static void normalizeCloud(List<Tag> cloud, int min, int max,
            boolean linear) {
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

    public List<Tag> getSuggestions(CoreSession session, String label,
            String username) throws ClientException {
        UnrestrictedGetTagSuggestions r = new UnrestrictedGetTagSuggestions(
                session, label, username);
        r.runUnrestricted();
        return r.tags;
    }

    protected static class UnrestrictedGetTagSuggestions extends
            UnrestrictedSessionRunner {

        protected final String label;

        protected final String username;

        protected final List<Tag> tags;

        protected UnrestrictedGetTagSuggestions(CoreSession session, String label,
                String username) throws ClientException {
            super(session);
            label = cleanLabel(label, true);
            if (!label.contains("%")) {
                label += "%";
            }
            this.label = label;
            this.username = username == null ? null : username.replace("'", "");
            tags = new ArrayList<Tag>();
        }

        @Override
        public void run() throws ClientException {
            String query = String.format(
                    "SELECT DISTINCT tag:label FROM Tag WHERE tag:label LIKE '%s' AND ecm:isProxy = 0",
                    label);
            if (username != null) {
                query += String.format(" AND dc:creator = '%s'", username);
            }
            IterableQueryResult res = session.queryAndFetch(query, "NXQL");
            try {
                for (Map<String, Serializable> map : res) {
                    String label = (String) map.get(TagConstants.TAG_LABEL_FIELD);
                    tags.add(new Tag(label, 0));
                }
            } finally {
                res.close();
            }
            // XXX should sort on tag weight
            Collections.sort(tags, Tag.LABEL_COMPARATOR);
        }
    }

}

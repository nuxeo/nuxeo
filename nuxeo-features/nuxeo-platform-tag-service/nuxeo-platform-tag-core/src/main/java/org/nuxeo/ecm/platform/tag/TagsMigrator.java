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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.tag;

import static java.lang.Boolean.TRUE;
import static java.util.function.Predicate.isEqual;
import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_NAME;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.platform.tag.FacetedTagService.LABEL_PROPERTY;
import static org.nuxeo.ecm.platform.tag.FacetedTagService.USERNAME_PROPERTY;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_STATE_FACETS;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_STATE_RELATIONS;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_STEP_RELATIONS_TO_FACETS;
import static org.nuxeo.ecm.platform.tag.TagConstants.TAGGING_SOURCE_FIELD;
import static org.nuxeo.ecm.platform.tag.TagConstants.TAG_LIST;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.migration.MigrationService.Migrator;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Migrator of tags.
 *
 * @since 9.3
 */
public class TagsMigrator implements Migrator {

    private static final Log log = LogFactory.getLog(TagsMigrator.class);

    protected static final String QUERY_TAGGING = "SELECT ecm:uuid, relation:source, ecm:name, dc:creator FROM Tagging WHERE ecm:isProxy = 0";

    /**
     * A label + username.
     *
     * @since 9.3
     */
    protected static class Tag {

        protected final String label;

        protected final String username;

        public Tag(String label, String username) {
            this.label = label;
            this.username = username;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((label == null) ? 0 : label.hashCode());
            result = prime * result + ((username == null) ? 0 : username.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Tag)) {
                return false;
            }
            Tag other = (Tag) obj;
            if (label == null) {
                if (other.label != null) {
                    return false;
                }
            } else if (!label.equals(other.label)) {
                return false;
            }
            if (username == null) {
                if (other.username != null) {
                    return false;
                }
            } else if (!username.equals(other.username)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Tag(" + label + "," + username + ")";
        }
    }

    protected static final int BATCH_SIZE = 50;

    protected MigrationContext migrationContext;

    // exception used for simpler flow control
    protected static class MigrationShutdownException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public MigrationShutdownException() {
            super();
        }
    }

    @Override
    public void notifyStatusChange() {
        TagServiceImpl tagService = (TagServiceImpl) Framework.getRuntime().getComponent(TagServiceImpl.NAME);
        tagService.invalidateTagServiceImplementation();
    }

    @Override
    public String probeState() {
        List<String> repositoryNames = Framework.getService(RepositoryService.class).getRepositoryNames();
        if (repositoryNames.stream().map(this::probeRepository).anyMatch(isEqual(MIGRATION_STATE_RELATIONS))) {
            return MIGRATION_STATE_RELATIONS;
        }
        return MIGRATION_STATE_FACETS;
    }

    protected String probeRepository(String repositoryName) {
        return TransactionHelper.runInTransaction(() -> CoreInstance.doPrivileged(repositoryName, this::probeSession));
    }

    protected String probeSession(CoreSession session) {
        // finds if there are any taggings
        List<Map<String, Serializable>> taggingMaps = session.queryProjection(QUERY_TAGGING, 1, 0); // limit 1
        if (!taggingMaps.isEmpty()) {
            return MIGRATION_STATE_RELATIONS;
        } else {
            return MIGRATION_STATE_FACETS;
        }
    }

    @Override
    public void run(String step, MigrationContext migrationContext) {
        if (!MIGRATION_STEP_RELATIONS_TO_FACETS.equals(step)) {
            throw new NuxeoException("Unknown migration step: " + step);
        }
        this.migrationContext = migrationContext;
        reportProgress("Initializing", 0, -1); // unknown
        List<String> repositoryNames = Framework.getService(RepositoryService.class).getRepositoryNames();
        try {
            repositoryNames.forEach(this::migrateRepository);
        } catch (MigrationShutdownException e) {
            return;
        }
    }

    protected void checkShutdownRequested() {
        if (migrationContext.isShutdownRequested()) {
            throw new MigrationShutdownException();
        }
    }

    protected void reportProgress(String message, long num, long total) {
        log.debug(message + ": " + num + "/" + total);
        migrationContext.reportProgress(message, num, total);
    }

    protected void migrateRepository(String repositoryName) {
        TransactionHelper.runInTransaction(() -> CoreInstance.doPrivileged(repositoryName, this::migrateSession));
    }

    protected void migrateSession(CoreSession session) {
        // query all tagging
        List<Map<String, Serializable>> taggingMaps = session.queryProjection(QUERY_TAGGING, -1, 0);

        checkShutdownRequested();

        // query all tags we'll have to remove too
        String tagSql = "SELECT ecm:uuid FROM Tag WHERE ecm:isProxy = 0";
        List<Map<String, Serializable>> tagMaps = session.queryProjection(tagSql, -1, 0);

        checkShutdownRequested();

        // compute all tagged documents and their tag label and username
        Map<String, Set<Tag>> docTags = new HashMap<>();
        for (Map<String, Serializable> map : taggingMaps) {
            String docId = (String) map.get(TAGGING_SOURCE_FIELD);
            String label = (String) map.get(ECM_NAME);
            String username = (String) map.get("dc:creator");
            Tag tag = new Tag(label, username);
            docTags.computeIfAbsent(docId, key -> new HashSet<>()).add(tag);
        }
        // compute all Tagging doc ids
        Set<String> taggingIds = taggingMaps.stream() //
                                            .map(map -> (String) map.get(ECM_UUID))
                                            .collect(Collectors.toSet());
        // compute all Tag doc ids
        Set<String> tagIds = tagMaps.stream() //
                                    .map(map -> (String) map.get(ECM_UUID))
                                    .collect(Collectors.toSet());

        checkShutdownRequested();

        // recreate all doc tags
        processBatched(docTags.entrySet(), es -> addTags(session, es.getKey(), es.getValue()), "Creating new tags");

        // delete all Tagging and Tag documents
        processBatched(taggingIds, docId -> removeDocument(session, docId), "Deleting old Tagging documents");
        processBatched(tagIds, docId -> removeDocument(session, docId), "Deleting old Tag documents");

        reportProgress("Done", docTags.size(), docTags.size());
    }

    protected void removeDocument(CoreSession session, String docId) {
        try {
            session.removeDocument(new IdRef(docId));
        } catch (DocumentNotFoundException e) {
            // ignore document that was already removed, or whose type is unknown
            return;
        }
    }

    protected void addTags(CoreSession session, String docId, Set<Tag> tags) {
        DocumentModel doc;
        try {
            doc = session.getDocument(new IdRef(docId));
        } catch (DocumentNotFoundException e) {
            // ignore document that was already removed, or whose type is unknown
            return;
        }
        addTags(doc, tags);
    }

    @SuppressWarnings("unchecked")
    protected void addTags(DocumentModel doc, Set<Tag> tags) {
        if (doc.isProxy()) {
            // adding tags is not allowed on proxies
            return;
        }
        List<Map<String, Serializable>> tagsList;
        try {
            tagsList = (List<Map<String, Serializable>>) doc.getPropertyValue(TAG_LIST);
        } catch (PropertyNotFoundException e) {
            // missing facet, cannot add tag
            return;
        }
        boolean changed = false;
        for (Tag tag : tags) {
            if (tagsList.stream().noneMatch(t -> tag.label.equals(t.get(LABEL_PROPERTY)))) {
                Map<String, Serializable> tagMap = new HashMap<>(2);
                tagMap.put(LABEL_PROPERTY, tag.label);
                tagMap.put(USERNAME_PROPERTY, tag.username);
                tagsList.add(tagMap);
                changed = true;
            }
        }
        if (changed) {
            doc.putContextData(ALLOW_VERSION_WRITE, TRUE);
            doc.setPropertyValue(TAG_LIST, (Serializable) tagsList);
            doc.getCoreSession().saveDocument(doc);
        }
    }

    /**
     * Runs a consumer on the collection, committing every BATCH_SIZE elements, reporting progress and checking for
     * shutdown request.
     */
    protected <T> void processBatched(Collection<T> collection, Consumer<T> consumer, String progressMessage) {
        int size = collection.size();
        int i = -1;
        for (T element : collection) {
            consumer.accept(element);
            checkShutdownRequested();
            i++;
            if (i % BATCH_SIZE == 0 || i == size - 1) {
                reportProgress(progressMessage, i + 1, size);
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
    }

}

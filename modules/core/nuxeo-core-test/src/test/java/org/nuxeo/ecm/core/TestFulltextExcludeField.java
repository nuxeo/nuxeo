/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.IgnoreIfNotDBSRepository;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests that an {@code <excludeField>} on a content-type field registers all subpaths as excluded (binary {@code /data}
 * and string metadata), and that saving a document whose only dirty paths are excluded does not schedule a
 * {@code FulltextExtractorWork} (no {@code fulltextJobId} set).
 *
 * @since 2025.22
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@ConditionalIgnoreRule.Ignore(condition = IgnoreIfNotDBSRepository.class, cause = "excludeField dirty-detection fix is DBS only")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-fulltext-excludefield-contrib.xml")
public class TestFulltextExcludeField {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    /**
     * Verifies that an {@code <excludeField>} on a content-type field (e.g. {@code file:content}) registers all its
     * subpaths — both binary ({@code /data}) and string metadata ({@code /name}, {@code /mime-type}, etc.) — as
     * excluded in {@link FulltextConfiguration}, in both prefixed and unprefixed form (the {@code file} schema has no
     * namespace prefix, so DirtyPathsFinder produces both {@code file:content/data} and {@code content/data}).
     */
    @Test
    public void testExcludeFieldRegistersAllSubpaths() {
        FulltextConfiguration config = getFulltextConfiguration();

        // binary subpath must be excluded in both prefixed and unprefixed form
        assertTrue("file:content/data must be in indexesByPropPathExcludedBinary",
                config.indexesByPropPathExcludedBinary.containsKey("file:content/data"));
        assertTrue("content/data must be in indexesByPropPathExcludedBinary (file schema has no prefix)",
                config.indexesByPropPathExcludedBinary.containsKey("content/data"));

        // string metadata subpaths must be excluded in both forms so they don't trigger scheduling
        assertTrue("file:content/name must be in indexesByPropPathExcludedSimple",
                config.indexesByPropPathExcludedSimple.containsKey("file:content/name"));
        assertTrue("content/name must be in indexesByPropPathExcludedSimple (file schema has no prefix)",
                config.indexesByPropPathExcludedSimple.containsKey("content/name"));
        assertTrue("file:content/mime-type must be in indexesByPropPathExcludedSimple",
                config.indexesByPropPathExcludedSimple.containsKey("file:content/mime-type"));
        assertTrue("file:content/encoding must be in indexesByPropPathExcludedSimple",
                config.indexesByPropPathExcludedSimple.containsKey("file:content/encoding"));
        assertTrue("file:content/digest must be in indexesByPropPathExcludedSimple",
                config.indexesByPropPathExcludedSimple.containsKey("file:content/digest"));
    }

    /**
     * Verifies that saving a document whose only dirty paths belong to an excluded field does not cause
     * {@code findDirtyDocuments()} to schedule a {@code FulltextExtractorWork}. {@code fulltextJobId} is set
     * synchronously by {@code markIndexingInProgress()} inside {@code save()} when a work is about to be queued; its
     * absence after save confirms no work was scheduled.
     */
    @Test
    public void testNoWorkScheduledWhenOnlyExcludedFieldsDirty() {
        // create doc without blob and commit to clear initial dirty state
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // update only the excluded field
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("hello"));
        session.saveDocument(doc);
        // read fulltextJobId within the same transaction, before async runs — save() sets it synchronously
        session.save();

        String jobId = session.getDocumentSystemProp(doc.getRef(), DBSDocument.SYSPROP_FULLTEXT_JOBID, String.class);
        assertNull("fulltextJobId must not be set when only excluded paths are dirty", jobId);
    }

    /**
     * Verifies that saving a document with a non-excluded dirty field (dc:title) does schedule a
     * {@code FulltextExtractorWork}, confirming the exclusion does not disable fulltext indexing entirely.
     */
    @Test
    public void testWorkScheduledWhenNonExcludedFieldDirty() {
        // create doc and commit to clear initial dirty state
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // update only a non-excluded string field
        doc.setPropertyValue("dc:title", "hello");
        session.saveDocument(doc);
        // read fulltextJobId within the same transaction, before async runs
        session.save();

        String jobId = session.getDocumentSystemProp(doc.getRef(), DBSDocument.SYSPROP_FULLTEXT_JOBID, String.class);
        assertNotNull("fulltextJobId must be set when a non-excluded string field is dirty", jobId);
    }

    protected FulltextConfiguration getFulltextConfiguration() {
        Repository repository = Framework.getService(RepositoryService.class)
                                         .getRepository(session.getRepositoryName());
        return repository.getFulltextConfiguration();
    }
}

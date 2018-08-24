/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.webapp.bulkedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.tag.FacetedTagService;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.webapp.base")
@Deploy("org.nuxeo.ecm.platform.tag.api")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.collections.core:OSGI-INF/collection-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.query.api")
public class TestBulkEditService {

    @Inject
    protected CoreSession session;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected EventService eventService;

    @Inject
    protected TagService tagService;

    @Inject
    protected BulkEditService bulkEditService;

    protected List<DocumentModel> createTestDocuments() {
        List<DocumentModel> docs = new ArrayList<>();
        DocumentModel file = session.createDocumentModel("/", "doc1", "File");
        file.setPropertyValue("dc:title", "doc1");
        file = session.createDocument(file);
        docs.add(file);

        file = session.createDocumentModel("/", "doc2", "File");
        file.setPropertyValue("dc:title", "doc2");
        file = session.createDocument(file);
        docs.add(file);

        file = session.createDocumentModel("/", "doc3", "File");
        file.setPropertyValue("dc:title", "doc3");
        file = session.createDocument(file);
        docs.add(file);

        // wait for fulltext processing before copy, to avoid transaction
        // isolation issues (SQL Server)
        eventService.waitForAsyncCompletion();

        return docs;
    }

    protected DocumentModel buildSimpleDocumentModel() {
        DocumentModel sourceDoc = new SimpleDocumentModel();
        sourceDoc.setProperty("dublincore", "title", "new title");
        sourceDoc.setProperty("dublincore", "description", "new description");
        sourceDoc.setPropertyValue("dublincore:creator", "new creator");
        sourceDoc.setPropertyValue("dc:source", "new source");
        sourceDoc.putContextData(BulkEditService.BULK_EDIT_PREFIX + "dc:title", true);
        sourceDoc.putContextData(BulkEditService.BULK_EDIT_PREFIX + "dc:description", false);
        sourceDoc.putContextData(BulkEditService.BULK_EDIT_PREFIX + "dc:creator", true);
        sourceDoc.putContextData(BulkEditService.BULK_EDIT_PREFIX + "dc:source", false);
        return sourceDoc;
    }

    @Test
    @Deploy("org.nuxeo.ecm.webapp.base:test-bulkedit-minorversion-contrib.xml")
    public void testBulkEdit() {
        List<DocumentModel> docs = createTestDocuments();
        DocumentModel sourceDoc = buildSimpleDocumentModel();

        bulkEditService.updateDocuments(session, sourceDoc, docs);
        for (DocumentModel doc : docs) {
            doc = session.getDocument(doc.getRef());
            assertEquals("new title", doc.getPropertyValue("dc:title"));
            assertEquals("new creator", doc.getProperty("dc:creator").getValue());
            assertFalse("new description".equals(doc.getPropertyValue("dc:description")));
            assertFalse("new source".equals(doc.getPropertyValue("dc:source")));

            assertFalse(doc.isCheckedOut());
            assertEquals("0.1", doc.getVersionLabel());

            DocumentModel version = session.getLastDocumentVersion(doc.getRef());
            assertEquals("new title", version.getPropertyValue("dc:title"));
            assertEquals("new creator", version.getProperty("dc:creator").getValue());
            assertFalse("new description".equals(version.getPropertyValue("dc:description")));
            assertFalse("new source".equals(version.getPropertyValue("dc:source")));
            assertEquals("0.1", version.getVersionLabel());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.webapp.base:test-bulkedit-minorversion-before-update-contrib.xml")
    public void testBulkEditBeforeUpdate() {

        boolean facetedTags = tagService instanceof FacetedTagService;
        assumeTrue("DBS does not support tags based on SQL relations",
                !coreFeature.getStorageConfiguration().isDBS() || facetedTags);

        // behaviour should be compliant with NXP-12225, NXP-22099 provides fix with auto-versioning
        List<DocumentModel> docs = createTestDocuments();
        DocumentModel sourceDoc = buildSimpleDocumentModel();

        bulkEditService.updateDocuments(session, sourceDoc, docs);

        for (DocumentModel doc : docs) {
            tagService.tag(session, doc.getId(), "tag");
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModel doc = session.getDocument(docs.get(0).getRef());
        assertEquals("new title", doc.getPropertyValue("dc:title"));
        assertEquals("new creator", doc.getProperty("dc:creator").getValue());
        assertFalse("new description".equals(doc.getPropertyValue("dc:description")));
        assertFalse("new source".equals(doc.getPropertyValue("dc:source")));

        List<String> tags = new ArrayList<>(tagService.getTags(session, doc.getId()));
        assertEquals(1, tags.size());
        assertEquals("tag", tags.get(0));

        assertEquals("0.1+", doc.getVersionLabel());

        DocumentModel version = session.getLastDocumentVersion(doc.getRef());
        assertEquals("doc1", version.getPropertyValue("dc:title"));
        assertEquals("0.1", version.getVersionLabel());

        tags = new ArrayList<>(tagService.getTags(session, version.getId()));
        assertEquals(0, tags.size());
    }

    @Test
    public void testBulkEditNoVersion() throws Exception {

        List<DocumentModel> docs = createTestDocuments();
        DocumentModel sourceDoc = buildSimpleDocumentModel();

        bulkEditService.updateDocuments(session, sourceDoc, docs);
        for (DocumentModel doc : docs) {
            doc = session.getDocument(doc.getRef());
            assertEquals("new title", doc.getPropertyValue("dc:title"));
            assertEquals("new creator", doc.getProperty("dc:creator").getValue());
            assertFalse("new description".equals(doc.getPropertyValue("dc:description")));
            assertFalse("new source".equals(doc.getPropertyValue("dc:source")));

            assertTrue(doc.isCheckedOut());
            assertEquals("0.0", doc.getVersionLabel());

            assertNull(session.getLastDocumentVersion(doc.getRef()));
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.webapp.base:test-bulkedit-majorversion-contrib.xml")
    public void testBulkEditMajorVersion() throws Exception {

        List<DocumentModel> docs = createTestDocuments();
        DocumentModel sourceDoc = buildSimpleDocumentModel();

        bulkEditService.updateDocuments(session, sourceDoc, docs);
        for (DocumentModel doc : docs) {
            doc = session.getDocument(doc.getRef());
            assertEquals("new title", doc.getPropertyValue("dc:title"));
            assertEquals("new creator", doc.getProperty("dc:creator").getValue());
            assertFalse("new description".equals(doc.getPropertyValue("dc:description")));
            assertFalse("new source".equals(doc.getPropertyValue("dc:source")));

            assertFalse(doc.isCheckedOut());
            assertEquals("1.0", doc.getVersionLabel());

            DocumentModel version = session.getLastDocumentVersion(doc.getRef());
            assertEquals("new title", doc.getPropertyValue("dc:title"));
            assertEquals("new creator", doc.getProperty("dc:creator").getValue());
            assertFalse("new description".equals(doc.getPropertyValue("dc:description")));
            assertFalse("new source".equals(doc.getPropertyValue("dc:source")));
            assertEquals("1.0", version.getVersionLabel());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.webapp.base:test-bulkedit-restricted-version-options.xml")
    public void testBulkEditAutoVersioningFailure() throws Exception {

        List<DocumentModel> docs = createTestDocuments();
        docs.forEach(doc -> doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR));
        DocumentModel sourceDoc = buildSimpleDocumentModel();

        try {
            bulkEditService.updateDocuments(session, sourceDoc, docs);
            fail("Creating documents should have failed as the version option does not exist");
        } catch (NuxeoException e) {
            assertEquals(
                    "Versioning option=MINOR is not allowed by the configuration for type=File/lifeCycleState=project",
                    e.getMessage());
        }
    }

}

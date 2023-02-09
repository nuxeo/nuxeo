/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * Tests of the record blob dispatcher.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-blob-dispatcher-record.xml")
public class TestBlobDispatcherRecord {

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected CoreSession session;

    @Inject
    protected BlobManager blobManager;

    protected void assertSaveFail(DocumentModel doc) {
        var exception = assertThrows(DocumentSecurityException.class, () -> session.saveDocument(doc));
        assertEquals(String.format("Cannot change blob from document %s, it is under retention / hold", doc.getRef()),
                exception.getMessage());
    }

    @Test
    public void testDispatchRecord() throws Exception {
        String foo = "foo";
        String foo_test_key = "test:acbd18db4cc2f85cedef654fccc4a4d8";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        Blob blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        String key = ((ManagedBlob) blob).getKey();
        assertEquals(foo_test_key, key);

        // turn the blob into a record
        session.makeRecord(doc.getRef());

        // check that it was dispatched to the record blob provider
        doc.refresh();
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("records1:" + doc.getId(), ((ManagedBlob) blob).getKey());

        // the blob cannot be changed if there is a legal hold
        session.setLegalHold(doc.getRef(), true, null);
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("bar"));
        assertSaveFail(doc);

        // the blob cannot be deleted if there is a legal hold
        doc.setPropertyValue("file:content", null);
        assertSaveFail(doc);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml")
    public void testDispatchRecordAttachements() throws Exception {
        String bar = "bar";
        String bar_test_key = "test:37b51d194a7513e45b56f6524f2d51f2";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "docAttachements", "File");
        Blob blob = Blobs.createBlob(bar, "text/plain");
        doc.setPropertyValue("files:files", (Serializable) List.of(Map.of("file", blob)));
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("files:files/0/file");
        String key = ((ManagedBlob) blob).getKey();
        assertEquals(bar_test_key, key);

        // turn the blob into a record
        session.makeRecord(doc.getRef());

        // check that it was dispatched to the record blob provider
        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("files:files/0/file");
        assertEquals("records1:" + doc.getId() + "-files-0-file", ((ManagedBlob) blob).getKey());

        // the blob cannot be changed if there is a legal hold
        session.setLegalHold(doc.getRef(), true, null);
        doc.setPropertyValue("files:files/0/file", (Serializable) Blobs.createBlob("bar"));
        assertSaveFail(doc);

        // the blob cannot be deleted if there is a legal hold
        doc.setPropertyValue("files:files/0/file", null);
        assertSaveFail(doc);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-retain-blobList-property.xml")
    public void testDispatchRecordBlobList() throws Exception {
        String bar = "bar";
        String bar_test_key = "test:37b51d194a7513e45b56f6524f2d51f2";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "blobList", "BlobList");
        Blob blob = Blobs.createBlob(bar, "text/plain");
        doc.setPropertyValue("blobList:myListOfBlob", (Serializable) List.of(blob));
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("blobList:myListOfBlob/0");
        String key = ((ManagedBlob) blob).getKey();
        assertEquals(bar_test_key, key);

        // turn the blob into a record
        session.makeRecord(doc.getRef());

        // check that it was dispatched to the record blob provider
        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("blobList:myListOfBlob/0");
        assertEquals("records1:" + doc.getId() + "-bl_myListOfBlob-0", ((ManagedBlob) blob).getKey());

        // the blob cannot be changed if there is a legal hold
        session.setLegalHold(doc.getRef(), true, null);
        doc.setPropertyValue("blobList:myListOfBlob/0", (Serializable) Blobs.createBlob("bar"));
        assertSaveFail(doc);

        // the blob cannot be deleted if there is a legal hold
        doc.setPropertyValue("blobList:myListOfBlob/0", null);
        assertSaveFail(doc);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml")
    public void testDispatchRecordAttachementsOverride() throws Exception {
        String bar = "bar";
        String bar_test_key = "test:37b51d194a7513e45b56f6524f2d51f2";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "docAttachements", "File");
        Blob blob = Blobs.createBlob(bar, "text/plain");
        doc.setPropertyValue("files:files", (Serializable) List.of(Map.of("file", blob)));
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("files:files/0/file");
        String key = ((ManagedBlob) blob).getKey();
        assertEquals(bar_test_key, key);

        // turn the blob into a record
        session.makeRecord(doc.getRef());

        // check that it was dispatched to the record blob provider
        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("files:files/0/file");
        assertEquals("records1:" + doc.getId() + "-files-0-file", ((ManagedBlob) blob).getKey());

        deployer.undeploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml");

        // check that it is still dispatched to the record blob provider
        doc = session.getDocument(doc.getRef());
        String foo = "foo";
        blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("files:files", (Serializable) List.of(Map.of("file", blob)));
        doc = session.saveDocument(doc);
        blob = (Blob) doc.getPropertyValue("files:files/0/file");
        assertEquals(foo, ((ManagedBlob) blob).getString());
        assertEquals("records1:" + doc.getId() + "-files-0-file", ((ManagedBlob) blob).getKey());
    }

    @Test
    public void testDispatchRecordCopy() throws Exception {
        String foo = "foo";
        String foo_test_key = "test:acbd18db4cc2f85cedef654fccc4a4d8";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        Blob blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(foo_test_key, ((ManagedBlob) blob).getKey());

        // turn the blob into a record
        session.makeRecord(doc.getRef());
        assertEquals(1, doc.getRetainedProperties().size());
        assertEquals("content", doc.getRetainedProperties().get(0));

        // check that it was dispatched to the record blob provider
        doc.refresh();
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("records1:" + doc.getId(), ((ManagedBlob) blob).getKey());

        // copy the doc
        session.copy(doc.getRef(), new PathRef("/"), "doccopy");

        // check that the copied document is not a record anymore
        DocumentModel docCopy = session.getDocument(new PathRef("/doccopy"));
        assertFalse(docCopy.isRecord());
        assertTrue(docCopy.getRetainedProperties().isEmpty());
        // and its blob is in the regular blob provider
        Blob blobCopy = (Blob) docCopy.getPropertyValue("file:content");
        assertEquals(foo_test_key, ((ManagedBlob) blobCopy).getKey());
    }

    @Test
    public void testDispatchRecordCopyDeep() throws Exception {
        String foo = "foo";
        String foo_test_key = "test:acbd18db4cc2f85cedef654fccc4a4d8";

        // create a folder to be copied
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/folder", "doc", "File");
        Blob blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(foo_test_key, ((ManagedBlob) blob).getKey());

        // turn the blob into a record
        session.makeRecord(doc.getRef());

        // check that it was dispatched to the record blob provider
        doc.refresh();
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("records1:" + doc.getId(), ((ManagedBlob) blob).getKey());

        // copy the folder
        session.copy(folder.getRef(), new PathRef("/"), "foldercopy");

        // check that the copied document is not a record anymore
        DocumentModel docCopy = session.getDocument(new PathRef("/foldercopy/doc"));
        assertFalse(docCopy.isRecord());
        // and its blob is in the regular blob provider
        Blob blobCopy = (Blob) docCopy.getPropertyValue("file:content");
        assertEquals(foo_test_key, ((ManagedBlob) blobCopy).getKey());
    }

    /**
     * Make the version a record, and check that when we restore it the live doc isn't a record.
     */
    @Test
    @Ignore("NXP-28645 but keep test for when versions can be made records")
    public void testDispatchRestoreVersionWhichIsRecord() throws Exception {
        String foo = "foo";
        String foo_test_key = "test:acbd18db4cc2f85cedef654fccc4a4d8";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        Blob blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(foo_test_key, ((ManagedBlob) blob).getKey());

        // check in the version
        DocumentRef verRef = session.checkIn(doc.getRef(), VersioningOption.MINOR, null);

        // turn the version into a record
        session.makeRecord(verRef);

        // check that it was dispatched to the record blob provider
        DocumentModel ver = session.getDocument(verRef);
        blob = (Blob) ver.getPropertyValue("file:content");
        assertEquals("records1:" + ver.getId(), ((ManagedBlob) blob).getKey());

        // restore the version
        session.restoreToVersion(doc.getRef(), verRef);

        // check that the copied version is not a record anymore
        doc.refresh();
        assertFalse(doc.isRecord());
        // and its blob is in the regular blob provider
        Blob blobCopy = (Blob) doc.getPropertyValue("file:content");
        assertEquals(foo_test_key, ((ManagedBlob) blobCopy).getKey());
    }

    /**
     * Slightly different from above: here the live doc is a record, and we restore a non-record version on top of it.
     */
    @Test
    @Ignore("NXP-28645 but keep test for when versions can be made records")
    public void testDispatchRestoreVersionOnTopOfRecord() throws Exception {
        String foo = "foo";
        String foo_test_key = "test:acbd18db4cc2f85cedef654fccc4a4d8";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        Blob blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(foo_test_key, ((ManagedBlob) blob).getKey());

        // check in the version
        DocumentRef verRef = session.checkIn(doc.getRef(), VersioningOption.MINOR, null);

        // turn the live doc into a record
        session.makeRecord(doc.getRef());

        // check that it was dispatched to the record blob provider
        doc.refresh();
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("records1:" + doc.getId(), ((ManagedBlob) blob).getKey());

        // restore the version
        session.restoreToVersion(doc.getRef(), verRef);

        // check that the live doc (now a copy of the version) is not a record anymore
        doc.refresh();
        assertFalse(doc.isRecord());
        // and its blob is in the regular blob provider
        Blob blobCopy = (Blob) doc.getPropertyValue("file:content");
        assertEquals(foo_test_key, ((ManagedBlob) blobCopy).getKey());
    }

}

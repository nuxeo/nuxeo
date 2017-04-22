/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume, jcarsique
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Sample test showing how to use a direct access to the binaries storage.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-blob-dispatcher.xml")
public class TestBlobDispatcher {

    @Inject
    protected CoreSession session;

    @Test
    public void testDirectBlob() throws Exception {
        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        DocumentModel doc2 = session.createDocumentModel("/", "doc1", "File");
        doc1 = session.createDocument(doc1);
        doc2 = session.createDocument(doc2);

        Blob blob1 = Blobs.createBlob("foo", "text/plain");
        doc1.setPropertyValue("file:content", (Serializable) blob1);
        doc1 = session.saveDocument(doc1);

        Blob blob2 = Blobs.createBlob("bar", "video/mp4");
        doc2.setPropertyValue("file:content", (Serializable) blob2);
        doc2 = session.saveDocument(doc2);

        // re-fetch
        doc1 = session.getDocument(doc1.getRef());
        doc2 = session.getDocument(doc2.getRef());

        blob1 = (Blob) doc1.getPropertyValue("file:content");
        try (InputStream in = blob1.getStream()) {
            assertEquals("foo", IOUtils.toString(in));
        }

        blob2 = (Blob) doc2.getPropertyValue("file:content");
        try (InputStream in = blob2.getStream()) {
            assertEquals("bar", IOUtils.toString(in));
        }
    }

    @Test
    public void testAlreadyManagedBlob() throws Exception {
        // register blob in provider by hand
        Blob b = Blobs.createBlob("foo", "video/mp4");
        String key = Framework.getService(BlobManager.class).getBlobProvider("dummy").writeBlob(b);
        key = "dummy:" + key;

        // create a blob already managed and not corresponding to a dispatch target
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = key;
        blobInfo.mimeType = "video/mp4";
        Blob blob = new SimpleManagedBlob(blobInfo);
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check that it wasn't dispatched even though the metadata would suggest it
        blob = (Blob) doc.getPropertyValue("file:content");
        assertTrue(blob.getClass().getName(), blob instanceof SimpleManagedBlob);
        String key2 = ((ManagedBlob) blob).getKey();
        assertEquals(key, key2);
    }

    @Test
    public void testSwitchDispatch() throws Exception {
        String foo = "foo";
        String foo_test_key = "test:acbd18db4cc2f85cedef654fccc4a4d8";
        String foo2_test_key = "test2:acbd18db4cc2f85cedef654fccc4a4d8";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        Blob blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        String key = ((ManagedBlob) blob).getKey();
        assertEquals(foo_test_key, key);

        // update the blob MIME type to change the dispatch target
        blob.setMimeType("video/mp4");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.saveDocument(doc);

        // check that it was dispatched on save to the second blob provider
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(foo2_test_key, ((ManagedBlob) blob).getKey());
    }

    @Test
    public void testSwitchDispatchOnChange() throws Exception {
        String foo = "foo";
        String foo_test_key = "test:acbd18db4cc2f85cedef654fccc4a4d8";
        String foo2_test_key = "test2:acbd18db4cc2f85cedef654fccc4a4d8";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        Blob blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        String key = ((ManagedBlob) blob).getKey();
        assertEquals(foo_test_key, key);

        // update the dc:format to change the dispatch target
        doc.setPropertyValue("dc:format", "video");
        doc = session.saveDocument(doc);

        // check that it was dispatched on save to the second blob provider
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(foo2_test_key, ((ManagedBlob) blob).getKey());
    }

    @Test
    public void testSwitchDispatchOnLifeCycle() throws Exception {
        String foo = "foo";
        String foo_test_key = "test:acbd18db4cc2f85cedef654fccc4a4d8";
        String foo2_test_key = "test2:acbd18db4cc2f85cedef654fccc4a4d8";

        // create a regular binary in the first blob provider
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        Blob blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        String key = ((ManagedBlob) blob).getKey();
        assertEquals(foo_test_key, key);

        // change lifecycle to change the dispatch target
        doc.followTransition("approve");
        doc = session.saveDocument(doc);

        // check that it was dispatched on save to the second blob provider
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(foo2_test_key, ((ManagedBlob) blob).getKey());
    }

    @Test
    public void testDispatchXPath() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);

        Blob blob = Blobs.createBlob("foo", "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.saveDocument(doc);
        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        String key = ((ManagedBlob) blob).getKey();
        assertTrue(key, key.startsWith("test:"));

        // files/0/file gets stored in the second blob provider
        blob = Blobs.createBlob("bar", "text/plain");
        doc.setPropertyValue("files:files",
                (Serializable) Collections.singletonList(Collections.singletonMap("file", blob)));
        doc = session.saveDocument(doc);
        // check binary key
        blob = (Blob) doc.getPropertyValue("files:files/0/file");
        key = ((ManagedBlob) blob).getKey();
        assertTrue(key, key.startsWith("test2:"));
    }

    // same with low-level Document API
    // (has a different code path to compute the xpath)
    @Test
    public void testDispatchXPathOnDocument() throws Exception {
        Session documentSession = ((AbstractSession) session).getSession();
        Document root = documentSession.getRootDocument();
        Document doc = root.addChild("doc", "File");

        Blob blob = Blobs.createBlob("foo", "text/plain");
        doc.setValue("content", blob);
        // check binary key
        blob = (Blob) doc.getValue("content");
        String key = ((ManagedBlob) blob).getKey();
        assertTrue(key, key.startsWith("test:"));

        // files/0/file gets stored in the second blob provider
        blob = Blobs.createBlob("bar", "text/plain");
        doc.setValue("files", Collections.singletonList(Collections.singletonMap("file", blob)));
        // check binary key
        blob = (Blob) doc.getValue("files/0/file");
        key = ((ManagedBlob) blob).getKey();
        assertTrue(key, key.startsWith("test2:"));
    }

}

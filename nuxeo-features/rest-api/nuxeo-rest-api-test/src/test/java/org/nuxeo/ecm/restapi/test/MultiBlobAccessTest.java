/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

/**
 * @since 5.8
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test:multiblob-doctype.xml")
public class MultiBlobAccessTest extends BaseTest {

    @Inject
    CoreSession session;

    private DocumentModel doc;

    @Override
    @Before
    public void doBefore() throws Exception {
        super.doBefore();
        doc = session.createDocumentModel("/", "testBlob", "MultiBlobDoc");
        addBlob(doc, Blobs.createBlob("one"));
        addBlob(doc, Blobs.createBlob("two"));
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void itDoesNotUpdateBlobsThroughDocEndpoint() throws Exception {
        String docJsonIN;
        Map<String, String> headers = new HashMap<>();
        headers.put("properties", "multiblob");
        try (CloseableClientResponse response = getResponse(RequestType.GET, "path" + doc.getPathAsString(), headers)) {
            docJsonIN = IOUtils.toString(response.getEntityInputStream());
        }
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "path" + doc.getPathAsString(), docJsonIN,
                headers)) {
            DocumentModel doc = session.getDocument(new PathRef("/testBlob"));
            assertEquals(2, ((List<?>) doc.getProperty("mb:blobs").getValue()).size());
            Blob blob1 = (Blob) doc.getProperty("mb:blobs/0/content").getValue();
            assertNotNull(blob1);
            assertEquals("one", blob1.getString());
            Blob blob2 = (Blob) doc.getProperty("mb:blobs/1/content").getValue();
            assertNotNull(blob2);
            assertEquals("two", blob2.getString());
        }
    }

    @Test
    public void itCanAccessBlobs() throws Exception {
        // When i call the rest api
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path" + doc.getPathAsString() + "/@blob/mb:blobs/0/content")) {

            // Then i receive the content of the blob
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertEquals("one", response.getEntity(String.class));
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path" + doc.getPathAsString() + "/@blob/mb:blobs/1/content")) {

            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertEquals("two", response.getEntity(String.class));
        }
    }

    @Test
    public void itCanModifyABlob() throws Exception {
        // Given a doc with a blob

        // When i send a PUT with a new value on the blob
        try (FormDataMultiPart form = new FormDataMultiPart()) {
            BodyPart fdp = new StreamDataBodyPart("content", new ByteArrayInputStream("modifiedData".getBytes()));
            form.bodyPart(fdp);
            try (CloseableClientResponse response = getResponse(RequestType.PUT,
                    "path" + doc.getPathAsString() + "/@blob/mb:blobs/0/content", form)) {
                // The the blob is updated
                fetchInvalidations();
                doc = getTestBlob();
                Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
                assertEquals("modifiedData", blob.getString());
            }
        }
    }

    @Test
    public void itCanRemoveABlob() throws Exception {
        // Given a doc with a blob

        // When i send A DELETE command on its blob
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "path" + doc.getPathAsString() + "/@blob/mb:blobs/0/content")) {

            // The the blob is reset
            fetchInvalidations();
            doc = getTestBlob();
            Blob blob = (Blob) doc.getPropertyValue("mb:blobs/0/content");
            assertNull(blob);
        }
    }

    private DocumentModel getTestBlob() {
        return session.getDocument(new PathRef("/testBlob"));
    }

    private void addBlob(DocumentModel doc, Blob blob) {
        Map<String, Serializable> blobProp = new HashMap<>();
        blobProp.put("content", (Serializable) blob);
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> blobs = (List<Map<String, Serializable>>) doc.getPropertyValue("mb:blobs");
        blobs.add(blobProp);
        doc.setPropertyValue("mb:blobs", (Serializable) blobs);
    }

    @Test
    public void itCanAccessBlobsThroughBlobHolder() throws Exception {
        DocumentModel doc = getTestBlob();
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        bh.setBlob(Blobs.createBlob("main"));
        doc = session.saveDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // When i call the rest api
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path" + doc.getPathAsString() + "/@blob/blobholder:0")) {

            // Then i receive the content of the blob
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertEquals("main", response.getEntity(String.class));
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path" + doc.getPathAsString() + "/@blob/blobholder:1")) {

            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertEquals("one", response.getEntity(String.class));
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path" + doc.getPathAsString() + "/@blob/blobholder:2")) {

            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertEquals("two", response.getEntity(String.class));
        }
    }

}

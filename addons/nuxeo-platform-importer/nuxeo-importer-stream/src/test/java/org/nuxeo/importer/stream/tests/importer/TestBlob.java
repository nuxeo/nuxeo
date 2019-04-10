/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.importer.stream.tests.importer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Play with blob provider.
 *
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBlob {

    @Inject
    CoreSession session;

    @Test
    public void testBlobKeys() throws Exception {
        // 1. create a blob an save it into the binary store
        String blobProviderName = "test";
        Blob blob = createBlob("Some content", "filename1.txt");
        String digest = saveBlobOnBlobProvider(blobProviderName, blob);
        assertEquals("b53227da4280f0e18270f21dd77c91d0", digest);

        // 2. create a blob with same content but different attribute
        Blob blob2 = createBlob("Some content", "filename123424.txt");
        String digest2 = saveBlobOnBlobProvider(blobProviderName, blob2);
        assertEquals(digest, digest2);

    }

    @Test
    public void testCreateDocumentWithBlobRef() throws Exception {
        // 1. create a blob an save it into the binary store
        String blobProviderName = "test";
        Blob blob = createBlob("Some content blob", "filename.txt");
        String digest = saveBlobOnBlobProvider(blobProviderName, blob);
        assertEquals("22b5f8fdb555328519b21f25d3130d68", digest);
        assertEquals(17, blob.getLength());

        // 2. get the blob info to keep and create a ref
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = blobProviderName + ":" + digest;
        blobInfo.digest = digest;
        blobInfo.encoding = blob.getEncoding();
        blobInfo.mimeType = blob.getMimeType();
        blobInfo.filename = blob.getFilename();
        blobInfo.length = blob.getLength();
        ManagedBlob blobLink = new SimpleManagedBlob(blobInfo);

        // 3. create a document model
        DocumentModel doc = createDocumentModel();

        // 4. add blob to the doc and commit
        doc.setProperty("file", "content", blobLink);
        doc = session.createDocument(doc);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // reload the doc and check
        doc = session.getDocument(doc.getRef());
        Blob readBlob = (Blob) doc.getProperty("file", "content");

        assertEquals(digest, readBlob.getDigest());
        assertEquals(blob.getString(), blob.getString());
        assertEquals(blob.getMimeType(), readBlob.getMimeType());
        assertEquals(blob.getLength(), readBlob.getLength());
        assertEquals(blob.getFilename(), readBlob.getFilename());
        assertEquals(blob.getEncoding(), readBlob.getEncoding());
        assertEquals(17, blob.getLength());
    }

    protected DocumentModel createDocumentModel() {
        String rootPath = session.getRootDocument().getPathAsString();
        return session.createDocumentModel(rootPath, "docname", "File");
    }

    protected String saveBlobOnBlobProvider(String providerName, Blob blob) throws IOException {
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(providerName);
        return blobProvider.writeBlob(blob);
    }

    protected Blob createBlob(String content, String filename) {
        return new StringBlob(content, "plain/text", UTF_8.name(), filename);
    }

}

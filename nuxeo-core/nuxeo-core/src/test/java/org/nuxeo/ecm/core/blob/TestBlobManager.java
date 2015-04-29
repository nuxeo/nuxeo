/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(BlobManagerFeature.class)
@LocalDeploy("org.nuxeo.ecm.core:OSGI-INF/fake-blob-provider-component.xml")
public class TestBlobManager {

    private static final String FAKE = FakeBlobProviderComponent.FAKE_BLOB_PROVIDER_PREFIX;

    Mockery mockery = new JUnit4Mockery();

    @Inject
    protected BlobManager blobManager;

    @Test
    public void testFakeRegistration() throws Exception {
        BlobProvider fakeBlobProvider = blobManager.getBlobProvider(FAKE);
        assertNotNull(fakeBlobProvider);
    }

    @Test
    public void testGetSetMetadata() throws Exception {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = FAKE + ":1234";
        blobInfo.mimeType = "test/type";
        blobInfo.encoding = "UTF-8";
        blobInfo.filename = "doc.ext";
        blobInfo.length = Long.valueOf(123);
        blobInfo.digest = "55667788";

        final Document doc = mockery.mock(Document.class);
        mockery.checking(new Expectations() {
            {
                allowing(doc).getRepositoryName();
                will(returnValue("somerepo"));
                //
                // allowing(doc).getDatabaseMajorVersion();
                // will(returnValue(9));
                //
                // allowing(doc).getDatabaseMinorVersion();
                // will(returnValue(0));
                //
                // allowing(doc).getColumns(with(any(String.class)), with(any(String.class)), with(any(String.class)),
                // with(any(String.class)));
                // will(returnValue(getMockEmptyResultSet()));
                //
                // allowing(doc).getConnection();
                // will(returnValue(getMockConnection()));
            }
        });
        ManagedBlob blob = (ManagedBlob) blobManager.readBlob(blobInfo, doc);
        assertNotNull(blob);
        assertEquals("fake:1234", blob.getKey());
        assertEquals("test/type", blob.getMimeType());
        assertEquals("UTF-8", blob.getEncoding());
        assertEquals("doc.ext", blob.getFilename());
        assertEquals(123, blob.getLength());
        assertEquals("55667788", blob.getDigest());

        BlobInfo bi = blobManager.writeBlob(blob, doc);
        assertEquals("fake:1234", bi.key);
        assertEquals("test/type", bi.mimeType);
        assertEquals("UTF-8", bi.encoding);
        assertEquals("doc.ext", bi.filename);
        assertEquals(Long.valueOf(123), bi.length);
        assertEquals("55667788", bi.digest);
    }

}

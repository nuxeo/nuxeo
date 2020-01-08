/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.wopi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.wopi.TestConstants.FILE_CONTENT_PROPERTY;

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.wopi.exception.UnauthorizedException;

/**
 * Tests the {@link Helpers} class.
 *
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(WOPIFeature.class)
@Deploy("org.nuxeo.ecm.jwt")
@Deploy("org.nuxeo.wopi:OSGI-INF/test-jwt-contrib.xml")
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/dummy-blob-provider.xml")
public class TestHelpers {

    @Inject
    protected CoreSession session;

    @Inject
    protected BlobManager blobManager;

    @Test
    public void testGetEditableBlob() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "wopiDoc", "File");

        // no blob
        assertNull(Helpers.getEditableBlob(doc, FILE_CONTENT_PROPERTY));

        // regular blob
        Blob blob = Blobs.createBlob("dummy content");
        doc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
        doc = session.createDocument(doc);
        assertNotNull(Helpers.getEditableBlob(doc, FILE_CONTENT_PROPERTY));

        // external blob provider
        BlobProvider blobProvider = blobManager.getBlobProvider("dummy");
        blobProvider.writeBlob(blob);
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = "dummy:1";
        ManagedBlob externalBlob = (ManagedBlob) blobProvider.readBlob(blobInfo);
        assertNotNull(externalBlob);
        doc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) externalBlob);
        assertNull(Helpers.getEditableBlob(doc, FILE_CONTENT_PROPERTY));
    }

    @Test
    public void testGetJWToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        // null token
        try {
            Helpers.getJWTToken(request);
            fail("UnauthorizedException should be thrown.");
        } catch (UnauthorizedException e) {
            assertEquals(401, e.getStatusCode());
        }

        // invalid token
        try {
            when(request.getParameter("access_token")).thenReturn("invalidToken");
            Helpers.getJWTToken(request);
            fail("UnauthorizedException should be thrown.");
        } catch (UnauthorizedException e) {
            assertEquals(401, e.getStatusCode());
        }

        // valid token
        String token = Helpers.createJWTToken();
        when(request.getParameter("access_token")).thenReturn(token);
        assertEquals(token, Helpers.getJWTToken(request));
    }
}

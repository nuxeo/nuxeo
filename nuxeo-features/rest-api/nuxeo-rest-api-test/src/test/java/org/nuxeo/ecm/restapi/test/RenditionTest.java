/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.restapi.test;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.core.io.download.DownloadService.EXTENDED_INFO_RENDITION;
import static org.nuxeo.ecm.core.io.download.DownloadService.REQUEST_ATTR_DOWNLOAD_RENDITION;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, AuditFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.rendition.api")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.platform.restapi.test:renditions-test-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.platform.thumbnail")
public class RenditionTest extends BaseTest {

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void shouldRetrieveTheRendition() {
        DocumentModel doc = session.createDocumentModel("/", "adoc", "File");
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path" + doc.getPathAsString() + "/@rendition/dummyRendition")) {
            assertEquals(200, response.getStatus());
            assertEquals("adoc", response.getEntity(String.class));
        }
    }

    // NXP-30483
    @Test
    public void shouldRetrieveTheImageToPdfRendition() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "adoc", "File");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"), "image/jpeg",
                                     StandardCharsets.UTF_8.name(), "test.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc.putContextData(ThumbnailConstants.DISABLE_THUMBNAIL_COMPUTATION, true); // not useful for us
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                                                            "path" + doc.getPathAsString() + "/@rendition/pdf")) {
            assertEquals(200, response.getStatus());
        }

        // NXP-31383
        txFeature.nextTransaction();
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path" + doc.getPathAsString() + "/@audit")) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("\"pdf\"", node.get("entries").get(0).get("extended").get(EXTENDED_INFO_RENDITION).toString());
            assertNull(node.get("entries").get(0).get("extended").get(REQUEST_ATTR_DOWNLOAD_RENDITION));
        }
    }

    @Test
    public void shouldFailForNonExistingRendition() {
        DocumentModel doc = session.createDocumentModel("/", "adoc", "File");
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path" + doc.getPathAsString() + "/@rendition/unexistingRendition")) {
            assertEquals(500, response.getStatus()); // should be 404?
        }
    }

    // NXP-31166
    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.test:download-permission-contrib.xml")
    public void shouldRetrieveRenditionsBasedOnDoc() {
        DocumentModel doc = session.createDocumentModel("/", "downloadable", "Folder");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/downloadable", "reachable", "File");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/", "unreachable", "File");
        doc = session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path/downloadable/reachable/@rendition/dummyRendition")) {
            assertEquals(SC_OK, response.getStatus());
            assertEquals("reachable", response.getEntity(String.class));
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path/unreachable/@rendition/dummyRendition")) {
            assertEquals(SC_FORBIDDEN, response.getStatus());
        }
    }

}

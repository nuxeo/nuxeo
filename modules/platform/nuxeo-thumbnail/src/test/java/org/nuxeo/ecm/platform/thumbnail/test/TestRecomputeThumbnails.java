/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.thumbnail.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.AutomationCoreFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.ecm.platform.thumbnail.operation.RecomputeThumbnails;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests the {@link RecomputeThumbnails} operation.
 *
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationCoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.thumbnail")
public class TestRecomputeThumbnails {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected ThumbnailService thumbnailService;

    @Test
    public void testRecomputeThumbnails() throws IOException, OperationException {
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/big_nuxeo_logo.jpg"), "image/jpeg",
                StandardCharsets.UTF_8.name(), "big_nuxeo_logo.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // wait for thumbnail generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        Blob thumbnail = thumbnailService.getThumbnail(doc, session);
        assertNotNull(thumbnail);

        // empty thumbnail
        doc.setPropertyValue("thumbnail:thumbnail", null);
        session.saveDocument(doc);
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        thumbnail = thumbnailService.getThumbnail(doc, session);
        assertNull(thumbnail);

        // call operation to recompute the thumbnails with the default query
        Map<String, String> parameters = Collections.singletonMap("query", RecomputeThumbnails.DEFAULT_QUERY);
        try (OperationContext ctx = new OperationContext(session)) {
            automationService.run(ctx, RecomputeThumbnails.ID, parameters);
        }

        // wait for thumbnails generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        thumbnail = thumbnailService.getThumbnail(doc, session);
        assertNotNull(thumbnail);

        // call operation to recompute the thumbnails with a custom query
        parameters = Collections.singletonMap("query", "SELECT * FROM Document WHERE ecm:mixinType = 'Thumbnail'");
        try (OperationContext ctx = new OperationContext(session)) {
            automationService.run(ctx, RecomputeThumbnails.ID, parameters);
        }

        // wait for thumbnails generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        thumbnail = thumbnailService.getThumbnail(doc, session);
        assertNotNull(thumbnail);
    }

    @Test
    public void testThumbnailsForTIFF() throws IOException {

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/at3_1m4_01.tif"), "image/tiff",
                StandardCharsets.UTF_8.name(), "at3_1m4_01.tif");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // wait for thumbnail generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        Blob thumbnail = thumbnailService.getThumbnail(doc, session);
        assertNotNull("the TIFF thumbnail is not generated", thumbnail);
    }

    @Test
    public void testNoThumbnailWhenDisabled() throws IOException, OperationException {
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/big_nuxeo_logo.jpg"), "image/jpeg",
                StandardCharsets.UTF_8.name(), "big_nuxeo_logo.jpg");

        DocumentModel fileWithoutThumbnail = session.createDocumentModel("/", "fileWithoutThumbnail", "File");
        fileWithoutThumbnail.putContextData(ThumbnailConstants.DISABLE_THUMBNAIL_COMPUTATION, true);
        fileWithoutThumbnail.setPropertyValue("file:content", (Serializable) blob);
        fileWithoutThumbnail = session.createDocument(fileWithoutThumbnail);

        txFeature.nextTransaction();
        fileWithoutThumbnail = session.getDocument(fileWithoutThumbnail.getRef());
        assertNull(thumbnailService.getThumbnail(fileWithoutThumbnail, session));
    }

}

/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.VersioningOption.MAJOR;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.platform.picture.PictureViewsHelper;
import org.nuxeo.ecm.platform.picture.core.ImagingFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/** @since 2021.27 */
@RunWith(FeaturesRunner.class)
@Features(ImagingFeature.class)
public class TestComputePictureViews {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected BulkService bulkService;

    protected PictureViewsHelper pvh = new PictureViewsHelper();

    @Test
    public void testComputePictureViewsOnCreatedDocumentVersion() throws IOException, OperationException {
        // Take into account the initial status count.
        var initialCount = bulkService.getStatuses("Administrator").size();

        DocumentModel doc = session.createDocumentModel("/", "pictureDoc", "Picture");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"), "image/jpeg",
                StandardCharsets.UTF_8.name(), "test.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);
        DocumentRef versionRef = doc.checkIn(MAJOR, "version a new doc and check we compute its picture views");
        DocumentModel version = session.getDocument(versionRef);
        var statuses = bulkService.getStatuses("Administrator");
        assertEquals(initialCount, statuses.size());
        assertTrue(pvh.hasPrefillPictureViews(doc));
        assertTrue(pvh.hasPrefillPictureViews(version));

        // Wait for picture views generation.
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        version = session.getDocument(versionRef);
        statuses = bulkService.getStatuses("Administrator");
        assertEquals(initialCount + 2, statuses.size());
        assertTrue(hasPictureViews(doc));
        assertFalse(pvh.hasPrefillPictureViews(doc));
        assertTrue(hasPictureViews(version));
        assertFalse(pvh.hasPrefillPictureViews(version));

        // Make a light change to allow another check in.
        doc.setPropertyValue("dc:title", "test");
        doc = session.saveDocument(doc);
        versionRef = doc.checkIn(MAJOR, "version a new doc and check we don't recompute its picture views");

        // No generation should have been submitted.
        txFeature.nextTransaction();
        statuses = bulkService.getStatuses("Administrator");
        assertEquals(initialCount + 2, statuses.size());
        version = session.getDocument(versionRef);
        assertTrue(hasPictureViews(version));
    }

    @SuppressWarnings("unchecked")
    protected boolean hasPictureViews(DocumentModel doc) {
        var pictureViews = (List<Serializable>) doc.getPropertyValue("picture:views");
        return pictureViews != null && !pictureViews.isEmpty();
    }
}

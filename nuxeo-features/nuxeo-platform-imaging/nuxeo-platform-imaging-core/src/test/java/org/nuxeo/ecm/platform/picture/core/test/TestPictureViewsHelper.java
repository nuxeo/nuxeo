/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Vincent Dutat <vdutat@nuxeo.com>
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_INFO_PROPERTY;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.PictureViewsHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2021.12
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.tag")
public class TestPictureViewsHelper {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testComputePictureViewsModificationDate() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "pictureDoc", "Picture");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"), "image/jpeg",
                StandardCharsets.UTF_8.name(), "test.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // wait for picture views generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        List<Serializable> pictureViews = (List<Serializable>) doc.getPropertyValue("picture:views");
        assertNotNull(pictureViews);
        assertFalse(pictureViews.isEmpty());
        Calendar lastModificationDateBefore = (Calendar) doc.getPropertyValue("dc:modified");
        assertNotNull(lastModificationDateBefore);

        PictureViewsHelper ph = new PictureViewsHelper();
        ph.computePictureViews(session, doc.getId(), "file:content", s -> {
        });

        // wait for picture views generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        pictureViews = (List<Serializable>) doc.getPropertyValue("picture:views");
        assertNotNull(pictureViews);
        assertFalse(pictureViews.isEmpty());
        Calendar lastModificationAfter = (Calendar) doc.getPropertyValue("dc:modified");
        assertNotNull(lastModificationAfter);
        assertEquals(lastModificationDateBefore.getTimeInMillis(), lastModificationAfter.getTimeInMillis());
    }

    // NXP-31342
    @Test
    public void testPictureInfoReset() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "pictureDoc", "Picture");
        doc = session.createDocument(doc);
        // wait for picture views generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        Serializable originalPictureInfo = doc.getPropertyValue(PICTURE_INFO_PROPERTY);
        assertNotNull(originalPictureInfo);

        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"), "image/jpeg",
                StandardCharsets.UTF_8.name(), "test.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.saveDocument(doc);
        // wait for picture views generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        // Picture info has evolved
        assertNotEquals(originalPictureInfo, doc.getPropertyValue(PICTURE_INFO_PROPERTY));

        // When nullifying main blob
        doc.setPropertyValue("file:content", null);
        doc = session.saveDocument(doc);
        // Picture info is immediately back to the original empty one
        assertEquals(originalPictureInfo, doc.getPropertyValue(PICTURE_INFO_PROPERTY));
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        // even after async recomputation
        assertEquals(originalPictureInfo, doc.getPropertyValue(PICTURE_INFO_PROPERTY));
    }

}

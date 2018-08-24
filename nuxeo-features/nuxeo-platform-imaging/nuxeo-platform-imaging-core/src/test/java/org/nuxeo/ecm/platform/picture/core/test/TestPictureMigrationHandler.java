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

package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.PictureMigrationHandler;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.collections.core:OSGI-INF/collection-core-types-contrib.xml")
public class TestPictureMigrationHandler {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Test
    public void testPictureMigration() throws IOException {
        doTestPictureMigration("test.jpg", "Original_test.jpg");
    }

    @Test
    public void testPictureMigrationNullFilename() throws IOException {
        doTestPictureMigration("", null);
    }

    public void doTestPictureMigration(String expected, String filename) throws IOException {
        // create an "old" picture
        List<Map<String, Serializable>> views = new ArrayList<>();
        Map<String, Serializable> map = new HashMap<>();
        map.put("title", "Original");
        Blob originalBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext(ImagingResourcesHelper.TEST_DATA_FOLDER
                + "test.jpg"), "image/jpeg", null, null);
        originalBlob.setFilename(filename); // don't default to file's name when null
        map.put("content", (Serializable) originalBlob);
        map.put("filename", filename);
        views.add(map);
        Blob thumbnailBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext(ImagingResourcesHelper.TEST_DATA_FOLDER
                + "test.jpg"), "image/jpeg", null, "Thumbnail_test.jpg");
        map = new HashMap<>();
        map.put("title", "Thumbnail");
        map.put("content", (Serializable) thumbnailBlob);
        map.put("filename", "Thumbnail_test.jpg");
        views.add(map);

        DocumentModel picture = session.createDocumentModel("/", "picture", "Picture");
        picture.setPropertyValue("picture:views", (Serializable) views);
        picture = session.createDocument(picture);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertNotNull(picture);
        assertNull(picture.getPropertyValue("file:content"));
        MultiviewPicture multiviewPicture = picture.getAdapter(MultiviewPicture.class);
        PictureView[] pictureViews = multiviewPicture.getViews();
        assertNotNull(pictureViews);
        assertEquals(2, pictureViews.length);

        // do the migration
        PictureMigrationHandler pictureMigrationHandler = new PictureMigrationHandler();
        pictureMigrationHandler.doInitializeRepository(session);

        // check that it's correctly migrated
        picture = session.getDocument(picture.getRef());
        assertNotNull(picture);
        BlobHolder bh = picture.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        Blob blob = bh.getBlob();
        assertNotNull(blob);
        String blobFilename = blob.getFilename();
        if ("".equals(expected) && !expected.equals(blobFilename)
                && coreFeature.getStorageConfiguration().isVCSOracle()) {
            // Oracle confuses "" and null
            assertNull(blobFilename);
        } else {
            assertEquals(expected, blobFilename);
        }
        assertNotNull(picture.getPropertyValue("file:content"));
        multiviewPicture = picture.getAdapter(MultiviewPicture.class);
        pictureViews = multiviewPicture.getViews();
        assertNotNull(pictureViews);
        assertEquals(1, pictureViews.length);
        assertNull(multiviewPicture.getView("Original"));
        assertNotNull(multiviewPicture.getView("Thumbnail"));
    }
}

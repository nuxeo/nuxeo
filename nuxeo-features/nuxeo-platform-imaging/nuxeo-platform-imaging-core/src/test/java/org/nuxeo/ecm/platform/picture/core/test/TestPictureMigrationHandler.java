/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.picture.PictureMigrationHandler;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.picture.api", "org.nuxeo.ecm.platform.picture.convert",
        "org.nuxeo.ecm.platform.picture.core", "org.nuxeo.ecm.platform.commandline.executor" })
public class TestPictureMigrationHandler {

    @Inject
    protected CoreSession session;

    @Test
    public void testPictureMigration() throws IOException {
        // create an "old" picture
        List<Map<String, Serializable>> views = new ArrayList<>();
        Map<String, Serializable> map = new HashMap<>();
        map.put("title", "Original");
        Blob originalBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext(ImagingResourcesHelper.TEST_DATA_FOLDER
                + "test.jpg"), "image/jpeg", null, "Original_test.jpg");
        map.put("content", (Serializable) originalBlob);
        map.put("filename", "Original_test.jpg");
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
        assertEquals("test.jpg", blob.getFilename());
        assertNotNull(picture.getPropertyValue("file:content"));
        multiviewPicture = picture.getAdapter(MultiviewPicture.class);
        pictureViews = multiviewPicture.getViews();
        assertNotNull(pictureViews);
        assertEquals(1, pictureViews.length);
        assertNull(multiviewPicture.getView("Original"));
        assertNotNull(multiviewPicture.getView("Thumbnail"));
    }
}

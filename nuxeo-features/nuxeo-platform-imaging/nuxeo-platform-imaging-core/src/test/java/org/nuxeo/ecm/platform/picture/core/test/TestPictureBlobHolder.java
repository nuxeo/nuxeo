/*
 * (C) Copyright 2007-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Laurent Doguin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTUREBOOK_TYPE_NAME;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_TYPE_NAME;

import java.io.File;
import java.io.Serializable;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureBlobHolder;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureBookBlobHolder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.tag")
public class TestPictureBlobHolder {

    @Inject
    protected BlobHolderAdapterService service;

    @Inject
    protected CoreSession session;

    private static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return file;
    }

    @Test
    public void testBasics() {
        DocumentModel pictureDoc = new DocumentModelImpl(PICTURE_TYPE_NAME);
        pictureDoc.addFacet(PICTURE_FACET);
        BlobHolder bh = pictureDoc.getAdapter(BlobHolder.class);
        assertTrue(bh instanceof PictureBlobHolder);

        DocumentModel docWithPictureFacet = new DocumentModelImpl("MyType");
        docWithPictureFacet.addFacet(PICTURE_FACET);
        bh = docWithPictureFacet.getAdapter(BlobHolder.class);
        assertTrue(bh instanceof PictureBlobHolder);

        DocumentModel pictureBookDoc = new DocumentModelImpl(PICTUREBOOK_TYPE_NAME);
        BlobHolder pbbh = service.getBlobHolderAdapter(pictureBookDoc);
        assertTrue(pbbh instanceof PictureBookBlobHolder);
        pbbh = pictureBookDoc.getAdapter(BlobHolder.class);
        assertTrue(pbbh instanceof PictureBookBlobHolder);
    }

    @Test
    public void testBlobHolder() throws Exception {
        DocumentModel picturebook = session.createDocumentModel("/", "picturebook", PICTUREBOOK_TYPE_NAME);
        session.createDocument(picturebook);
        DocumentModel picture = session.createDocumentModel(picturebook.getPathAsString(), "pic1", PICTURE_TYPE_NAME);
        picture.setPropertyValue("file:content", (Serializable) Blobs.createBlob(
                getFileFromPath("images/exif_sample.jpg"), "image/jpeg", null, "mysample.jpg"));
        picture = session.createDocument(picture);
        DocumentModel picture2 = session.createDocumentModel(picturebook.getPathAsString(), "pic2", PICTURE_TYPE_NAME);
        picture2.setPropertyValue("file:content", (Serializable) Blobs.createBlob(
                getFileFromPath("images/exif_sample.jpg"), "image/jpeg", null, "mysample.jpg"));
        session.createDocument(picture2);
        session.save();

        BlobHolder bh = picturebook.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        Blob blob = bh.getBlob();
        assertNotNull(blob);
        assertEquals(2, bh.getBlobs().size());

        bh = picture.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        blob = bh.getBlob();
        assertNotNull(blob);
        assertEquals(1, bh.getBlobs().size());

        // test blob content
        assertEquals("mysample.jpg", blob.getFilename());
        assertEquals("image/jpeg", blob.getMimeType());
        byte[] bytes = IOUtils.toByteArray(blob.getStream());
        assertEquals(134561, bytes.length);
        bytes = null;
    }

}

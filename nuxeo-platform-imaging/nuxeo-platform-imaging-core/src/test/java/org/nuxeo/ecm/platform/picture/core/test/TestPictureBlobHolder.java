/*
 * (C) Copyright 2007-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Laurent Doguin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTUREBOOK_TYPE_NAME;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_TYPE_NAME;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureBlobHolder;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureBookBlobHolder;
import org.nuxeo.runtime.api.Framework;

public class TestPictureBlobHolder extends SQLRepositoryTestCase {

    BlobHolderAdapterService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        openSession();
        service = Framework.getLocalService(BlobHolderAdapterService.class);
        assertNotNull(service);
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
        service = null;
    }

    private static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return file;
    }

    private static List<Map<String, Serializable>> createViews() {
        List<Map<String, Serializable>> views = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("title", "Original");
        map.put("content", new FileBlob(
                getFileFromPath("images/exif_sample.jpg"), "image/jpeg", null,
                "mysample.jpg", null));
        map.put("filename", "mysample.jpg");
        views.add(map);
        return views;
    }

    public void testBasics() {
        DocumentModel pictureDoc = new DocumentModelImpl(PICTURE_TYPE_NAME);
        pictureDoc.addFacet(PICTURE_FACET);
        BlobHolder bh = pictureDoc.getAdapter(BlobHolder.class);
        assertTrue(bh instanceof PictureBlobHolder);

        DocumentModel docWithPictureFacet = new DocumentModelImpl("MyType");
        docWithPictureFacet.addFacet(PICTURE_FACET);
        bh = docWithPictureFacet.getAdapter(BlobHolder.class);
        assertTrue(bh instanceof PictureBlobHolder);

        DocumentModel pictureBookDoc = new DocumentModelImpl(
                PICTUREBOOK_TYPE_NAME);
        BlobHolder pbbh = service.getBlobHolderAdapter(pictureBookDoc);
        assertTrue(pbbh instanceof PictureBookBlobHolder);
        pbbh = pictureBookDoc.getAdapter(BlobHolder.class);
        assertTrue(pbbh instanceof PictureBookBlobHolder);
    }

    public void testBlobHolder() throws Exception {
        DocumentModel picturebook = new DocumentModelImpl("/", "picturebook",
                PICTUREBOOK_TYPE_NAME);
        session.createDocument(picturebook);
        DocumentModel picture = new DocumentModelImpl(
                picturebook.getPathAsString(), "pic1", PICTURE_TYPE_NAME);
        picture.setPropertyValue("picture:views", (Serializable) createViews());
        picture = session.createDocument(picture);
        DocumentModel picture2 = new DocumentModelImpl(
                picturebook.getPathAsString(), "pic2", PICTURE_TYPE_NAME);
        picture2.setPropertyValue("picture:views", (Serializable) createViews());
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
        byte[] bytes = FileUtils.readBytes(blob.getStream());
        assertEquals(134561, bytes.length);
        bytes = null;
    }

}

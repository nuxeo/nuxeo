/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.picture.core.test;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureBlobHolder;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Laurent Doguin
 *
 */
public class TestPictureBlobHolder extends RepositoryOSGITestCase {

    DocumentModel root;

    BlobHolderAdapterService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        openRepository();
        root = getCoreSession().getRootDocument();
        assertNotNull(root);
        service = Framework.getLocalService(BlobHolderAdapterService.class);
        assertNotNull(service);

        DocumentModel pictureDoc = new DocumentModelImpl("Picture");
        BlobHolder bh = pictureDoc.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        assertTrue(bh instanceof PictureBlobHolder);

        DocumentModel pictureBookDoc = new DocumentModelImpl("PictureBook");
        BlobHolder pbbh = service.getBlobHolderAdapter(pictureBookDoc);
        assertNotNull(pbbh);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        service = null;
    }

    private static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return file;
    }

    private List<Map<String, Serializable>> createViews(){
        List<Map<String, Serializable>> views = new ArrayList<Map<String,Serializable>>();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("title","Original");
        map.put("content",new FileBlob(getFileFromPath("exif_sample.jpg")));
        views.add(map);
        return views;
    }

  public void testBlobHolder() throws ClientException{
      DocumentModel picturebook = new DocumentModelImpl(root.getPathAsString(),
              "picturebook", "PictureBook");
      coreSession.createDocument(picturebook);
      DocumentModel picture = new DocumentModelImpl(picturebook.getPathAsString(),
              "pic1", "Picture");
      picture.setPropertyValue("picture:views", (Serializable) createViews());
      coreSession.createDocument(picture);
      DocumentModel picture2 = new DocumentModelImpl(picturebook.getPathAsString(),
              "pic2", "Picture");
      picture2.setPropertyValue("picture:views", (Serializable) createViews());
      coreSession.createDocument(picture2);
      coreSession.save();

      BlobHolder bh = service.getBlobHolderAdapter(picturebook);
      assertNotNull(bh);
      Blob pic1Blob = bh.getBlob();
      assertNotNull(pic1Blob);
      List<Blob> blobList = bh.getBlobs();
      assertTrue(blobList.size()>1);

      bh = service.getBlobHolderAdapter(picture);
      pic1Blob = bh.getBlob();
      assertNotNull(pic1Blob);

      blobList = bh.getBlobs();
      assertTrue(blobList.size() == 1);


  }
}

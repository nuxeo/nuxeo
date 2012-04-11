/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.core.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author btatar
 *
 */
public class TestImageInfo extends RepositoryOSGITestCase {

    protected DocumentModel root;

    protected BlobHolderAdapterService blobHolderService;

    protected ImagingService imagingService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");

        openRepository();
        root = getCoreSession().getRootDocument();
        assertNotNull(root);
        blobHolderService = Framework.getLocalService(BlobHolderAdapterService.class);
        assertNotNull(blobHolderService);
        imagingService = Framework.getService(ImagingService.class);
        assertNotNull(imagingService);
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
        blobHolderService = null;
        imagingService = null;
    }

    private List<Map<String, Serializable>> createViews() {
        List<Map<String, Serializable>> views = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("title", "Original");
        map.put("content",
                StreamingBlob.createFromURL(this.getClass().getClassLoader().getResource(
                        "images/exif_sample.jpg")));
        views.add(map);
        return views;
    }

    @Test
    public void testGetImageInfo() throws ClientException {
        DocumentModel picturebook = new DocumentModelImpl(
                root.getPathAsString(), "picturebook", "PictureBook");
        coreSession.createDocument(picturebook);
        DocumentModel picture = new DocumentModelImpl(
                picturebook.getPathAsString(), "pic1", "Picture");
        picture.setPropertyValue("picture:views", (Serializable) createViews());
        coreSession.createDocument(picture);
        coreSession.save();

        BlobHolder blobHolder = blobHolderService.getBlobHolderAdapter(picture);
        assertNotNull(blobHolder);
        Blob blob = blobHolder.getBlob();
        assertNotNull(blob);
        blob.setFilename("exif_sample.jpg");
        ImageInfo imageInfo = imagingService.getImageInfo(blob);
        assertNotNull(imageInfo);
    }
}

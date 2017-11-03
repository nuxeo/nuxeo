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

import static org.junit.Assert.assertNotNull;

import java.net.URL;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author btatar
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core" })
public class TestImageInfo {

    protected DocumentModel root;

    @Inject
    protected BlobHolderAdapterService blobHolderService;

    @Inject
    protected ImagingService imagingService;

    @Inject
    protected CoreSession session;

    @Before
    public void init() {
        root = session.getRootDocument();
        assertNotNull(root);
    }

    @Test
    public void testGetImageInfo() {
        DocumentModel picturebook = session.createDocumentModel(root.getPathAsString(), "picturebook", "PictureBook");
        session.createDocument(picturebook);
        DocumentModel picture = session.createDocumentModel(picturebook.getPathAsString(), "pic1", "Picture");
        URL resource = getClass().getClassLoader().getResource("images/exif_sample.jpg");
        picture.setPropertyValue("file:content", new URLBlob(resource));
        session.createDocument(picture);
        session.save();

        BlobHolder blobHolder = blobHolderService.getBlobHolderAdapter(picture);
        assertNotNull(blobHolder);
        Blob blob = blobHolder.getBlob();
        assertNotNull(blob);
        blob.setFilename("exif_sample.jpg");
        ImageInfo imageInfo = imagingService.getImageInfo(blob);
        assertNotNull(imageInfo);
    }
}

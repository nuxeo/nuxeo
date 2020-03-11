/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.tag")
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

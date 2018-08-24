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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.core.convert")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.collections.core:OSGI-INF/collection-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.picture.core:OSGI-INF/imaging-listeners-override.xml")
public class TestBlobHolderSet {

    protected DocumentModel root;

    @Inject
    protected CoreSession session;

    @Before
    public void init() throws Exception {
        root = session.getRootDocument();
    }

    private List<Map<String, Serializable>> createViews() throws IOException {
        List<Map<String, Serializable>> views = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("title", "Original");
        map.put("content",
                (Serializable) Blobs.createBlob(
                        FileUtils.getResourceFileFromContext(ImagingResourcesHelper.TEST_DATA_FOLDER + "test.jpg"),
                        "image/jpeg", null, "test.jpg"));
        map.put("filename", "test.jpg");
        views.add(map);
        return views;
    }

    @Test
    public void testBlobHolderSet() throws Exception {
        DocumentModel picture = session.createDocumentModel(root.getPathAsString(), "pic", "Picture");
        picture.setPropertyValue("picture:views", (Serializable) createViews());
        picture = session.createDocument(picture);
        session.save();

        BlobHolder bh = picture.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        Blob blob = bh.getBlob();
        assertNull(blob);
        assertEquals(1, bh.getBlobs().size());

        // test write
        blob = Blobs.createBlob(
                FileUtils.getResourceFileFromContext(ImagingResourcesHelper.TEST_DATA_FOLDER + "test.jpg"),
                "image/jpeg", null, "logo.jpg");
        bh.setBlob(blob);
        session.saveDocument(picture);
        session.save();

        // reread
        bh = picture.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        assertTrue(bh instanceof DocumentBlobHolder);
        assertEquals("/pic/logo.jpg", bh.getFilePath());
        blob = bh.getBlob();
        assertEquals("logo.jpg", blob.getFilename());
        assertEquals("image/jpeg", blob.getMimeType());
        byte[] bytes = IOUtils.toByteArray(blob.getStream());
        assertEquals(2022140, bytes.length);
        bytes = null;

        // generated views
        assertEquals(6, bh.getBlobs().size());

        // test set null blob
        bh.setBlob(null);
        session.saveDocument(picture);
        session.save();
        blob = bh.getBlob();
        assertNull(blob);
    }

}

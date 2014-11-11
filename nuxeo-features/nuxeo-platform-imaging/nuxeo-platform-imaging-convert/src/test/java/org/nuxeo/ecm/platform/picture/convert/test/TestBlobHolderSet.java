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

package org.nuxeo.ecm.platform.picture.convert.test;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;

public class TestBlobHolderSet extends RepositoryOSGITestCase {

    protected DocumentModel root;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
        openRepository();
        root = getCoreSession().getRootDocument();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    private static File getFileFromPath(String path) {
        return FileUtils.getResourceFileFromContext(path);
    }

    private List<Map<String, Serializable>> createViews() {
        List<Map<String, Serializable>> views = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("title", "Original");
        map.put("content", new FileBlob(
                getFileFromPath("test-data/sample.jpeg"), "image/jpeg", null,
                "mysample.jpg", null));
        map.put("filename", "mysample.jpg");
        views.add(map);
        return views;
    }

    @Test
    public void testBlobHolderSet() throws Exception {
        DocumentModel picture = new DocumentModelImpl(root.getPathAsString(),
                "pic", "Picture");
        picture.setPropertyValue("picture:views", (Serializable) createViews());
        picture = coreSession.createDocument(picture);
        coreSession.save();

        BlobHolder bh = picture.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        Blob blob = bh.getBlob();
        assertNotNull(blob);
        assertEquals(1, bh.getBlobs().size());

        // test write
        blob = new FileBlob(getFileFromPath("test-data/big_nuxeo_logo.jpg"),
                "image/jpeg", null, "logo.jpg", null);
        bh.setBlob(blob);
        coreSession.saveDocument(picture);
        coreSession.save();

        // reread
        bh = picture.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        assertTrue(bh instanceof DocumentBlobHolder);
        assertEquals("/pic/logo.jpg", bh.getFilePath());
        blob = bh.getBlob();
        assertEquals("logo.jpg", blob.getFilename());
        assertEquals("image/jpeg", blob.getMimeType());
        byte[] bytes = FileUtils.readBytes(blob.getStream());
        assertEquals(36830, bytes.length);
        bytes = null;

        // generated views
        assertEquals(4, bh.getBlobs().size());

        // test set null blob
        bh.setBlob(null);
        coreSession.saveDocument(picture);
        coreSession.save();
        blob = bh.getBlob();
        assertNull(blob);
    }

}

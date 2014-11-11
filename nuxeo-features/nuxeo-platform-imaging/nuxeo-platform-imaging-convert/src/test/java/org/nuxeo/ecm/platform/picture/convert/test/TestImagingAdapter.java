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
package org.nuxeo.ecm.platform.picture.convert.test;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;

public class TestImagingAdapter extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
        openRepository();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    public void testAdapter() throws Exception {

        ArrayList<Map<String, Object>> pictureTemplates = new ArrayList<Map<String, Object>>();
        Map<String, Object> view = new HashMap<String, Object>();
        view.put("title", "Original");
        view.put("description", "Original Size");
        view.put("tag", "");
        view.put("maxsize", null);
        pictureTemplates.add(view);
        view = new HashMap<String, Object>();
        view.put("title", "Thumbnail");
        view.put("description", "small Size");
        view.put("tag", "");
        view.put("maxsize", new Long("100"));
        pictureTemplates.add(view);

        DocumentModel root = coreSession.getRootDocument();
        DocumentModel document1 = coreSession.createDocumentModel(
                root.getPathAsString(), "document", "Folder");
        document1.setProperty("dublincore", "description", "toto");
        document1 = coreSession.createDocument(document1);

        DocumentModel child = coreSession.createDocumentModel(
                document1.getPathAsString(), "fils" + 1, "Picture");
        child = coreSession.createDocument(child);
        child.setProperty("dublincore", "description", "fils" + 1
                + " description");
        child.setProperty("dublincore", "valid", Calendar.getInstance());

        PictureResourceAdapter adapter = child.getAdapter(PictureResourceAdapter.class);
        assertNotNull(adapter);

        for (String filename : ImagingRessourcesHelper.TEST_IMAGE_FILENAMES) {
            String path = ImagingRessourcesHelper.TEST_DATA_FOLDER + filename;
            Blob blob = new FileBlob(
                    ImagingRessourcesHelper.getFileFromPath(path));
            assertNotNull(blob);
            boolean ret = adapter.createPicture(blob, filename, "sample",
                    pictureTemplates);
            assertTrue(ret);
            child = coreSession.saveDocument(child);
            coreSession.save();
            DocumentModel documentModel = coreSession.getChildren(
                    document1.getRef()).get(0);
            MultiviewPicture multiview = documentModel.getAdapter(MultiviewPicture.class);
            assertEquals(child.getRef(), documentModel.getRef());
            assertEquals(child, documentModel);
            MultiviewPicture adaptedChild = child.getAdapter(MultiviewPicture.class);
            assertNotNull(adaptedChild);
            assertNotNull(adaptedChild.getView("Thumbnail"));
            assertNotNull(multiview.getView("Thumbnail"));
        }
    }

    public void testBlobReadOnlyOnce() throws Exception {
        DocumentModel doc = coreSession.createDocumentModel("/", "pic",
                "Picture");
        doc = coreSession.createDocument(doc);

        PictureResourceAdapter adapter = doc.getAdapter(PictureResourceAdapter.class);
        assertNotNull(adapter);

        String filename = ImagingRessourcesHelper.TEST_IMAGE_FILENAMES.get(0);
        String path = ImagingRessourcesHelper.TEST_DATA_FOLDER + filename;

        FileInputStream in = new FileInputStream(
                ImagingRessourcesHelper.getFileFromPath(path));
        try {
            // blob that can be read only once, like HttpServletRequest streams
            Blob blob = new InputStreamBlob(in);
            adapter.createPicture(blob, "foo.png", "sample", null);
            doc = coreSession.saveDocument(doc);
            MultiviewPicture pic = doc.getAdapter(MultiviewPicture.class);
            assertNotNull(pic);
            assertNotNull(pic.getView("Thumbnail"));
        } finally {
            in.close();
        }
    }

}

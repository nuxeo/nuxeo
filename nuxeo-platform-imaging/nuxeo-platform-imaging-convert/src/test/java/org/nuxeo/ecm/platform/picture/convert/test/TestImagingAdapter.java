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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class TestImagingAdapter extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
        openRepository();
    }

    private static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return file;
    }

    public void testAdapter() throws ClientException, IOException {

        ArrayList<Map<String, Object>> pictureTemplates = new ArrayList<Map<String, Object>>();
        Map view = new HashMap<String, Object>();
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

        DocumentModel fils = coreSession.createDocumentModel(
                document1.getPathAsString(), "fils" + 1, "Picture");
        fils = coreSession.createDocument(fils);
        fils.setProperty("dublincore", "description", "fils" + 1
                + " description");
        fils.setProperty("dublincore", "valid", Calendar.getInstance());

        PictureResourceAdapter adapter = fils.getAdapter(PictureResourceAdapter.class);
        assertNotNull(adapter);
        String path = "test-data/sample.jpeg";
        Blob blob = new FileBlob(getFileFromPath(path));
        assertNotNull(blob);
        boolean ret = adapter.createPicture(blob, "sample.jpeg", "sample", pictureTemplates);
        fils = coreSession.saveDocument(fils);
        coreSession.save();
        DocumentModel documentModel = coreSession.getChildren(
                document1.getRef()).get(0);
        MultiviewPicture multiview = documentModel.getAdapter(MultiviewPicture.class);
        System.err.println(fils.getRef());
        assertEquals(fils.getRef(), documentModel.getRef());
        assertEquals(fils, documentModel);
        MultiviewPicture adapterFils = fils.getAdapter(MultiviewPicture.class);
        assertNotNull(adapterFils);
        assertNotNull(adapterFils.getView(
                "Thumbnail"));
        assertNotNull(multiview.getView("Thumbnail"));
    }

}

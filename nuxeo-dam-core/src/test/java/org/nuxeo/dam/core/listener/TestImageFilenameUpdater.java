/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */
package org.nuxeo.dam.core.listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.dam.core.Constants.PICTURE_SCHEMA;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.dam.core.Constants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryJUnit4;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;

/**
 * @author btatar
 *
 */
public class TestImageFilenameUpdater extends SQLRepositoryJUnit4 {

    public TestImageFilenameUpdater() {
        super("TestImageFilenameUpdater");
    }

    @Before
    public void setUp() throws Exception {
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.video.core");
        deployBundle("org.nuxeo.ecm.platform.audio.core");
        deployBundle("org.nuxeo.dam.core");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        openSession();
    }

    @Test
    public void testListener() throws Exception {
        // Create Import set document
        DocumentModel importSet = session.createDocumentModel("/",
                "importSetTest", Constants.IMPORT_SET_TYPE);
        importSet.setPropertyValue("damc:author", "testCreator");
        Calendar cal = GregorianCalendar.getInstance();
        importSet.setPropertyValue("damc:authoringDate", cal);
        importSet.setPropertyValue("dc:description", "testDescription");
        importSet.setPropertyValue("dc:coverage", "testCoverage");
        importSet.setPropertyValue("dc:expired", cal);
        importSet = session.createDocument(importSet);
        assertNotNull(importSet);
        session.saveDocument(importSet);
        session.save();
        assertTrue(importSet.hasFacet("SuperSpace"));

        ArrayList<Map<String, Object>> pictureTemplates = new ArrayList<Map<String, Object>>();
        Map<String, Object> view = new HashMap<String, Object>();
        view.put("title", "Original");
        view.put("description", "Original Size");
        view.put("filename", "");
        view.put("tag", "");
        view.put("maxsize", null);
        pictureTemplates.add(view);
        view = new HashMap<String, Object>();
        view.put("title", "Thumbnail");
        view.put("description", "small Size");
        view.put("filename", "");
        view.put("tag", "");
        view.put("maxsize", new Long("100"));
        pictureTemplates.add(view);
        view = new HashMap<String, Object>();
        view.put("title", "Medium");
        view.put("description", "medium Size");
        view.put("filename", "");
        view.put("tag", "");
        view.put("maxsize", new Long("10"));
        pictureTemplates.add(view);

        // Create Picture document
        DocumentModel picture = session.createDocumentModel(
                importSet.getPathAsString(), "pictureTest", "Picture");
        picture.setPropertyValue("dc:title", "big nuxeo logo");
        picture = session.createDocument(picture);
        assertNotNull(picture);
        session.save();

        // Update Picture document without uploading picture
        picture = session.getChildren(importSet.getRef()).get(0);
        picture.setPropertyValue("dc:description", "big nuxeo logo description");
        session.saveDocument(picture);
        assertNotNull(picture);
        session.save();

        // Update Picture document and upload picture
        PictureResourceAdapter adapter = picture.getAdapter(PictureResourceAdapter.class);
        assertNotNull(adapter);
        Blob blob = new FileBlob(new File(
                this.getClass().getClassLoader().getResource(
                        "big_nuxeo_logo.jpg").toURI()));
        assertNotNull(blob);
        boolean ret = adapter.createPicture(blob, "big_nuxeo_logo.jpg",
                "big_nuxeo_logo.jpg", pictureTemplates);
        assertTrue(ret);

        session.saveDocument(picture);
        assertNotNull(picture);
        session.save();

        // update 'Original' filename
        picture = session.getChildren(importSet.getRef()).get(0);
        Map<String, Object> pictureMap = picture.getDataModel(PICTURE_SCHEMA).getMap();
        List<Map<String, Object>> viewsList = (List<Map<String, Object>>) pictureMap.get("views");
        viewsList.get(0).put("filename", "big_nuxeo_logo edit.jpg");
        picture.getDataModel((PICTURE_SCHEMA)).setMap(pictureMap);
        session.saveDocument(picture);
        session.save();

        picture = session.getChildren(importSet.getRef()).get(0);
        viewsList = (List<Map<String, Object>>) picture.getPropertyValue("picture:views");
        String title = (String) viewsList.get(0).get("title");
        Blob fileBlob;
        for (Map<String, Object> el : viewsList) {
            assertTrue("big_nuxeo_logo edit.jpg".equals(el.get("filename")));
            fileBlob = (Blob) el.get("content");
            assertTrue((title + "_" + "big_nuxeo_logo edit.jpg").equals(fileBlob.getFilename()));
        }
    }

    @After
    public void tearDown() throws Exception {
        closeSession(session);
    }

}

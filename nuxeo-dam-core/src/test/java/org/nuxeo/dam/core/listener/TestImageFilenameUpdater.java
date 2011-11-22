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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.dam.Constants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author btatar
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
@Deploy({ "org.nuxeo.ecm.core.api",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.video.core",
        "org.nuxeo.ecm.platform.audio.core", "org.nuxeo.dam.core",
        "org.nuxeo.ecm.core.convert.api", "org.nuxeo.ecm.core.convert",
        "org.nuxeo.ecm.platform.picture.convert",
        "org.nuxeo.ecm.platform.commandline.executor" })
public class TestImageFilenameUpdater {

    @Inject
    protected CoreSession session;

    @Before
    public void deactivateBinaryTextListener() {
        EventServiceAdmin eventServiceAdmin = Framework.getLocalService(EventServiceAdmin.class);
        eventServiceAdmin.setListenerEnabledFlag("sql-storage-binary-text",
                false);
    }

    @Test
    public void testListener() throws Exception {
        // Create Import set document
        DocumentModel importSet = session.createDocumentModel("/",
                "importSetTest", Constants.IMPORT_SET_TYPE);
        importSet.setPropertyValue(Constants.DAM_COMMON_AUTHOR_PROPERTY,
                "testCreator");
        Calendar cal = GregorianCalendar.getInstance();
        importSet.setPropertyValue(
                Constants.DAM_COMMON_AUTHORING_DATE_PROPERTY, cal);
        importSet.setPropertyValue(Constants.DUBLINCORE_DESCRIPTION_PROPERTY,
                "testDescription");
        importSet.setPropertyValue(Constants.DUBLINCORE_COVERAGE_PROPERTY,
                "testCoverage");
        importSet.setPropertyValue(Constants.DUBLINCORE_EXPIRED_PROPERTY, cal);
        importSet = session.createDocument(importSet);
        assertNotNull(importSet);
        session.saveDocument(importSet);
        session.save();

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
        picture.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY,
                "big nuxeo logo");
        picture = session.createDocument(picture);
        assertNotNull(picture);
        session.save();

        // Update Picture document without uploading picture
        picture = session.getChildren(importSet.getRef()).get(0);
        picture.setPropertyValue(Constants.DUBLINCORE_DESCRIPTION_PROPERTY,
                "big nuxeo logo description");
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
        Map<String, Object> pictureMap = picture.getDataModel(
                Constants.PICTURE_SCHEMA).getMap();
        List<Map<String, Object>> viewsList = (List<Map<String, Object>>) pictureMap.get("views");
        viewsList.get(0).put("filename", "big_nuxeo_logo edit.jpg");
        picture.getDataModel(Constants.PICTURE_SCHEMA).setMap(pictureMap);
        session.saveDocument(picture);
        session.save();

        picture = session.getChildren(importSet.getRef()).get(0);
        viewsList = (List<Map<String, Object>>) picture.getPropertyValue("picture:views");
        String title = (String) viewsList.get(0).get("title");
        for (Map<String, Object> el : viewsList) {
            assertEquals("big_nuxeo_logo edit.jpg", el.get("filename"));
            Blob fileBlob = (Blob) el.get("content");
            assertEquals((title + "_" + "big_nuxeo_logo edit.jpg"),
                    fileBlob.getFilename());
        }
    }

}

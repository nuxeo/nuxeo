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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.service.test;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.adapter.PictureTilesAdapter;

public class TestAdapters extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployContrib("org.nuxeo.ecm.platform.pictures.tiles",
                "OSGI-INF/pictures-tiles-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.pictures.tiles",
                "OSGI-INF/pictures-tiles-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.pictures.tiles",
                "OSGI-INF/pictures-tiles-adapter-contrib.xml");
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    public void testAdapter() throws Exception {

        DocumentModel root = session.getRootDocument();

        DocumentModel doc = session.createDocumentModel(root.getPathAsString(),
                "file", "File");
        doc.setProperty("dublincore", "title", "MyDoc");
        doc.setProperty("dublincore", "coverage", "MyDocCoverage");
        doc.setProperty("dublincore", "modified", new GregorianCalendar());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);
        doc.setProperty("file", "content", image);
        doc.setProperty("file", "filename", "test.jpg");

        doc = session.createDocument(doc);
        session.save();

        assertTilingIsWorkingFor(doc);
    }

    protected void assertTilingIsWorkingFor(DocumentModel doc) throws Exception {
        PictureTilesAdapter tilesAdapter = doc.getAdapter(PictureTilesAdapter.class);
        assertNotNull(tilesAdapter);

        PictureTiles tiles = tilesAdapter.getTiles(255, 255, 15);
        assertNotNull(tiles);

        PictureTiles tiles2 = tilesAdapter.getTiles(255, 255, 20);
        assertNotNull(tiles2);
    }

    public void testAdapterOnPicture() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel picture = session.createDocumentModel(
                root.getPathAsString(), "picture1", "Picture");
        picture.setProperty("dublincore", "description", "test picture");
        picture.setProperty("dublincore", "modified", new GregorianCalendar());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);

        List<Map<String, Object>> viewsList = getDefaultViewsList(image);
        picture.getProperty("picture:views").setValue(viewsList);
        picture = session.createDocument(picture);
        session.save();

        assertTilingIsWorkingFor(picture);
    }

    public void testAdapterOnPictureWithOriginalJpegView() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel picture = session.createDocumentModel(
                root.getPathAsString(), "picture1", "Picture");
        picture.setProperty("dublincore", "description", "test picture");
        picture.setProperty("dublincore", "modified", new GregorianCalendar());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = new FileBlob(file);

        List<Map<String, Object>> viewsList = getDefaultViewsList(image);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("title", "OriginalJpeg");
        map.put("description", "OriginalJpeg Size");
        map.put("filename", "test.jpg");
        map.put("tag", "originalJpeg");
        map.put("width", 3872);
        map.put("height", 2592);
        image.setFilename("OriginalJpeg" + "_" + "test.jpg");
        map.put("content", image);
        viewsList.add(map);

        picture.getProperty("picture:views").setValue(viewsList);
        picture = session.createDocument(picture);
        session.save();

        assertTilingIsWorkingFor(picture);
    }

    protected List<Map<String, Object>> getDefaultViewsList(Blob image) {
        List<Map<String, Object>> viewsList = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("title", "Medium");
        map.put("description", "Medium Size");
        map.put("filename", "test.jpg");
        map.put("tag", "medium");
        map.put("width", 3872);
        map.put("height", 2592);
        image.setFilename("Medium" + "_" + "test.jpg");
        map.put("content", image);
        viewsList.add(map);
        map = new HashMap<String, Object>();
        map.put("title", "Original");
        map.put("description", "Original Size");
        map.put("filename", "test.jpg");
        map.put("tag", "original");
        map.put("width", 3872);
        map.put("height", 2592);
        image.setFilename("Original" + "_" + "test.jpg");
        map.put("content", image);
        viewsList.add(map);
        map = new HashMap<String, Object>();
        map.put("title", "Thumbnail");
        map.put("description", "Thumbnail Size");
        map.put("filename", "test.jpg");
        map.put("tag", "thumbnail");
        map.put("width", 3872);
        map.put("height", 2592);
        image.setFilename("Thumbnail" + "_" + "test.jpg");
        map.put("content", image);
        viewsList.add(map);

        return viewsList;
    }

}

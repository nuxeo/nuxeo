/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.pictures.tiles.service.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.adapter.PictureTilesAdapter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

// NXP-13240: temporarily disabled
@Ignore
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.types.api", //
        "org.nuxeo.ecm.platform.commandline.executor", //
        "org.nuxeo.ecm.platform.picture.api", //
        "org.nuxeo.ecm.platform.picture.core", //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.pictures.tiles:OSGI-INF/pictures-tiles-framework.xml",
        "org.nuxeo.ecm.platform.pictures.tiles:OSGI-INF/pictures-tiles-contrib.xml",
        "org.nuxeo.ecm.platform.pictures.tiles:OSGI-INF/pictures-tiles-adapter-contrib.xml" })
public class TestAdapters {

    @Inject
    protected CoreSession session;

    @Test
    public void testAdapter() throws Exception {

        DocumentModel root = session.getRootDocument();

        DocumentModel doc = session.createDocumentModel(root.getPathAsString(), "file", "File");
        doc.setProperty("dublincore", "title", "MyDoc");
        doc.setProperty("dublincore", "coverage", "MyDocCoverage");
        doc.setProperty("dublincore", "modified", new GregorianCalendar());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);
        doc.setProperty("file", "content", image);

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

    @Test
    public void testAdapterOnPicture() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel picture = session.createDocumentModel(root.getPathAsString(), "picture1", "Picture");
        picture.setProperty("dublincore", "description", "test picture");
        picture.setProperty("dublincore", "modified", new GregorianCalendar());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);

        List<Map<String, Object>> viewsList = getDefaultViewsList(image);
        picture.getProperty("picture:views").setValue(viewsList);
        picture = session.createDocument(picture);
        session.save();

        assertTilingIsWorkingFor(picture);
    }

    @Test
    public void testAdapterOnPictureWithOriginalJpegView() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel picture = session.createDocumentModel(root.getPathAsString(), "picture1", "Picture");
        picture.setProperty("dublincore", "description", "test picture");
        picture.setProperty("dublincore", "modified", new GregorianCalendar());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);

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

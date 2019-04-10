/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.video.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

/*
 * Tests that the VideoImporter class works by importing a sample video
 */
public class TestVideoImporterAndListeners extends SQLRepositoryTestCase {

    // http://www.elephantsdream.org/
    public static final String ELEPHANTS_DREAM = "elephantsdream-160-mpeg4-su-ac3.avi";

    public static final Log log = LogFactory.getLog(TestVideoImporterAndListeners.class);

    protected static final String VIDEO_TYPE = "Video";

    protected FileManager fileManagerService;

    protected DocumentModel root;

    private File getTestFile() {
        return new File(
                FileUtils.getResourcePathFromContext("test-data/sample.mpg"));
    }

    protected static BlobHolder getBlobFromPath(String path) throws IOException {
        InputStream is = TestVideoImporterAndListeners.class.getResourceAsStream("/"
                + path);
        assertNotNull(String.format("Failed to load resource: " + path), is);
        Blob blob = StreamingBlob.createFromStream(is, path).persist();
        blob.setFilename(path);
        return new SimpleBlobHolder(blob);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
        deployBundle("org.nuxeo.ecm.platform.video.convert");
        deployBundle("org.nuxeo.ecm.platform.video.core");

        // use these to get the fileManagerService
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");

        openSession();

        root = session.getRootDocument();
        fileManagerService = Framework.getService(FileManager.class);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        fileManagerService = null;
        root = null;
    }

    public void testVideoType() throws ClientException {

        DocumentType videoType = session.getDocumentType(VIDEO_TYPE);
        assertNotNull("Does our type exist?", videoType);

        // TODO: check get/set properties on common properties of videos

        // Create a new DocumentModel of our type in memory
        DocumentModel docModel = session.createDocumentModel("/", "doc",
                VIDEO_TYPE);
        assertNotNull(docModel);

        assertNull(docModel.getPropertyValue("common:icon"));
        assertNull(docModel.getPropertyValue("dc:title"));
        assertNull(docModel.getPropertyValue("picture:credit"));
        assertNull(docModel.getPropertyValue("uid:uid"));
        assertNull(docModel.getPropertyValue("strm:duration"));

        docModel.setPropertyValue("common:icon", "/icons/video.png");
        docModel.setPropertyValue("dc:title", "testTitle");
        docModel.setPropertyValue("picture:credit", "testUser");
        docModel.setPropertyValue("uid:uid", "testUid");
        docModel.setPropertyValue("strm:duration", 133);

        DocumentModel docModelResult = session.createDocument(docModel);
        assertNotNull(docModelResult);

        assertEquals("/icons/video.png",
                docModelResult.getPropertyValue("common:icon"));
        assertEquals("testTitle", docModelResult.getPropertyValue("dc:title"));
        assertEquals("testUser",
                docModelResult.getPropertyValue("picture:credit"));
        assertEquals("testUid", docModelResult.getPropertyValue("uid:uid"));
        assertEquals("133.0",
                docModelResult.getPropertyValue("strm:duration").toString());

    }

    @SuppressWarnings("unchecked")
    public void testImportSmallVideo() throws Exception {

        File testFile = getTestFile();
        Blob blob = StreamingBlob.createFromFile(testFile, "video/mpg");
        blob.setFilename("Test file.mov");
        String rootPath = root.getPathAsString();
        assertNotNull(blob);
        assertNotNull(rootPath);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        DocumentModel docModel = fileManagerService.createDocumentFromBlob(
                session, blob, rootPath, true, "test-data/sample.mpg");

        assertNotNull(docModel);
        DocumentRef ref = docModel.getRef();
        session.save();

        closeSession();
        openSession();

        docModel = session.getDocument(ref);
        assertEquals("Video", docModel.getType());
        assertEquals("sample", docModel.getTitle());

        assertNotNull(docModel.getProperty("file:content"));
        assertEquals("sample.mpg", docModel.getPropertyValue("file:filename"));

        CommandAvailability ca = Framework.getService(
                CommandLineExecutorService.class).getCommandAvailability(
                "ffmpeg-storyboard");
        if (!ca.isAvailable()) {
            log.warn("ffmpeg is not avalaible, skipping the end of the test");
            return;
        }

        // the test video is very short, no storyboard:
        assertEquals(0.0, docModel.getPropertyValue("strm:duration"));
        List<Map<String, Serializable>> storyboard = docModel.getProperty(
                "vid:storyboard").getValue(List.class);
        assertNotNull(storyboard);
        assertEquals(0, storyboard.size());

    }

    public void testImportBigVideo() throws Exception {

        CommandAvailability ca = Framework.getService(
                CommandLineExecutorService.class).getCommandAvailability(
                "ffmpeg-storyboard");
        if (!ca.isAvailable()) {
            log.warn("ffmpeg is not avalaible, skipping the end of the test");
            return;
        }
        DocumentModel docModel = session.createDocumentModel("/", "doc",
                VIDEO_TYPE);
        assertNotNull(docModel);
        docModel.setPropertyValue("file:content",
                (Serializable) getBlobFromPath(ELEPHANTS_DREAM).getBlob());
        docModel = session.createDocument(docModel);

        // the test video last around 10 minutes
        assertEquals(653.53, docModel.getPropertyValue("strm:duration"));
        List<Map<String, Serializable>> storyboard = docModel.getProperty(
                "vid:storyboard").getValue(List.class);
        assertNotNull(storyboard);
        assertEquals(9, storyboard.size());

        assertEquals(0.0, storyboard.get(0).get("timecode"));
        assertEquals("elephantsdream-160-mpeg4-su-ac3.avi 1",
                storyboard.get(0).get("comment"));
        Blob thumb0 = (Blob) storyboard.get(0).get("content");
        assertEquals("00000.000-seconds.jpeg", thumb0.getFilename());

        assertEquals(70.0, storyboard.get(1).get("timecode"));
        assertEquals("elephantsdream-160-mpeg4-su-ac3.avi 2",
                storyboard.get(1).get("comment"));
        Blob thumb1 = (Blob) storyboard.get(1).get("content");
        assertEquals("00070.000-seconds.jpeg", thumb1.getFilename());

        // check that the thumbnails where extracted
        assertEquals("Thumbnail",
                docModel.getPropertyValue("picture:views/0/title"));
        assertEquals(62L, docModel.getPropertyValue("picture:views/0/height"));
        assertEquals(100L, docModel.getPropertyValue("picture:views/0/width"));
        assertTrue((Long) docModel.getPropertyValue("picture:views/0/content/length") > 1000);

        // the original video is also 100 pixels high hence the player preview
        // has the same size
        assertEquals("StaticPlayerView",
                docModel.getPropertyValue("picture:views/1/title"));
        assertEquals(100L, docModel.getPropertyValue("picture:views/1/height"));
        assertEquals(160L, docModel.getPropertyValue("picture:views/1/width"));
        assertTrue((Long) docModel.getPropertyValue("picture:views/1/content/length") > 1000);

        // TODO: add picture metadata extraction where if
        // they make sense for videos (ie. extract these from the
        // metadata already included in the video and use them to
        // set the appropriate schema properties)
        assertNull("", docModel.getPropertyValue("picture:credit"));

        // check that the update with a null video removes the previews and
        // storyboard
        docModel.setPropertyValue("file:content", null);
        docModel = session.saveDocument(docModel);

        assertTrue(docModel.getProperty("vid:storyboard").getValue(List.class).isEmpty());
        assertTrue(docModel.getProperty("picture:views").getValue(List.class).isEmpty());
    }

}

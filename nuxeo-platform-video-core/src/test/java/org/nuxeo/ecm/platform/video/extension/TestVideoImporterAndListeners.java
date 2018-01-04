/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.video.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.video.VideoConstants.DURATION_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.video.Stream;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests that the VideoImporter class works by importing a sample video
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", //
        "org.nuxeo.ecm.platform.types.api", //
        "org.nuxeo.ecm.platform.types.core", //
        "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.platform.picture.core", //
        "org.nuxeo.ecm.platform.picture.api", //
        "org.nuxeo.ecm.platform.picture.convert", //
        "org.nuxeo.ecm.platform.video.convert", //
        "org.nuxeo.ecm.platform.video.core", //
        "org.nuxeo.ecm.platform.filemanager.api", //
        "org.nuxeo.ecm.platform.filemanager.core", //
        "org.nuxeo.ecm.platform.tag" })
public class TestVideoImporterAndListeners {

    // http://www.elephantsdream.org/
    public static final String ELEPHANTS_DREAM = "elephantsdream-160-mpeg4-su-ac3.avi";

    protected static final String VIDEO_TYPE = "Video";

    public static final Log log = LogFactory.getLog(TestVideoImporterAndListeners.class);

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Inject
    protected FileManager fileManagerService;

    @Inject
    protected CommandLineExecutorService cles;

    @Inject
    protected EventService eventService;

    private File getTestFile() {
        return new File(FileUtils.getResourcePathFromContext("test-data/sample.mpg"));
    }

    protected static BlobHolder getBlobFromPath(String path) throws IOException {
        Blob blob;
        try (InputStream in = TestVideoImporterAndListeners.class.getResourceAsStream("/" + path)) {
            assertNotNull(String.format("Failed to load resource: " + path), in);
            blob = Blobs.createBlob(in);
        }
        blob.setFilename(path);
        return new SimpleBlobHolder(blob);
    }

    @Before
    public void setUp() throws Exception {
        eventServiceAdmin.setListenerEnabledFlag("videoAutomaticConversions", false);
        eventServiceAdmin.setListenerEnabledFlag("sql-storage-binary-text", false);
    }

    protected void waitForAsyncCompletion() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        eventService.waitForAsyncCompletion();
    }

    @Test
    public void testVideoType() {
        DocumentType videoType = session.getDocumentType(VIDEO_TYPE);
        assertNotNull("Does our type exist?", videoType);

        // TODO: check get/set properties on common properties of videos

        // Create a new DocumentModel of our type in memory
        DocumentModel docModel = session.createDocumentModel("/", "doc", VIDEO_TYPE);
        assertNotNull(docModel);

        assertNull(docModel.getPropertyValue("common:icon"));
        assertNull(docModel.getPropertyValue("dc:title"));
        assertNull(docModel.getPropertyValue("picture:credit"));
        assertNull(docModel.getPropertyValue("uid:uid"));
        assertNull(docModel.getPropertyValue(DURATION_PROPERTY));

        docModel.setPropertyValue("common:icon", "/icons/video.png");
        docModel.setPropertyValue("dc:title", "testTitle");
        docModel.setPropertyValue("picture:credit", "testUser");
        docModel.setPropertyValue("uid:uid", "testUid");

        DocumentModel docModelResult = session.createDocument(docModel);
        assertNotNull(docModelResult);

        assertEquals("/icons/video.png", docModelResult.getPropertyValue("common:icon"));
        assertEquals("testTitle", docModelResult.getPropertyValue("dc:title"));
        assertEquals("testUser", docModelResult.getPropertyValue("picture:credit"));
        assertEquals("testUid", docModelResult.getPropertyValue("uid:uid"));
        assertEquals("0.0", docModelResult.getPropertyValue(DURATION_PROPERTY).toString());
    }

    @Test
    public void testImportSmallVideo() throws Exception {
        File testFile = getTestFile();
        Blob blob = Blobs.createBlob(testFile, "video/mpg");
        blob.setFilename("Test file.mov");
        assertNotNull(blob);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        DocumentModel docModel = fileManagerService.createDocumentFromBlob(session, blob, "/", true,
                "test-data/sample.mpg");

        assertNotNull(docModel);
        DocumentRef ref = docModel.getRef();
        session.save();

        // reopen session
        session = coreFeature.reopenCoreSession();

        docModel = session.getDocument(ref);
        assertEquals("Video", docModel.getType());
        assertEquals("sample.mpg", docModel.getTitle());

        assertNotNull(docModel.getProperty("file:content"));
        assertEquals("sample.mpg", docModel.getPropertyValue("file:content/name"));

        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot");
        Assume.assumeTrue("ffmpeg-screenshot is not available, skipping test", ca.isAvailable());

        waitForAsyncCompletion();

        // the test video is very short, no storyboard:
        Serializable duration = docModel.getPropertyValue(DURATION_PROPERTY);
        if (!Double.valueOf(0.05).equals(duration)) { // ffmpeg 2.2.1
            assertEquals(0.04, duration);
        }
        List<Map<String, Serializable>> storyboard = docModel.getProperty("vid:storyboard").getValue(List.class);
        assertNotNull(storyboard);
        assertEquals(0, storyboard.size());

    }

    @Test
    public void testImportBigVideo() throws Exception {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot");
        Assume.assumeTrue("ffmpeg-screenshot is not available, skipping test", ca.isAvailable());
        DocumentModel docModel = session.createDocumentModel("/", "doc", VIDEO_TYPE);
        assertNotNull(docModel);
        docModel.setPropertyValue("file:content", (Serializable) getBlobFromPath(ELEPHANTS_DREAM).getBlob());
        docModel = session.createDocument(docModel);
        session.save();

        waitForAsyncCompletion();

        docModel = session.getDocument(docModel.getRef());
        // the test video last around 10 minutes
        Serializable duration = docModel.getPropertyValue(DURATION_PROPERTY);
        if (!Double.valueOf(653.81).equals(duration)) { // ffmpeg 2.2.1
            assertEquals(653.8, duration);
        }
        List<Map<String, Serializable>> storyboard = docModel.getProperty("vid:storyboard").getValue(List.class);
        assertNotNull(storyboard);
        assertEquals(9, storyboard.size());

        assertEquals(0.0, storyboard.get(0).get("timecode"));
        assertEquals("elephantsdream-160-mpeg4-su-ac3.avi 0", storyboard.get(0).get("comment"));
        Blob thumb0 = (Blob) storyboard.get(0).get("content");
        assertEquals("0.00-seconds.jpeg", thumb0.getFilename());

        Blob thumb1 = (Blob) storyboard.get(1).get("content");
        Serializable timecode = storyboard.get(1).get("timecode");
        if (!Double.valueOf(72.65).equals(timecode)) {
            assertEquals(72.64, timecode);
            assertEquals("72.64-seconds.jpeg", thumb1.getFilename());
        }
        assertEquals("elephantsdream-160-mpeg4-su-ac3.avi 1", storyboard.get(1).get("comment"));

        // check that the thumbnails where extracted
        assertEquals("Small", docModel.getPropertyValue("picture:views/0/title"));
        assertEquals(100L, docModel.getPropertyValue("picture:views/0/height"));
        assertEquals(160L, docModel.getPropertyValue("picture:views/0/width"));
        assertTrue((Long) docModel.getPropertyValue("picture:views/0/content/length") > 1000);

        // the original video is also 100 pixels high hence the player preview
        // has the same size
        assertEquals("StaticPlayerView", docModel.getPropertyValue("picture:views/1/title"));
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

        session.save();
        waitForAsyncCompletion();

        docModel = session.getDocument(docModel.getRef());

        assertTrue(docModel.getProperty("vid:storyboard").getValue(List.class).isEmpty());
        assertTrue(docModel.getProperty("picture:views").getValue(List.class).isEmpty());
    }

    @Test
    public void testVideoInfo() throws Exception {
        File testFile = getTestFile();
        Blob blob = Blobs.createBlob(testFile, "video/mpg");
        blob.setFilename("Sample.mpg");
        String rootPath = "/";
        assertNotNull(blob);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        DocumentModel docModel = fileManagerService.createDocumentFromBlob(session, blob, rootPath, true,
                "test-data/sample.mpg");
        session.save();

        // reopen session
        session = coreFeature.reopenCoreSession();

        docModel = session.getDocument(docModel.getRef());
        assertEquals("Video", docModel.getType());
        assertEquals("sample.mpg", docModel.getTitle());

        VideoDocument videoDocument = docModel.getAdapter(VideoDocument.class);
        assertNotNull(videoDocument);

        Video video = videoDocument.getVideo();
        assertNotNull(video);
        assertEquals("mpegvideo", video.getFormat());
        assertEquals(0.04, video.getDuration(), 0.1);
        assertEquals(23.98, video.getFrameRate(), 0.1);
        assertEquals(320, video.getWidth());
        assertEquals(200, video.getHeight());

        List<Stream> streams = video.getStreams();
        assertNotNull(streams);
        assertEquals(1, streams.size());
        Stream stream = streams.get(0);
        assertEquals(Stream.VIDEO_TYPE, stream.getType());
        assertEquals("mpeg1video", stream.getCodec());
        assertEquals(104857, stream.getBitRate(), 0.1);
        String streamInfo = stream.getStreamInfo();
        // assert that the stream info contains common info, to avoid strict
        // equals
        assertTrue(streamInfo.contains("Video: mpeg1video"));
        assertTrue(streamInfo.contains("320x200"));
        assertTrue(streamInfo.contains("23.98 fps"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.video.core:video-configuration-override.xml")
    public void testConfiguration() throws Exception {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot");
        Assume.assumeTrue("ffmpeg-screenshot is not available, skipping test", ca.isAvailable());

        DocumentModel docModel = session.createDocumentModel("/", "doc", VIDEO_TYPE);
        assertNotNull(docModel);
        docModel.setPropertyValue("file:content", (Serializable) getBlobFromPath("test-data/sample.mpg").getBlob());
        docModel = session.createDocument(docModel);
        session.save();

        waitForAsyncCompletion();

        docModel = session.getDocument(docModel.getRef());

        List<Map<String, Serializable>> storyboard = docModel.getProperty("vid:storyboard").getValue(List.class);
        assertNotNull(storyboard);
        assertEquals(2, storyboard.size());
    }

}

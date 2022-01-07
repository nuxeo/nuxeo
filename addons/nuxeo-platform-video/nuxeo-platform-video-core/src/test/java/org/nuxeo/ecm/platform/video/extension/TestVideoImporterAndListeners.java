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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.video.VideoConstants.DURATION_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.STORYBOARD_PROPERTY;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.video.Stream;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.ecm.platform.video.listener.VideoChangedListener;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests that the VideoImporter class works by importing a sample video and that the VideoChangedListener schedules the
 * different works to update the Video document info, storyboard, previews and conversions.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.video.convert")
@Deploy("org.nuxeo.ecm.platform.video.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.api")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.tag")
// contribution to deactivate the unwanted works: fulltextExtractor, fulltextUpdater and videoConversion
@Deploy("org.nuxeo.ecm.platform.video.core:test-video-workmanager-config.xml")
public class TestVideoImporterAndListeners {

    // http://www.elephantsdream.org/
    public static final String ELEPHANTS_DREAM = "elephantsdream-160-mpeg4-su-ac3.avi";

    protected static final String VIDEO_TYPE = "Video";

    @Inject
    protected CoreSession session;

    @Inject
    protected FileManager fileManagerService;

    @Inject
    protected CommandLineExecutorService cles;

    @Inject
    protected TransactionalFeature txFeature;

    protected static Blob getBlobFromPath(String path, String mimeType) throws IOException {
        try (InputStream in = TestVideoImporterAndListeners.class.getResourceAsStream("/" + path)) {
            Blob blob = Blobs.createBlob(in, mimeType);
            blob.setFilename(FilenameUtils.getName(path));
            return blob;
        }
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

        docModelResult = session.getDocument(docModelResult.getRef());

        // no video blob so expecting null/empty values for info, storyboard and conversions
        assertNull(docModelResult.getPropertyValue(DURATION_PROPERTY));
        assertEquals(Collections.emptyList(), docModel.getPropertyValue("vid:storyboard"));
        assertEquals(Collections.emptyList(), docModel.getPropertyValue("vid:transcodedVideos"));
    }

    @Test
    public void testImportSmallVideo() throws Exception {
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        assertNotNull(blob);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        FileImporterContext context = FileImporterContext.builder(session, blob, "/")
                                                         .overwrite(true)
                                                         .build();
        DocumentModel docModel = fileManagerService.createOrUpdateDocument(context);
        assertNotNull(docModel);

        txFeature.nextTransaction();
        docModel = session.getDocument(docModel.getRef());

        assertEquals("Video", docModel.getType());
        assertEquals("sample.mpg", docModel.getTitle());

        assertNotNull(docModel.getProperty("file:content"));
        assertEquals("sample.mpg", docModel.getPropertyValue("file:content/name"));

        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot");
        Assume.assumeTrue("ffmpeg-screenshot is not available, skipping test", ca.isAvailable());

        txFeature.nextTransaction();

        // the test video is very short, no storyboard:
        Serializable duration = docModel.getPropertyValue(DURATION_PROPERTY);
        if (!Double.valueOf(0.05).equals(duration)) { // ffmpeg 2.2.1
            assertEquals(0.04, duration);
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> storyboard = (List<Map<String, Serializable>>) docModel.getPropertyValue(
                "vid:storyboard");
        assertNotNull(storyboard);
        assertEquals(0, storyboard.size());

    }

    @Test
    public void testImportBigVideo() throws Exception {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot");
        Assume.assumeTrue("ffmpeg-screenshot is not available, skipping test", ca.isAvailable());
        DocumentModel docModel = session.createDocumentModel("/", "doc", VIDEO_TYPE);
        assertNotNull(docModel);
        docModel.setPropertyValue("file:content", (Serializable) getBlobFromPath(ELEPHANTS_DREAM, "video/mp4"));
        docModel = session.createDocument(docModel);

        txFeature.nextTransaction();
        docModel = session.getDocument(docModel.getRef());

        // the test video last around 10 minutes
        Serializable duration = docModel.getPropertyValue(DURATION_PROPERTY);
        if (!Double.valueOf(653.81).equals(duration)) { // ffmpeg 2.2.1
            assertEquals(653.8, duration);
        }
        // check storyboard
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> storyboard = (List<Map<String, Serializable>>) docModel.getPropertyValue(
                "vid:storyboard");
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

        txFeature.nextTransaction();

        docModel = session.getDocument(docModel.getRef());

        assertEquals(Collections.emptyList(), docModel.getPropertyValue("vid:storyboard"));
        assertEquals(Collections.emptyList(), docModel.getPropertyValue("picture:views"));
    }

    @Test
    public void testVideoInfo() throws Exception {
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        String rootPath = "/";
        assertNotNull(blob);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        FileImporterContext context = FileImporterContext.builder(session, blob, rootPath)
                                                         .overwrite(true)
                                                         .build();
        DocumentModel docModel = fileManagerService.createOrUpdateDocument(context);

        txFeature.nextTransaction();
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
    @SuppressWarnings("unchecked")
    public void testVideoUpdate() throws Exception {
        // create a Video document without any video blob
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VIDEO_TYPE);
        doc = session.createDocument(doc);

        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());

        // no video blob so expecting zero/empty values for info, storyboard, previews and conversions
        Double duration = (Double) doc.getPropertyValue(DURATION_PROPERTY);
        assertEquals(0.0, duration, 0.0);
        List<Map<String, Serializable>> storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue(
                "vid:storyboard");
        assertEquals(Collections.emptyList(), storyboard);
        List<Map<String, Serializable>> previews = (List<Map<String, Serializable>>) doc.getPropertyValue(
                "picture:views");
        assertEquals(Collections.emptyList(), previews);
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(
                "vid:transcodedVideos");
        assertEquals(Collections.emptyList(), transcodedVideos);

        // update document with a video blob
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.saveDocument(doc);

        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());

        // expecting info, storyboard and previews but no conversions since they are deactivated for the tests
        duration = (Double) doc.getPropertyValue(DURATION_PROPERTY);
        assertTrue(duration > 0.0);
        storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:storyboard");
        assertNotEquals(Collections.emptyList(), storyboard);
        previews = (List<Map<String, Serializable>>) doc.getPropertyValue("picture:views");
        assertNotEquals(Collections.emptyList(), previews);
        transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:transcodedVideos");
        assertEquals(Collections.emptyList(), transcodedVideos);

        // update document with a different video blob
        blob = Blobs.createBlob(new File(FileUtils.getResourcePathFromContext("test-data/ccdemo.mov")),
                "video/quicktime");
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.saveDocument(doc);

        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());

        // expecting different info, storyboard and previews
        Double updatedDuration = (Double) doc.getPropertyValue(DURATION_PROPERTY);
        assertNotEquals(duration, updatedDuration);
        List<Map<String, Serializable>> updatedStoryboard = (List<Map<String, Serializable>>) doc.getPropertyValue(
                "vid:storyboard");
        assertNotEquals(Collections.emptyList(), updatedStoryboard);
        assertNotEquals(((Blob) storyboard.get(0).get("content")).getLength(),
                ((Blob) updatedStoryboard.get(0).get("content")).getLength());
        List<Map<String, Serializable>> updatedPreviews = (List<Map<String, Serializable>>) doc.getPropertyValue(
                "picture:views");
        assertNotEquals(Collections.emptyList(), updatedPreviews);
        assertNotEquals(((Blob) previews.get(0).get("content")).getLength(),
                ((Blob) updatedPreviews.get(0).get("content")).getLength());

        // remove video blob from document
        doc.setPropertyValue("file:content", null);
        session.saveDocument(doc);

        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());

        // no video blob so expecting zero/empty values for info, storyboard and previews
        updatedDuration = (Double) doc.getPropertyValue(DURATION_PROPERTY);
        assertEquals(0.0, updatedDuration, 0.0);
        updatedStoryboard = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:storyboard");
        assertEquals(Collections.emptyList(), updatedStoryboard);
        updatedPreviews = (List<Map<String, Serializable>>) doc.getPropertyValue("picture:views");
        assertEquals(Collections.emptyList(), updatedPreviews);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.video.core:test-video-conversions-enabled.xml")
    @Deploy("org.nuxeo.ecm.platform.video.core:test-video-listeners-contrib.xml")
    @SuppressWarnings("unchecked")
    public void testVideoConversions() throws IOException, InterruptedException {
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VIDEO_TYPE);
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // Init the KV and the counter used by the conversion done listener
        KeyValueStore kv = Framework.getService(KeyValueService.class).getKeyValueStore("default");
        kv.put(doc.getId(), 0L);

        txFeature.nextTransaction();

        // assert that conversion done event is fired only once
        assertEquals(Long.valueOf(1), kv.getLong(doc.getId()));

        doc = session.getDocument(doc.getRef());

        // expecting video conversions since they are activated for this test
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(
                "vid:transcodedVideos");
        assertEquals(2, transcodedVideos.size());
        Set<String> transcodedNames = transcodedVideos.stream().map(v -> (String) v.get("name")).collect(Collectors.toSet());
        assertTrue(transcodedNames.toString(), transcodedNames.contains("MP4 480p"));
        assertTrue(transcodedNames.toString(), transcodedNames.contains("WebM 480p"));
        assertTrue(((Blob) transcodedVideos.get(0).get("content")).getLength() > 0);
        assertTrue(((Blob) transcodedVideos.get(1).get("content")).getLength() > 0);

        // launching conversions on the same video shouldn't store duplicated transcoded videos
        VideoService videoService = Framework.getService(VideoService.class);
        videoService.launchAutomaticConversions(doc);

        txFeature.nextTransaction();

        // assert that conversion done event is fired only once
        assertEquals(Long.valueOf(2), kv.getLong(doc.getId()));

        doc = session.getDocument(doc.getRef());

        transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:transcodedVideos");
        assertEquals(2, transcodedVideos.size());
        transcodedNames = transcodedVideos.stream().map(v -> (String) v.get("name")).collect(Collectors.toSet());
        assertTrue(transcodedNames.toString(), transcodedNames.contains("MP4 480p"));
        assertTrue(transcodedNames.toString(), transcodedNames.contains("WebM 480p"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.video.core:video-configuration-override.xml")
    public void testConfiguration() throws Exception {
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-screenshot");
        Assume.assumeTrue("ffmpeg-screenshot is not available, skipping test", ca.isAvailable());

        DocumentModel docModel = session.createDocumentModel("/", "doc", VIDEO_TYPE);
        assertNotNull(docModel);
        docModel.setPropertyValue("file:content", (Serializable) getBlobFromPath("test-data/sample.mpg", "video/mpeg"));
        docModel = session.createDocument(docModel);

        txFeature.nextTransaction();
        docModel = session.getDocument(docModel.getRef());

        // check storyboard
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> storyboard = (List<Map<String, Serializable>>) docModel.getPropertyValue(
                "vid:storyboard");
        assertNotNull(storyboard);
        assertEquals(2, storyboard.size());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.video.core:test-video-conversions-enabled.xml")
    @Deploy("org.nuxeo.ecm.platform.video.core:test-video-listeners-contrib.xml")
    @Deploy("org.nuxeo.ecm.platform.video.core:video-configuration-override.xml")
    @SuppressWarnings("unchecked")
    public void testNoVideoConversionWhenDisabled() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VideoConstants.VIDEO_TYPE);
        Blob blobContent = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blobContent);
        // disable the conversions/storyboard
        doc.putContextData(VideoChangedListener.DISABLE_VIDEO_CONVERSIONS_GENERATION_LISTENER, true);
        doc = session.createDocument(doc);

        KeyValueStore kvs = Framework.getService(KeyValueService.class).getKeyValueStore("default");
        kvs.put(doc.getId(), 0L);
        txFeature.nextTransaction();

        // no conversion is done because we have disabled them
        assertEquals(Long.valueOf(0), kvs.getLong(doc.getId()));
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(
                TRANSCODED_VIDEOS_PROPERTY);
        assertEquals(0, transcodedVideos.size());
        List<Map<String, Serializable>> storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue(STORYBOARD_PROPERTY);
        assertEquals(0, storyboard.size());

        // re-enable the conversions/storyboard
        doc.putContextData(VideoChangedListener.DISABLE_VIDEO_CONVERSIONS_GENERATION_LISTENER, false);
        doc.setPropertyValue("file:content", (Serializable) blobContent);
        doc = session.saveDocument(doc);
        txFeature.nextTransaction();

        // ensure that the conversions are done, once enabled
        assertEquals(Long.valueOf(1), kvs.getLong(doc.getId()));
        transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(
                TRANSCODED_VIDEOS_PROPERTY);
        assertEquals(2, transcodedVideos.size());
        storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue(STORYBOARD_PROPERTY);
        assertEquals(2, storyboard.size());
    }

    // NXP-29966
    @Test
    public void testVideoInfoUpdateWhenCreating() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VIDEO_TYPE);
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        Map<String, Serializable> videoInfoMap = new HashMap<>();
        videoInfoMap.put(VideoInfo.WIDTH, 100);
        videoInfoMap.put(VideoInfo.HEIGHT, 50);
        videoInfoMap.put(VideoInfo.DURATION, 150);
        videoInfoMap.put(VideoInfo.FORMAT, "foo");
        doc.setPropertyValue(VideoConstants.INFO_PROPERTY, (Serializable) videoInfoMap);
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // video info not computed as it was provided at creation time
        doc = session.getDocument(doc.getRef());
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        Video video = videoDocument.getVideo();
        assertEquals(100, video.getWidth());
        assertEquals(50, video.getHeight());
        assertEquals(150, video.getDuration(), 0.1);

        // make the main blob dirty, triggers the video info recompute
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.saveDocument(doc);
        txFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        videoDocument = doc.getAdapter(VideoDocument.class);
        video = videoDocument.getVideo();
        assertEquals(320, video.getWidth());
        assertEquals(200, video.getHeight());
        assertEquals(0.05, video.getDuration(), 0.1);
    }

    // NXP-29966
    @Test
    @SuppressWarnings("unchecked")
    public void testPreviewsUpdateWhenCreating() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VIDEO_TYPE);
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        List<Map<String, Serializable>> pictureviews = new ArrayList<>();
        Map<String, Serializable> pictureView = new HashMap<>();
        pictureView.put("title", "StaticPlayerView");
        pictureView.put("content", (Serializable) Blobs.createBlob("dummy"));
        pictureviews.add(pictureView);
        doc.setPropertyValue("picture:views", (Serializable) pictureviews);
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // previews not computed as it was provided at creation time
        doc = session.getDocument(doc.getRef());
        pictureviews = (List<Map<String, Serializable>>) doc.getPropertyValue("picture:views");
        assertEquals(1, pictureviews.size());
        assertEquals("StaticPlayerView", pictureviews.get(0).get("title"));

        // make the main blob dirty, triggers the previews recompute
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.saveDocument(doc);
        txFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        pictureviews = (List<Map<String, Serializable>>) doc.getPropertyValue("picture:views");
        assertEquals(2, pictureviews.size());
        assertEquals("Small", pictureviews.get(0).get("title"));
        assertEquals("StaticPlayerView", pictureviews.get(1).get("title"));
    }

    // NXP-29966
    @Test
    @Deploy("org.nuxeo.ecm.platform.video.core:video-configuration-override.xml")
    @SuppressWarnings("unchecked")
    public void testStoryboardUpdateWhenCreating() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VIDEO_TYPE);
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        Map<String, Serializable> item = new HashMap<>();
        item.put("content", (Serializable) Blobs.createBlob("dummy"));
        item.put("timecode", 5);
        List<Map<String, Serializable>> storyboard = new ArrayList<>();
        storyboard.add(item);
        doc.setPropertyValue(STORYBOARD_PROPERTY, (Serializable) storyboard);
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // storyboard not computed as it was provided at creation time
        doc = session.getDocument(doc.getRef());
        storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue(STORYBOARD_PROPERTY);
        assertEquals(1, storyboard.size());
        Blob storyboardBlob = (Blob) storyboard.get(0).get("content");
        assertEquals("dummy", storyboardBlob.getString());

        // make the main blob dirty, triggers the storyboard recompute
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.saveDocument(doc);
        txFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue(STORYBOARD_PROPERTY);
        assertEquals(2, storyboard.size());
    }

    // NXP-29966
    @Test
    @Deploy("org.nuxeo.ecm.platform.video.core:test-video-conversions-enabled.xml")
    @SuppressWarnings("unchecked")
    public void testVideoConversionsUpdateWhenCreating() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VIDEO_TYPE);
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        List<Map<String, Serializable>> transcodedVideos = new ArrayList<>();
        Map<String, Serializable> transcodedVideo = new HashMap<>();
        transcodedVideo.put("name", "MP4 480p");
        transcodedVideo.put("content", (Serializable) Blobs.createBlob("dummy"));
        transcodedVideos.add(transcodedVideo);
        doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, (Serializable) transcodedVideos);
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // only missing conversion is computed as the other was provided at creation time
        doc = session.getDocument(doc.getRef());
        transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(TRANSCODED_VIDEOS_PROPERTY);
        assertEquals(2, transcodedVideos.size());
        assertEquals("MP4 480p", transcodedVideos.get(0).get("name"));
        Blob transcodedVideoBlob = (Blob) transcodedVideos.get(0).get("content");
        assertEquals("text/plain", transcodedVideoBlob.getMimeType());
        assertEquals("dummy", transcodedVideoBlob.getString());
        assertEquals("WebM 480p", transcodedVideos.get(1).get("name"));
        transcodedVideoBlob = (Blob) transcodedVideos.get(1).get("content");
        assertNotNull(transcodedVideoBlob);
        assertEquals("video/webm", transcodedVideoBlob.getMimeType());

        // make the main blob dirty, triggers the video conversions
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.saveDocument(doc);
        txFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(TRANSCODED_VIDEOS_PROPERTY);
        assertEquals(2, transcodedVideos.size());
        Set<String> transcodedNames = transcodedVideos.stream().map(v -> (String) v.get("name")).collect(Collectors.toSet());
        Set<String> transcodedMimeTypes = transcodedVideos.stream().map(v -> ((Blob) v.get("content")).getMimeType()).collect(Collectors.toSet());
        assertTrue(transcodedNames.toString(), transcodedNames.contains("MP4 480p"));
        assertTrue(transcodedNames.toString(), transcodedNames.contains("WebM 480p"));
        assertTrue(transcodedMimeTypes.toString(), transcodedMimeTypes.contains("video/mp4"));
        assertTrue(transcodedMimeTypes.toString(), transcodedMimeTypes.contains("video/webm"));
    }

    // NXP-29966
    @Test
    @Deploy("org.nuxeo.ecm.platform.video.core:test-video-conversions-enabled.xml")
    @Deploy("org.nuxeo.ecm.platform.video.core:video-configuration-override.xml")
    @SuppressWarnings("unchecked")
    public void testComputeAllOnVersion() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VIDEO_TYPE);
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc.putContextData(VideoChangedListener.DISABLE_VIDEO_CONVERSIONS_GENERATION_LISTENER, true);
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // nothing computed
        doc = session.getDocument(doc.getRef());
        Map<String, Serializable> videoInfo = (Map<String, Serializable>) doc.getPropertyValue(
                VideoConstants.INFO_PROPERTY);
        assertNull(videoInfo.get("width"));
        assertNull(videoInfo.get("height"));
        List<Map<String, Serializable>> pictureViews = (List<Map<String, Serializable>>) doc.getPropertyValue(
                "picture:views");
        assertTrue(pictureViews.isEmpty());
        List<Map<String, Serializable>> storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue(
                STORYBOARD_PROPERTY);
        assertTrue(storyboard.isEmpty());
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(
                TRANSCODED_VIDEOS_PROPERTY);
        assertTrue(transcodedVideos.isEmpty());

        // create a version
        DocumentRef versionRef = session.checkIn(doc.getRef(), VersioningOption.NONE, null);
        txFeature.nextTransaction();

        // everything is computed
        doc = session.getDocument(versionRef);
        // check video info
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        Video video = videoDocument.getVideo();
        assertEquals(320, video.getWidth());
        assertEquals(200, video.getHeight());
        assertEquals(0.05, video.getDuration(), 0.1);
        // check previews
        pictureViews = (List<Map<String, Serializable>>) doc.getPropertyValue("picture:views");
        assertEquals(2, pictureViews.size());
        assertEquals("Small", pictureViews.get(0).get("title"));
        assertEquals("StaticPlayerView", pictureViews.get(1).get("title"));
        // check storyboard
        storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue(STORYBOARD_PROPERTY);
        assertEquals(2, storyboard.size());

        transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(TRANSCODED_VIDEOS_PROPERTY);
        assertEquals(2, transcodedVideos.size());
        Set<String> transcodedNames = transcodedVideos.stream().map(v -> (String) v.get("name")).collect(Collectors.toSet());
        Set<String> transcodedMimeTypes = transcodedVideos.stream().map(v -> ((Blob) v.get("content")).getMimeType()).collect(Collectors.toSet());
        assertTrue(transcodedNames.toString(), transcodedNames.contains("MP4 480p"));
        assertTrue(transcodedNames.toString(), transcodedNames.contains("WebM 480p"));
        assertTrue(transcodedMimeTypes.toString(), transcodedMimeTypes.contains("video/mp4"));
        assertTrue(transcodedMimeTypes.toString(), transcodedMimeTypes.contains("video/webm"));
    }

    // NXP-29966
    @Test
    @Deploy("org.nuxeo.ecm.platform.video.core:test-video-conversions-enabled.xml")
    @Deploy("org.nuxeo.ecm.platform.video.core:video-configuration-override.xml")
    @SuppressWarnings("unchecked")
    public void testComputeMissingOnVersion() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VIDEO_TYPE);
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc.putContextData(VideoChangedListener.DISABLE_VIDEO_CONVERSIONS_GENERATION_LISTENER, true);
        // prefill video info and previews
        Map<String, Serializable> videoInfoMap = new HashMap<>();
        videoInfoMap.put(VideoInfo.WIDTH, 100);
        videoInfoMap.put(VideoInfo.HEIGHT, 50);
        videoInfoMap.put(VideoInfo.DURATION, 150);
        videoInfoMap.put(VideoInfo.FORMAT, "foo");
        doc.setPropertyValue(VideoConstants.INFO_PROPERTY, (Serializable) videoInfoMap);
        List<Map<String, Serializable>> pictureviews = new ArrayList<>();
        Map<String, Serializable> pictureView = new HashMap<>();
        pictureView.put("title", "StaticPlayerView");
        pictureView.put("content", (Serializable) Blobs.createBlob("dummy"));
        pictureviews.add(pictureView);
        doc.setPropertyValue("picture:views", (Serializable) pictureviews);
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // only video info and previews computed
        doc = session.getDocument(doc.getRef());
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        Video video = videoDocument.getVideo();
        assertEquals(100, video.getWidth());
        assertEquals(50, video.getHeight());
        assertEquals(150, video.getDuration(), 0.1);
        assertEquals("foo", video.getFormat());
        List<Map<String, Serializable>> pictureViews = (List<Map<String, Serializable>>) doc.getPropertyValue(
                "picture:views");
        assertEquals(1, pictureViews.size());
        assertEquals("StaticPlayerView", pictureViews.get(0).get("title"));
        Blob pictureViewBlob = (Blob) pictureViews.get(0).get("content");
        assertEquals("dummy", pictureViewBlob.getString());
        List<Map<String, Serializable>> storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue(
                STORYBOARD_PROPERTY);
        assertTrue(storyboard.isEmpty());
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(
                TRANSCODED_VIDEOS_PROPERTY);
        assertTrue(transcodedVideos.isEmpty());

        // create a version
        DocumentRef versionRef = session.checkIn(doc.getRef(), VersioningOption.NONE, null);
        txFeature.nextTransaction();

        // only missing storyboard and conversions are computed
        doc = session.getDocument(versionRef);
        // check video info
        videoDocument = doc.getAdapter(VideoDocument.class);
        video = videoDocument.getVideo();
        assertEquals(100, video.getWidth());
        assertEquals(50, video.getHeight());
        assertEquals(150, video.getDuration(), 0.1);
        assertEquals("foo", video.getFormat());
        // check previews
        pictureViews = (List<Map<String, Serializable>>) doc.getPropertyValue("picture:views");
        assertEquals(1, pictureViews.size());
        assertEquals("StaticPlayerView", pictureViews.get(0).get("title"));
        pictureViewBlob = (Blob) pictureViews.get(0).get("content");
        assertEquals("dummy", pictureViewBlob.getString());
        // check storyboard
        storyboard = (List<Map<String, Serializable>>) doc.getPropertyValue(STORYBOARD_PROPERTY);
        assertEquals(2, storyboard.size());

        transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(TRANSCODED_VIDEOS_PROPERTY);
        assertEquals(2, transcodedVideos.size());
        Set<String> transcodedNames = transcodedVideos.stream().map(v -> (String) v.get("name")).collect(Collectors.toSet());
        Set<String> transcodedMimeTypes = transcodedVideos.stream().map(v -> ((Blob) v.get("content")).getMimeType()).collect(Collectors.toSet());
        assertTrue(transcodedNames.toString(), transcodedNames.contains("MP4 480p"));
        assertTrue(transcodedNames.toString(), transcodedNames.contains("WebM 480p"));
        assertTrue(transcodedMimeTypes.toString(), transcodedMimeTypes.contains("video/mp4"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.video.core:test-video-conversions-enabled.xml")
    @Deploy("org.nuxeo.ecm.platform.video.core:test-video-auto-versioning-on-creation.xml")
    public void testAutoVersioningOnCreationNoAutoCheckout() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "testVideoDoc", VIDEO_TYPE);
        Blob blob = getBlobFromPath("test-data/sample.mpg", "video/mpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());
    }
}

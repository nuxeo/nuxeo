/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.service;

import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_TYPE;
import static org.nuxeo.ecm.platform.video.convert.WebMConverter.WEBM_EXTENSION;
import static org.nuxeo.ecm.platform.video.convert.WebMConverter.WEBM_VIDEO_MIMETYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.3
 */
public class TestVideoService extends SQLRepositoryTestCase {

    public static final String DELTA_MP4 = "DELTA.mp4";

    protected VideoService videoService;

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

        openSession();

        EventServiceAdmin eventServiceAdmin = Framework.getLocalService(EventServiceAdmin.class);
        eventServiceAdmin.setListenerEnabledFlag(
                "videoAutomaticConversions", false);
        eventServiceAdmin.setListenerEnabledFlag(
                        "sql-storage-binary-text", false);


        videoService = Framework.getLocalService(VideoService.class);
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    public void testVideoConversion() throws IOException, ClientException {
        Blob video = getBlobFromPath(DELTA_MP4, "video/mp4");
        TranscodedVideo transcodedVideo = videoService.convert(video,
                "WebM 480p");
        assertNotNull(transcodedVideo);
        assertEquals(WEBM_VIDEO_MIMETYPE,
                transcodedVideo.getVideoBlob().getMimeType());
        assertTrue(transcodedVideo.getVideoBlob().getFilename().endsWith(
                WEBM_EXTENSION));
        assertEquals("WebM 480p", transcodedVideo.getName());
        assertEquals(8.38, transcodedVideo.getDuration(), 0.1);
        assertEquals(768, transcodedVideo.getWidth());
        assertEquals(480, transcodedVideo.getHeight());
        assertEquals(23.98, transcodedVideo.getFrameRate());
        assertEquals("webm", transcodedVideo.getFormat());
    }

    protected static Blob getBlobFromPath(String path, String mimeType)
            throws IOException {
        InputStream is = TestVideoService.class.getResourceAsStream("/" + path);
        assertNotNull(String.format("Failed to load resource: " + path), is);
        Blob blob = StreamingBlob.createFromStream(is, mimeType);
        blob.setFilename(FilenameUtils.getName(path));
        return blob.persist();
    }

    public void testAsynchronousVideoConversion() throws IOException,
            ClientException, InterruptedException {
        Blob video = getBlobFromPath(DELTA_MP4, "video/mp4");
        DocumentModel doc = session.createDocumentModel("/", "video",
                VIDEO_TYPE);
        doc.setPropertyValue("file:content", (Serializable) video);
        doc = session.createDocument(doc);
        session.save();

        videoService.launchConversion(doc, "WebM 480p");

        while(videoService.getProgressStatus(doc.getRepositoryName(), doc.getRef(), "WebM 480p") != null) {
            // wait for the conversion to complete
            Thread.sleep(200);
        }

        session.save();
        doc = session.getDocument(doc.getRef());
        assertNotNull(doc);
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue("vid:transcodedVideos");
        assertNotNull(transcodedVideos);
        assertEquals(1, transcodedVideos.size());
    }

}

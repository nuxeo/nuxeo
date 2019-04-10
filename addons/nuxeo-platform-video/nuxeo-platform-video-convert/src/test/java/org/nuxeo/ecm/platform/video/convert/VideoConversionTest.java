/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.video.convert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class VideoConversionTest extends NXRuntimeTestCase {

    public static final Log log = LogFactory.getLog(VideoConversionTest.class);

    public static final String DELTA_MP4 = "DELTA.mp4";

    public static final String DELTA_OGV = "DELTA.ogv";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.convert");
        deployBundle("org.nuxeo.ecm.platform.video.convert");
    }

    protected static BlobHolder getBlobFromPath(String path, String mimeType) throws IOException {
        InputStream is = VideoConvertersTest.class.getResourceAsStream("/" + path);
        assertNotNull(String.format("Failed to load resource: " + path), is);
        Blob blob = StreamingBlob.createFromStream(is, mimeType);
        blob.setFilename(FilenameUtils.getName(path));
        return new SimpleBlobHolder(blob.persist());
    }

    protected BlobHolder applyConverter(String converter, String fileName, String mimeType, long newHeight)
            throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);
        assertNotNull(cs.getRegistredConverters().contains(converter));
        BlobHolder in = getBlobFromPath(fileName, mimeType);
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        Map<String, Serializable> videoInfo = new HashMap<String, Serializable>();
        videoInfo.put(VideoInfo.WIDTH, 768L);
        videoInfo.put(VideoInfo.HEIGHT, 480L);
        parameters.put("videoInfo", VideoInfo.fromMap(videoInfo));
        parameters.put("height", newHeight);
        BlobHolder result = cs.convert(converter, in, parameters);
        assertNotNull(result);
        return result;
    }

    @Test
    public void testWebMConversion() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-towebm");
        Assume.assumeTrue("ffmpeg-towebm is not available, skipping test", ca.isAvailable());

        BlobHolder result = applyConverter(Constants.TO_WEBM_CONVERTER, DELTA_MP4, "video/x-msvideo", 480);
        List<Blob> blobs = result.getBlobs();
        assertFalse(blobs.isEmpty());
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("DELTA.webm", blob.getFilename());
        assertEquals("video/webm", blob.getMimeType());
    }

    @Test
    public void testOggConversion() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-toogg");
        Assume.assumeTrue("ffmpeg-toogg is not available, skipping test", ca.isAvailable());

        BlobHolder result = applyConverter(Constants.TO_OGG_CONVERTER, DELTA_MP4, "video/x-msvideo", 480);
        List<Blob> blobs = result.getBlobs();
        assertFalse(blobs.isEmpty());
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("DELTA.ogg", blob.getFilename());
        assertEquals("video/ogg", blob.getMimeType());
    }

    @Test
    public void testMP4Conversion() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);
        CommandAvailability ca = cles.getCommandAvailability("ffmpeg-toogg");
        Assume.assumeTrue("ffmpeg-toogg is not available, skipping test", ca.isAvailable());

        BlobHolder result = applyConverter(Constants.TO_MP4_CONVERTER, DELTA_OGV, "video/ogg", 480);
        List<Blob> blobs = result.getBlobs();
        assertFalse(blobs.isEmpty());
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("DELTA.mp4", blob.getFilename());
        assertEquals("video/mp4", blob.getMimeType());
    }
}

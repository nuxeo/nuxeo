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
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.media.convert;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestStreamableMediaConverters extends NXRuntimeTestCase {

    public static final Log log = LogFactory.getLog(TestStreamableMediaConverters.class);

    public static final String TEST_FILE_OGV = "DELTA.ogv";

    public static final String TEST_FILE_3GP = "DELTA.3gp";

    public static final String TEST_FILE_MP4 = "DELTA.mp4";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.video.convert");

        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);
        CommandAvailability caPivot = cles.getCommandAvailability(ConverterConstants.FFMPEG_CONVERT);
        CommandAvailability caH264Aac = cles.getCommandAvailability(ConverterConstants.HANDBRAKE_CONVERT_MP4);
        CommandAvailability caHint = cles.getCommandAvailability(ConverterConstants.MP4BOX_HINT_MEDIA);
        if (!caPivot.isAvailable()) {
            log.warn(caPivot.getInstallMessage());
            return;
        }
        if (!caH264Aac.isAvailable()) {
            log.warn(caH264Aac.getInstallMessage());
            return;
        }
        if (!caHint.isAvailable()) {
            log.warn(caHint.getInstallMessage());
            return;
        }

    }

    protected static BlobHolder getBlobFromPath(String path) throws IOException {
        InputStream is = TestStreamableMediaConverters.class.getResourceAsStream("/"
                + path);
        assertNotNull(String.format("Failed to load resource: " + path), is);
        return new SimpleBlobHolder(
                StreamingBlob.createFromStream(is, path).persist());
    }

    protected BlobHolder applyConverter(String converter, String fileName) throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);
        assertNotNull(cs.getRegistredConverters().contains(converter));
        BlobHolder in = getBlobFromPath(fileName);
        Map<String, Serializable> params = null;
        BlobHolder result = cs.convert(converter, in, params);
        assertNotNull(result);
        return result;
    }

    public void testNothing() {
        assertTrue(true);
    }

//    public void testStreamableConverterOgv() throws Exception {
//
//        BlobHolder result = applyConverter(ConverterConstants.STREAMABLE_MEDIA_CONVERTER_NAME,
//                TEST_FILE_OGV);
//        List<Blob> blobs = result.getBlobs();
//        assertEquals(1, blobs.size());
//        assertEquals("streamable-media.mp4", blobs.get(0).getFilename());
//    }
//
//    public void testStreamableConverterMp4() throws Exception {
//
//        BlobHolder result = applyConverter(ConverterConstants.STREAMABLE_MEDIA_CONVERTER_NAME,
//                TEST_FILE_MP4);
//        List<Blob> blobs = result.getBlobs();
//        assertEquals(1, blobs.size());
//        assertEquals("streamable-media.mp4", blobs.get(0).getFilename());
//    }
//    public void testStreamableConverter3gp() throws Exception {
//
//        BlobHolder result = applyConverter(ConverterConstants.STREAMABLE_MEDIA_CONVERTER_NAME,
//                TEST_FILE_3GP);
//        List<Blob> blobs = result.getBlobs();
//        assertEquals(1, blobs.size());
//        assertEquals("streamable-media.mp4", blobs.get(0).getFilename());
//    }
}

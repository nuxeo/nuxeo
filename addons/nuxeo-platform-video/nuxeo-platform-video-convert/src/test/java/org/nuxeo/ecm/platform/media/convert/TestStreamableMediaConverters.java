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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
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

    protected boolean skipTests = false;

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
        CommandAvailability caH264Aac = cles.getCommandAvailability(ConverterConstants.HANDBRAKE_CONVERT_MP4);
        CommandAvailability caHint = cles.getCommandAvailability(ConverterConstants.MP4BOX_HINT_MEDIA);
        if (!caH264Aac.isAvailable()) {
            log.warn(caH264Aac.getInstallMessage());
            skipTests = true;
        }
        if (!caHint.isAvailable()) {
            log.warn(caHint.getInstallMessage());
            skipTests = true;
        }

    }

    protected static BlobHolder getBlobFromPath(String path) throws IOException {
        InputStream is = TestStreamableMediaConverters.class.getResourceAsStream("/"
                + path);
        assertNotNull(String.format("Failed to load resource: " + path), is);
        Blob blob = StreamingBlob.createFromStream(is, path).persist();
        blob.setFilename(path);
        return new SimpleBlobHolder(blob);
    }

    protected BlobHolder applyConverter(String converter, String fileName)
            throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);
        assertNotNull(cs.getRegistredConverters().contains(converter));
        BlobHolder in = getBlobFromPath(fileName);
        Map<String, Serializable> params = null;
        BlobHolder result = cs.convert(converter, in, params);
        assertNotNull(result);
        return result;
    }

    public void testStreamableConverterFromOgv() throws Exception {
        if (skipTests) {
            return;
        }
        BlobHolder result = applyConverter(
                ConverterConstants.STREAMABLE_MEDIA_CONVERTER_NAME,
                TEST_FILE_OGV);
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals("DELTA.ogv--streamable.mp4", blobs.get(0).getFilename());
    }

    public void testStreamableConverterFromMp4() throws Exception {
        if (skipTests) {
            return;
        }
        BlobHolder result = applyConverter(
                ConverterConstants.STREAMABLE_MEDIA_CONVERTER_NAME,
                TEST_FILE_MP4);
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals("DELTA.mp4--streamable.mp4", blobs.get(0).getFilename());
    }

    public void testStreamableConverterFrom3gp() throws Exception {
        if (skipTests) {
            return;
        }
        BlobHolder result = applyConverter(
                ConverterConstants.STREAMABLE_MEDIA_CONVERTER_NAME,
                TEST_FILE_3GP);
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals("DELTA.3gp--streamable.mp4", blobs.get(0).getFilename());
    }
}

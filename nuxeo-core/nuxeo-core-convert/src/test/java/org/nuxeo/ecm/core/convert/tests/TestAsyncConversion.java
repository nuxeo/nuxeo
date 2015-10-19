/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.transientstore.test.TransientStoreFeature;

import com.google.inject.Inject;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ ConvertFeature.class, TransientStoreFeature.class })
@Deploy("org.nuxeo.ecm.core.event")
@LocalDeploy({ "org.nuxeo.ecm.core.convert:OSGI-INF/convert-service-config-test.xml",
        "org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib3.xml" })
public class TestAsyncConversion {

    @Inject
    protected ConversionService conversionService;

    @Inject
    protected EventService eventService;

    @Test
    public void shouldDoAsyncConversionGivenDestinationMimeType() throws IOException {
        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");
        Blob blob = Blobs.createBlob(file, "application/msword", null, "hello.doc");
        BlobHolder bh = new SimpleBlobHolder(blob);

        String id = conversionService.scheduleConversionToMimeType("test/cache", bh, null);
        assertNotNull(id);

        eventService.waitForAsyncCompletion();

        BlobHolder result = conversionService.getConversionResult(id, true);
        assertNotNull(result);
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        Blob resultBlob = blobs.get(0);
        assertEquals(blob.getFilename(), resultBlob.getFilename());
        assertEquals(blob.getMimeType(), resultBlob.getMimeType());
    }

    @Test
    public void shouldDoAsyncConversionGivenConverterName() throws IOException {
        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");
        Blob blob = Blobs.createBlob(file, "application/msword", null, "hello.doc");
        BlobHolder bh = new SimpleBlobHolder(blob);

        String id = conversionService.scheduleConversion("identity", bh, null);
        assertNotNull(id);

        eventService.waitForAsyncCompletion();

        BlobHolder result = conversionService.getConversionResult(id, true);
        assertNotNull(result);
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        Blob resultBlob = blobs.get(0);
        assertEquals(blob.getFilename(), resultBlob.getFilename());
        assertEquals(blob.getMimeType(), resultBlob.getMimeType());
    }

}

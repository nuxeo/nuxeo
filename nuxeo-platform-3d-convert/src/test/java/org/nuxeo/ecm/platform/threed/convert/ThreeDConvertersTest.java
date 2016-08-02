/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.convert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.core.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nuxeo.ecm.platform.threed.convert.Constants.BLENDER_PIPELINE_COMMAND;
import static org.nuxeo.ecm.platform.threed.convert.Constants.COLLADA2GLTF_COMMAND;

/**
 * Test 3D converters
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@LocalDeploy({ "org.nuxeo.ecm.platform.threed.convert", "org.nuxeo.ecm.platform.threed.api" })
public class ThreeDConvertersTest {

    public static final Log log = LogFactory.getLog(ThreeDConvertersTest.class);

    public static final String TEST_MODEL = "suzane";

    protected static BlobHolder getTestThreeDBlobs() throws IOException {
        List<Blob> blobs = new ArrayList<>();
        Blob blob = null;
        try (InputStream is = ThreeDConvertersTest.class.getResourceAsStream("/test-data/" + TEST_MODEL + ".obj")) {
            Assert.assertNotNull(String.format("Failed to load resource: " + TEST_MODEL + ".obj"), is);
            blob = Blobs.createBlob(is);
            blob.setFilename(TEST_MODEL + ".obj");
            blobs.add(blob);
        }
        try (InputStream is = ThreeDConvertersTest.class.getResourceAsStream("/test-data/" + TEST_MODEL + ".mtl")) {
            Assert.assertNotNull(String.format("Failed to load resource: " + TEST_MODEL + ".mtl"), is);
            blob = Blobs.createBlob(is);
            blob.setFilename(TEST_MODEL + ".mtl");
            blobs.add(blob);
        }
        return new SimpleBlobHolder(blobs);
    }

    protected static BlobHolder getTestColladaBlob() throws IOException {
        List<Blob> blobs = new ArrayList<>();
        try (InputStream is = ThreeDConvertersTest.class.getResourceAsStream("/test-data/" + TEST_MODEL + ".dae")) {
            Assert.assertNotNull(String.format("Failed to load resource: " + TEST_MODEL + ".dae"), is);
            Blob blob = Blobs.createBlob(is);
            blob.setFilename(TEST_MODEL + ".dae");
            blobs.add(blob);
        }
        return new SimpleBlobHolder(blobs);
    }

    protected BlobHolder applyConverter(String converter, BlobHolder blobs) throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);
        Assert.assertNotNull(cs.getRegistredConverters().contains(converter));
        Map<String, Serializable> params = new HashMap<>();
        BlobHolder result = cs.convert(converter, blobs, params);
        Assert.assertNotNull(result);
        return result;
    }

    @Test
    public void testBlenderPipelineCommand() throws Exception {
        CommandLineExecutorService commandLES = Framework.getService(CommandLineExecutorService.class);
        Assert.assertNotNull(commandLES);
        CommandAvailability ca = commandLES.getCommandAvailability(BLENDER_PIPELINE_COMMAND);
        Assert.assertTrue("blender_pipeline is not available, skipping test", ca.isAvailable());
    }

    @Test
    public void testDae2GltfCommand() throws Exception {
        CommandLineExecutorService commandLES = Framework.getService(CommandLineExecutorService.class);
        Assert.assertNotNull(commandLES);
        CommandAvailability ca = commandLES.getCommandAvailability(COLLADA2GLTF_COMMAND);
        Assert.assertTrue("dae2gltf is not available, skipping test", ca.isAvailable());
    }

    @Test
    public void testRenderConverter() throws Exception {
        BlobHolder result = applyConverter(Constants.RENDER_3D_CONVERTER, getTestThreeDBlobs());
        List<Blob> blobs = result.getBlobs();
        Assert.assertEquals(1, blobs.size());
        Assert.assertEquals("render-100.png", blobs.get(0).getFilename());
    }

    @Test
    public void testCollada2glTFConverter() throws Exception {
        BlobHolder result = applyConverter(Constants.COLLADA2GLTF_CONVERTER, getTestColladaBlob());
        List<Blob> blobs = result.getBlobs();
        Assert.assertEquals(1, blobs.size());
        Assert.assertEquals(TEST_MODEL + ".gltf", blobs.get(0).getFilename());
    }

}

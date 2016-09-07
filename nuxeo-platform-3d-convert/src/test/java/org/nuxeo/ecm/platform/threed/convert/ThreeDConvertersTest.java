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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.EXTENSION_3DSTUDIO;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.EXTENSION_COLLADA;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.EXTENSION_EXTENSIBLE_3D_GRAPHICS;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.EXTENSION_FILMBOX;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.EXTENSION_STANFORD;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.EXTENSION_STEREOLITHOGRAPHY;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.EXTENSION_WAVEFRONT;
import static org.nuxeo.ecm.platform.threed.convert.Constants.*;

/**
 * Test 3D converters
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@LocalDeploy({ "org.nuxeo.ecm.platform.threed.convert", "org.nuxeo.ecm.platform.threed.api",
    "org.nuxeo.ecm.platform.commandline.executor" })
public class ThreeDConvertersTest {

    protected static final String TEST_MODEL = "suzanne";

    private static final String renderId1 = "renderId1";

    private static final String renderId2 = "renderId2";

    @Inject
    protected ConversionService cs;

    @Inject
    protected CommandLineExecutorService commandLES;

    protected static BlobHolder getTestThreeDBlobs() throws IOException {
        List<Blob> blobs = new ArrayList<>();
        Blob blob;
        try (InputStream is = ThreeDConvertersTest.class.getResourceAsStream("/test-data/" + TEST_MODEL + ".obj")) {
            assertNotNull(String.format("Failed to load resource: %s.obj", TEST_MODEL), is);
            blob = Blobs.createBlob(is);
            blob.setFilename(TEST_MODEL + ".obj");
            blobs.add(blob);
        }
        try (InputStream is = ThreeDConvertersTest.class.getResourceAsStream("/test-data/" + TEST_MODEL + ".mtl")) {
            assertNotNull(String.format("Failed to load resource: %s.mtl", TEST_MODEL), is);
            blob = Blobs.createBlob(is);
            blob.setFilename(TEST_MODEL + ".mtl");
            blobs.add(blob);
        }
        return new SimpleBlobHolder(blobs);
    }

    protected static BlobHolder getTestBlob(String extension) throws IOException {
        List<Blob> blobs = new ArrayList<>();
        String fileName = String.format("%s.%s", TEST_MODEL, extension);
        try (InputStream is = ThreeDConvertersTest.class.getResourceAsStream("/test-data/" + fileName)) {
            assertNotNull(String.format("Failed to load resource: %s", fileName), is);
            Blob blob = Blobs.createBlob(is);
            blob.setFilename(fileName);
            blobs.add(blob);
        }
        return new SimpleBlobHolder(blobs);
    }

    protected BlobHolder applyConverter(String converter, BlobHolder blobs) throws Exception {
        assertNotNull(cs.getRegistredConverters().contains(converter));
        Map<String, Serializable> params = new HashMap<>();
        params.put(RENDER_IDS_PARAMETER, renderId1 + " " + renderId2);
        BlobHolder result = cs.convert(converter, blobs, params);
        assertNotNull(result);
        return result;
    }

    protected void testColladaConverterWithBlobs(BlobHolder inputBlobs) throws Exception {
        List<Blob> resultBlobs = applyConverter(COLLADA_CONVERTER, inputBlobs).getBlobs();
        assertEquals(1, resultBlobs.size());
        assertTrue(resultBlobs.get(0).getLength() > 0);
        assertTrue(resultBlobs.get(0).getFilename().contains("transmissionformat"));
        assertTrue(resultBlobs.get(0).getFilename().contains(".dae"));
    }

    @Test
    public void testBlenderPipelineCommand() throws Exception {
        CommandAvailability ca = commandLES.getCommandAvailability(BLENDER_PIPELINE_COMMAND);
        assertTrue("blender_pipeline is not available, skipping test", ca.isAvailable());
    }

    @Test
    public void testDae2GltfCommand() throws Exception {
        CommandAvailability ca = commandLES.getCommandAvailability(COLLADA2GLTF_COMMAND);
        assertTrue("dae2gltf is not available, skipping test", ca.isAvailable());
    }

    @Test
    public void testRenderConverter() throws Exception {
        BlobHolder result = applyConverter(RENDER_3D_CONVERTER, getTestThreeDBlobs());
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertTrue(blobs.get(0).getFilename().contains("render-" + renderId1));
    }

    @Test
    public void testCollada2glTFConverter() throws Exception {
        BlobHolder result = applyConverter(COLLADA2GLTF_CONVERTER, getTestBlob(EXTENSION_COLLADA));
        List<Blob> blobs = result.getBlobs();
        assertEquals(1, blobs.size());
        assertEquals(TEST_MODEL + ".gltf", blobs.get(0).getFilename());
    }

    @Test
    public void testColladaConverter3ds() throws Exception {
        testColladaConverterWithBlobs(getTestBlob(EXTENSION_3DSTUDIO));
    }

    @Test
    public void testColladaConverterFbx() throws Exception {
        testColladaConverterWithBlobs(getTestBlob(EXTENSION_FILMBOX));
    }

    @Test
    public void testColladaConverterPly() throws Exception {
        testColladaConverterWithBlobs(getTestBlob(EXTENSION_STANFORD));
    }

    @Test
    public void testColladaConverterX3d() throws Exception {
        testColladaConverterWithBlobs(getTestBlob(EXTENSION_EXTENSIBLE_3D_GRAPHICS));
    }

    @Test
    public void testColladaConverterStl() throws Exception {
        testColladaConverterWithBlobs(getTestBlob(EXTENSION_STEREOLITHOGRAPHY));
    }

    @Test
    public void testColladaConverterObj() throws Exception {
        testColladaConverterWithBlobs(getTestBlob(EXTENSION_WAVEFRONT));
    }

    @Test
    public void testColladaConverterObjMtl() throws Exception {
        BlobHolder blobHolder = getTestBlob(EXTENSION_WAVEFRONT);
        blobHolder.getBlobs().add(getTestBlob("mtl").getBlob());
        testColladaConverterWithBlobs(blobHolder);
    }

    @Test
    public void testLODConverter() throws Exception {
        BlobHolder result = applyConverter(LOD_CONVERTER, getTestThreeDBlobs());
        List<Blob> blobs = result.getBlobs();
        assertEquals(3, blobs.size());
        assertTrue(blobs.get(0).getFilename().contains("transmissionformat-03"));
        assertTrue(blobs.get(0).getFilename().contains(".dae"));
        assertTrue(blobs.get(1).getFilename().contains("transmissionformat-11"));
        assertTrue(blobs.get(1).getFilename().contains(".dae"));
        assertTrue(blobs.get(2).getFilename().contains("transmissionformat-33"));
        assertTrue(blobs.get(2).getFilename().contains(".dae"));
    }

    @Test
    public void testBatchConverter() throws Exception {
        BlobHolder result = applyConverter(BATCH_CONVERTER, getTestThreeDBlobs());
        List<Blob> blobs = result.getBlobs();
        assertEquals(5, blobs.size());
        List<String> fileNames = blobs.stream().map(Blob::getFilename).collect(Collectors.toList());
        assertTrue(blobs.get(0).getFilename().contains("transmissionformat-03"));
        assertTrue(blobs.get(0).getFilename().contains(".dae"));
        assertTrue(blobs.get(1).getFilename().contains("transmissionformat-11"));
        assertTrue(blobs.get(1).getFilename().contains(".dae"));
        assertTrue(blobs.get(2).getFilename().contains("transmissionformat-33"));
        assertTrue(blobs.get(2).getFilename().contains(".dae"));
        assertTrue(fileNames.get(3).contains("render-" + renderId1));
        assertTrue(fileNames.get(4).contains("render-" + renderId2));
    }
}

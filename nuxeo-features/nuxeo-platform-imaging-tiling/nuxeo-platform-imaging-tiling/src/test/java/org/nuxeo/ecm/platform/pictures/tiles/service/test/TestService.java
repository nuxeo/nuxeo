/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.pictures.tiles.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageIdentifier;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilesImpl;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.BlobResource;
import org.nuxeo.ecm.platform.pictures.tiles.magick.tiler.MagickTiler;
import org.nuxeo.ecm.platform.pictures.tiles.service.GCTask;
import org.nuxeo.ecm.platform.pictures.tiles.service.PictureTilingCacheGCManager;
import org.nuxeo.ecm.platform.pictures.tiles.service.PictureTilingComponent;
import org.nuxeo.ecm.platform.pictures.tiles.tilers.PictureTiler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.pictures.tiles:OSGI-INF/pictures-tiles-framework.xml")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.picture.core:OSGI-INF/commandline-imagemagick-contrib.xml")
public class TestService {

    @Before
    public void postSetUp() throws Exception {
        PictureTilingComponent.getCache().clear();
        PictureTilingComponent.setDefaultTiler(new MagickTiler());
        PictureTilingComponent.endGC();
    }

    @After
    public void tearDown() {
        PictureTilingComponent.endGC();
    }

    @Test
    public void testLookup() {
        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);
    }

    @Test
    public void testTilingSimple() throws Exception {
        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);

        PictureTiles tiles = pts.getTiles(new BlobResource(image), 255, 255, 20);

        assertNotNull(tiles);
        assertFalse(tiles.getZoomfactor() == 0);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.pictures.tiles:OSGI-INF/pictures-tiles-adapter-contrib.xml")
    public void testAdapter() throws Exception {
        // do nothing
    }

    @Ignore("NXP-25718")
    @Test
    public void testTilingSpead() throws Exception {
        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);

        image.setFilename("slow.jpg");
        PictureTiles tiles = pts.getTiles(new BlobResource(image), 255, 255, 20, 0, 0, false);
        assertNotNull(tiles);
        assertFalse(tiles.getZoomfactor() == 0);
        // System.out.println("ExecTime="
        // + tiles.getInfo().get("JavaProcessExecTime"));

        image.setFilename("quick.jpg");
        PictureTiles tiles2 = pts.getTiles(new BlobResource(image), 255, 255, 20, 0, 0, false);
        assertNotNull(tiles2);
        assertFalse(tiles2.getZoomfactor() == 0);
        // System.out.println("ExecTime="
        // + tiles2.getInfo().get("JavaProcessExecTime"));
    }

    @Test
    public void testLazy() throws Exception {
        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);

        File wdir = new File(Environment.getDefault().getTemp(), "testMe");
        wdir.mkdirs();
        pts.setWorkingDirPath(wdir.getPath());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);

        image.setFilename("test.jpg");
        PictureTiles tiles = pts.getTiles(new BlobResource(image), 255, 255, 20, 0, 0, false);

        assertNotNull(tiles);

        Blob tile00 = tiles.getTile(0, 0);
        assertNotNull(tile00);

        Blob tile01 = tiles.getTile(0, 1);
        assertNotNull(tile01);

        Blob tile02 = tiles.getTile(0, 2);
        assertNotNull(tile02);

    }

    @Test
    public void testTilingWithShrink() throws Exception {
        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);

        PictureTiles tiles = pts.getTiles(new BlobResource(image), 255, 255, 3);

        assertNotNull(tiles);
        assertFalse(tiles.getZoomfactor() == 0);
    }

    @Test
    public void testTilingSimpleMagick() throws Exception {

        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);

        PictureTilingComponent.setDefaultTiler(new MagickTiler());

        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);

        PictureTiles tiles = pts.getTiles(new BlobResource(image), 255, 255, 5);

        assertNotNull(tiles);
        assertFalse(tiles.getZoomfactor() == 0);
    }

    /*
     * @Test public void testTilingBench() throws Exception { PictureTilingService pts =
     * Framework.getLocalService(PictureTilingService.class); assertNotNull(pts); benchTiler(pts, new GimpTiler());
     * benchTiler(pts, new MagickTiler()); }
     */

    @Test
    public void testMagick() throws Exception {
        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);
        PictureTilingComponent.setDefaultTiler(new MagickTiler());
        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);
        PictureTiles tiles = pts.getTiles(new BlobResource(image), 200, 200, 2);
        assertNotNull(tiles);

        tiles.getTile(0, 1);

    }

    @Test
    public void testMagick2() throws Exception {
        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);
        PictureTilingComponent.setDefaultTiler(new MagickTiler());
        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);
        PictureTiles tiles = pts.getTiles(new BlobResource(image), 200, 160, 2);
        assertNotNull(tiles);

        tiles.getTile(0, 1);

    }

    protected void benchTiler(PictureTilingService pts, PictureTiler tiler) throws Exception {

        PictureTilingComponent.setDefaultTiler(tiler);
        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);
        long t0 = System.currentTimeMillis();
        int nb = 0;

        for (int maxTiles = 2; maxTiles < 5; maxTiles = maxTiles + 2) {

            long tt0 = System.currentTimeMillis();

            PictureTiles tiles = pts.getTiles(new BlobResource(image), 200, 200, maxTiles);
            assertNotNull(tiles);

            int nxt = tiles.getXTiles();
            int nyt = tiles.getYTiles();
            int nbt = 0;
            for (int i = 0; i < nxt; i++) {
                for (int j = 0; j < nyt; j++) {
                    nbt++;
                    long t1 = System.currentTimeMillis();
                    Blob tile = tiles.getTile(i, j);
                    long t2 = System.currentTimeMillis();

                    // System.out.println("maxTile=" + maxTiles + " " + i + "-"
                    // + j + " :" + (t2 - t1));
                }
            }
            nb = nb + nbt;
            long tt1 = System.currentTimeMillis();
            // System.out.println("maxTile=" + maxTiles
            // + " total generation time :" + (tt1 - tt0));
            // System.out.println("speed " + (nbt + 0.0) / ((tt1 - tt0) / 1000));
        }

        long t3 = System.currentTimeMillis();

        // System.out.println("complete run for tiler : " + tiler.getName() +
        // " :" + (t3 - t0));
        // System.out.println("speed " + (nb + 0.0) / ((t3 - t0) / 1000));
    }

    @Test
    public void testGC() throws Exception {
        int gcRuns = PictureTilingCacheGCManager.getGCRuns();

        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);
        benchTiler(pts, new MagickTiler());
        testMagick2();
        testMagick2();
        long cacheSize = PictureTilingCacheGCManager.getCacheSizeInKBs();
        // System.out.println("CacheSize = " + cacheSize + "KB");
        assertTrue(cacheSize > 0);

        int reduceSize = 500;
        // System.out.println("performing GC with " + reduceSize
        // + " KB target reduction");
        PictureTilingCacheGCManager.doGC(reduceSize);

        int gcRuns2 = PictureTilingCacheGCManager.getGCRuns();

        assertEquals(1, gcRuns2 - gcRuns);

        long newCacheSize = PictureTilingCacheGCManager.getCacheSizeInKBs();
        // System.out.println("new cacheSize = " + newCacheSize + "KB");
        // System.out.println("effective delta = " + (cacheSize - newCacheSize)
        // + "KB");
        assertTrue(cacheSize - newCacheSize > reduceSize);

    }

    public static final String NXP21214 = "NXP-21214: GC runs are not always correct";

    @Test
    @Ignore(NXP21214)
    public void testGC2() throws Exception {
        int reduceSize = 500;
        int gcRuns = PictureTilingCacheGCManager.getGCRuns();
        int gcCalls = PictureTilingCacheGCManager.getGCCalls();
        PictureTilingComponent.endGC();

        String maxStr = PictureTilingComponent.getEnvValue(PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KEY,
                Long.toString(PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KB));
        PictureTilingComponent.setEnvValue(PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KEY,
                Integer.toString(reduceSize));

        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);
        benchTiler(pts, new MagickTiler());

        // System.out.println("Tiling run 1");
        testMagick2();
        // System.out.println("Tiling run 2");
        testMagick2();
        // System.out.println("Tiling run 3");
        testMagick2();

        long cacheSize = PictureTilingCacheGCManager.getCacheSizeInKBs();
        // System.out.println("CacheSize = " + cacheSize + "KB");
        assertTrue(cacheSize > 0);

        GCTask.setGCIntervalInMinutes(-100);
        PictureTilingComponent.startGC();

        // System.out.println("waiting for GC to run");
        Thread.sleep(600);

        int gcRuns2 = PictureTilingCacheGCManager.getGCRuns();
        int gcCalls2 = PictureTilingCacheGCManager.getGCCalls();

        int runs = gcRuns2 - gcRuns;
        int calls = gcCalls2 - gcCalls;
        // System.out.println("GC runs = " + runs);
        // System.out.println("GC calls = " + calls);

        assertTrue(runs > 0);
        assertTrue(calls > 2);

        PictureTilingComponent.endGC();

        long newCacheSize = PictureTilingCacheGCManager.getCacheSizeInKBs();
        // System.out.println("new cacheSize = " + newCacheSize + "KB");
        // System.out.println("effective delta = " + (cacheSize - newCacheSize)
        // + "KB");
        assertTrue(cacheSize - newCacheSize > reduceSize);

        PictureTilingComponent.setEnvValue(PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KEY, maxStr);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.pictures.tiles:OSGI-INF/pictures-tiles-contrib.xml")
    public void testParametersContrib() throws Exception {
        String cacheSize = PictureTilingComponent.getEnvValue(PictureTilingCacheGCManager.MAX_DISK_SPACE_USAGE_KEY,
                "ERROR");
        assertEquals("50000", cacheSize);
    }

    @Test
    public void testBorderTiles() throws Exception {
        PictureTilingService pts = Framework.getService(PictureTilingService.class);
        assertNotNull(pts);
        PictureTilingComponent.setDefaultTiler(new MagickTiler());
        File file = FileUtils.getResourceFileFromContext("chutes.jpg");
        Blob image = Blobs.createBlob(file);
        PictureTiles tiles = pts.getTiles(new BlobResource(image), 64, 64, 3);
        assertNotNull(tiles);

        tiles.getTile(2, 1);
        String tilePath = ((PictureTilesImpl) tiles).getTileFilePath(2, 1);
        ImageInfo info = ImageIdentifier.getInfo(tilePath);
        int width = info.getWidth();
        int height = info.getHeight();
        if (width == 63) {
            width = 64; // tolerance for rounding
        }
        assertEquals(64, width);
        assertEquals(64, height);

        tiles.getTile(2, 2);
        tilePath = ((PictureTilesImpl) tiles).getTileFilePath(2, 2);
        info = ImageIdentifier.getInfo(tilePath);
        width = info.getWidth();
        height = info.getHeight();
        if (width == 63) {
            width = 64; // tolerance for rounding
        }
        if (height == 15) {
            height = 16; // tolerance for rounding
        }
        assertEquals(64, width);
        assertEquals(16, height);
    }
}

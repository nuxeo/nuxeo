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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.pictures.tiles.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageConverter;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilesImpl;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.ImageResource;
import org.nuxeo.ecm.platform.pictures.tiles.magick.tiler.MagickTiler;
import org.nuxeo.ecm.platform.pictures.tiles.tilers.PictureTiler;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component that expose the PictureTilingService interface. Also exposes the configuration Extension Point
 *
 * @author tiry
 */
public class PictureTilingComponent extends DefaultComponent implements PictureTilingService {

    public static final String ENV_PARAMETERS_EP = "environment";

    public static final String BLOB_PROPERTY_EP = "blobProperties";

    public static final String IMAGES_TO_CONVERT_EP = "imagesToConvert";

    protected static Map<String, PictureTilingCacheInfo> cache = new HashMap<>();

    protected static List<String> inprocessTiles = Collections.synchronizedList(new ArrayList<>());

    protected static PictureTiler defaultTiler = new MagickTiler();

    protected static Map<String, String> envParameters = new HashMap<>();

    protected Map<String, String> blobProperties = new HashMap<>();

    protected List<ImageToConvertDescriptor> imagesToConvert = new ArrayList<>();

    protected static Thread gcThread;

    private String workingDirPath = defaultWorkingDirPath();

    private static final Log log = LogFactory.getLog(PictureTilingComponent.class);

    @Override
    public void activate(ComponentContext context) {
        defaultTiler = new MagickTiler();
        startGC();
    }

    public static void startGC() {
        if (!GCTask.GCEnabled) {
            GCTask.GCEnabled = true;
            log.debug("PictureTilingComponent activated starting GC thread");
            gcThread = new Thread(new GCTask(), "Nuxeo-Tiling-GC");
            gcThread.setDaemon(true);
            gcThread.start();
            log.debug("GC Thread started");
        } else {
            log.debug("GC Thread is already started");
        }
    }

    public static void endGC() {
        if (GCTask.GCEnabled) {
            GCTask.GCEnabled = false;
            log.debug("Stopping GC Thread");
            gcThread.interrupt();
        } else {
            log.debug("GC Thread is already stopped");
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        endGC();
    }

    public static Map<String, PictureTilingCacheInfo> getCache() {
        return cache;
    }

    protected String getWorkingDirPath() {
        return workingDirPath;
    }

    protected String defaultWorkingDirPath() {
        String defaultPath = new File(Environment.getDefault().getData(), "nuxeo-tiling-cache").getAbsolutePath();
        String path = getEnvValue("WorkingDirPath", defaultPath);
        return normalizeWorkingDirPath(path);
    }

    protected String normalizeWorkingDirPath(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
        path = dir.getAbsolutePath();
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }

    @Override
    public void setWorkingDirPath(String path) {
        workingDirPath = normalizeWorkingDirPath(path);
    }

    protected String getWorkingDirPathForRessource(ImageResource resource) {
        String pathForBlob = getWorkingDirPath();
        String digest = resource.getHash();
        pathForBlob = pathForBlob + digest + File.separator;
        log.debug("WorkingDirPath for resource=" + pathForBlob);
        File wdir = new File(pathForBlob);
        if (!wdir.exists()) {
            wdir.mkdir();
        }
        return pathForBlob;
    }

    @Override
    public PictureTiles getTiles(ImageResource resource, int tileWidth, int tileHeight, int maxTiles) {
        return getTiles(resource, tileWidth, tileHeight, maxTiles, 0, 0, false);
    }

    @Override
    public PictureTiles completeTiles(PictureTiles existingTiles, int xCenter, int yCenter) {

        String outputDirPath = existingTiles.getTilesPath();

        long lastModificationTime = Long.parseLong(
                existingTiles.getInfo().get(PictureTilesImpl.LAST_MODIFICATION_DATE_KEY));
        return computeTiles(existingTiles.getSourceImageInfo(), outputDirPath, existingTiles.getTilesWidth(),
                existingTiles.getTilesHeight(), existingTiles.getMaxTiles(), xCenter, yCenter, lastModificationTime,
                false);
    }

    @Override
    public PictureTiles getTiles(ImageResource resource, int tileWidth, int tileHeight, int maxTiles, int xCenter,
            int yCenter, boolean fullGeneration) {

        log.debug("enter getTiles");
        String cacheKey = resource.getHash();

        if (defaultTiler.needsSync()) {
            // some tiler implementation may generate several tiles at once
            // in order to be efficient this requires synchronization
            while (inprocessTiles.contains(cacheKey)) {
                try {
                    log.debug("Waiting for tiler sync");
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    ExceptionUtils.checkInterrupt(e);
                }
            }
        }

        PictureTiles tiles = getTilesWithSync(resource, tileWidth, tileHeight, maxTiles, xCenter, yCenter,
                fullGeneration);
        inprocessTiles.remove(cacheKey);

        return tiles;
    }

    protected PictureTiles getTilesWithSync(ImageResource resource, int tileWidth, int tileHeight, int maxTiles,
            int xCenter, int yCenter, boolean fullGeneration) {

        String cacheKey = resource.getHash();
        String inputFilePath;
        PictureTilingCacheInfo cacheInfo;

        if (cache.containsKey(cacheKey)) {
            cacheInfo = cache.get(cacheKey);

            PictureTiles pt = cacheInfo.getCachedPictureTiles(tileWidth, tileHeight, maxTiles);

            if ((pt != null) && (pt.isTileComputed(xCenter, yCenter))) {
                return pt;
            }

            inputFilePath = cacheInfo.getOriginalPicturePath();
        } else {
            String wdirPath = getWorkingDirPathForRessource(resource);
            inputFilePath = wdirPath;
            Blob blob = resource.getBlob();
            inputFilePath += Integer.toString(blob.hashCode()) + ".";
            if (blob.getFilename() != null) {
                inputFilePath += FilenameUtils.getExtension(blob.getFilename());
            } else {
                inputFilePath += "img";
            }

            if (needToConvert(blob)) {
                inputFilePath = FilenameUtils.removeExtension(inputFilePath) + ".jpg";
            }

            File inputFile = new File(inputFilePath);

            if (!inputFile.exists()) {
                try {
                    // create the empty file ASAP to avoid concurrent transfer
                    // and conversions
                    if (inputFile.createNewFile()) {
                        transferBlob(blob, inputFile);
                    }
                } catch (IOException e) {
                    String msg = String.format(
                            "Unable to transfer blob to file at '%s', " + "working directory path: '%s'", inputFilePath,
                            wdirPath);
                    log.error(msg, e);
                    throw new NuxeoException(msg, e);
                }
                inputFile = new File(inputFilePath);
            } else {
                while (System.currentTimeMillis() - inputFile.lastModified() < 200) {
                    try {
                        log.debug("Waiting concurrent convert / dump");
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        ExceptionUtils.checkInterrupt(e);
                    }
                }

            }
            try {
                cacheInfo = new PictureTilingCacheInfo(cacheKey, wdirPath, inputFilePath);
                cache.put(cacheKey, cacheInfo);
            } catch (CommandNotAvailable | CommandException e) {
                throw new NuxeoException(e);
            }

        }

        // compute output dir
        String outDirPath = cacheInfo.getTilingDir(tileWidth, tileHeight, maxTiles);

        // try to see if a shrinked image can be used
        ImageInfo bestImageInfo = cacheInfo.getBestSourceImage(tileWidth, tileHeight, maxTiles);

        inputFilePath = bestImageInfo.getFilePath();
        log.debug("input source image path for tile computation=" + inputFilePath);

        long lastModificationTime = resource.getModificationDate().getTimeInMillis();
        PictureTiles tiles = computeTiles(bestImageInfo, outDirPath, tileWidth, tileHeight, maxTiles, xCenter, yCenter,
                lastModificationTime, fullGeneration);

        tiles.getInfo().put(PictureTilesImpl.MAX_TILES_KEY, Integer.toString(maxTiles));
        tiles.getInfo().put(PictureTilesImpl.TILES_WIDTH_KEY, Integer.toString(tileWidth));
        tiles.getInfo().put(PictureTilesImpl.TILES_HEIGHT_KEY, Integer.toString(tileHeight));
        String lastModificationDate = Long.toString(lastModificationTime);
        tiles.getInfo().put(PictureTilesImpl.LAST_MODIFICATION_DATE_KEY, lastModificationDate);
        tiles.setCacheKey(cacheKey);
        tiles.setSourceImageInfo(bestImageInfo);
        tiles.setOriginalImageInfo(cacheInfo.getOriginalPictureInfos());

        cacheInfo.addPictureTilesToCache(tiles);
        return tiles;
    }

    protected void transferBlob(Blob blob, File file) throws IOException {
        if (needToConvert(blob)) {
            transferAndConvert(blob, file);
        } else {
            blob.transferTo(file);
        }
    }

    protected boolean needToConvert(Blob blob) {
        for (ImageToConvertDescriptor desc : imagesToConvert) {
            String extension = getExtension(blob);
            if (desc.getMimeType().equalsIgnoreCase(blob.getMimeType())
                    || extension.equalsIgnoreCase(desc.getExtension())) {
                return true;
            }
        }
        return false;
    }

    protected String getExtension(Blob blob) {
        String filename = blob.getFilename();
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }

        return filename.substring(dotIndex + 1);
    }

    protected void transferAndConvert(Blob blob, File file) throws IOException {
        File tmpFile = new File(file.getAbsolutePath() + ".tmp");
        blob.transferTo(tmpFile);
        try {
            ImageConverter.convert(tmpFile.getAbsolutePath(), file.getAbsolutePath());
        } catch (CommandNotAvailable | CommandException e) {
            throw new IOException(e);
        }

        tmpFile.delete();
    }

    protected PictureTiles computeTiles(ImageInfo input, String outputDirPath, int tileWidth, int tileHeight,
            int maxTiles, int xCenter, int yCenter, long lastModificationTime, boolean fullGeneration) {

        PictureTiler pt = getDefaultTiler();
        return pt.getTilesFromFile(input, outputDirPath, tileWidth, tileHeight, maxTiles, xCenter, yCenter,
                lastModificationTime, fullGeneration);
    }

    protected PictureTiler getDefaultTiler() {
        return defaultTiler;
    }

    // tests
    public static void setDefaultTiler(PictureTiler tiler) {
        defaultTiler = tiler;
    }

    // ****************************************
    // Env setting management

    public static Map<String, String> getEnv() {
        return envParameters;
    }

    public static String getEnvValue(String paramName) {
        if (envParameters == null) {
            return null;
        }
        return envParameters.get(paramName);
    }

    public static String getEnvValue(String paramName, String defaultValue) {
        String value = getEnvValue(paramName);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    public static void setEnvValue(String paramName, String paramValue) {
        envParameters.put(paramName, paramValue);
    }

    // Blob properties management
    @Override
    public Map<String, String> getBlobProperties() {
        return blobProperties;
    }

    @Override
    public String getBlobProperty(String docType) {
        return blobProperties.get(docType);
    }

    @Override
    public String getBlobProperty(String docType, String defaultValue) {
        String property = blobProperties.get(docType);
        if (property == null) {
            return defaultValue;
        }
        return property;
    }

    // EP management

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (ENV_PARAMETERS_EP.equals(extensionPoint)) {
            TilingConfigurationDescriptor desc = (TilingConfigurationDescriptor) contribution;
            envParameters.putAll(desc.getParameters());
            workingDirPath = defaultWorkingDirPath();
        } else if (BLOB_PROPERTY_EP.equals(extensionPoint)) {
            TilingBlobPropertyDescriptor desc = (TilingBlobPropertyDescriptor) contribution;
            blobProperties.putAll(desc.getBlobProperties());
        } else if (IMAGES_TO_CONVERT_EP.equals(extensionPoint)) {
            ImageToConvertDescriptor desc = (ImageToConvertDescriptor) contribution;
            imagesToConvert.add(desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // TODO
    }

    @Override
    public void removeCacheEntry(ImageResource resource) {
        if (cache.containsKey(resource.getHash())) {
            PictureTilingCacheInfo cacheInfo = cache.remove(resource.getHash());
            cacheInfo.cleanUp();
        }
    }

}

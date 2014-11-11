/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageConverter;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilesImpl;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.BlobResource;
import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.ImageResource;
import org.nuxeo.ecm.platform.pictures.tiles.gimp.tiler.GimpTiler;
import org.nuxeo.ecm.platform.pictures.tiles.magick.tiler.MagickTiler;
import org.nuxeo.ecm.platform.pictures.tiles.tilers.PictureTiler;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component that expose the PictureTilingService interface. Also
 * exposes the configuration Extension Point
 *
 * @author tiry
 */
public class PictureTilingComponent extends DefaultComponent implements
        PictureTilingService {

    public static final String ENV_PARAMETERS_EP = "environment";

    public static final String BLOB_PROPERTY_EP = "blobProperties";

    public static final String IMAGES_TO_CONVERT_EP = "imagesToConvert";

    protected static Map<String, PictureTilingCacheInfo> cache = new HashMap<String, PictureTilingCacheInfo>();

    protected static List<String> inprocessTiles = Collections.synchronizedList(new ArrayList<String>());

    protected static PictureTiler defaultTiler = new MagickTiler();

    protected static List<PictureTiler> availableTilers = new ArrayList<PictureTiler>();

    protected static Map<String, String> envParameters = new HashMap<String, String>();

    protected Map<String, String> blobProperties = new HashMap<String, String>();

    protected List<ImageToConvertDescriptor> imagesToConvert = new ArrayList<ImageToConvertDescriptor>();

    protected static Thread gcThread;

    private String workingDirPath = defaultWorkingDirPath();

    private static final Log log = LogFactory.getLog(PictureTilingComponent.class);

    @Override
    public void activate(ComponentContext context) throws Exception {
        defaultTiler = new MagickTiler();
        availableTilers.add(defaultTiler);
        availableTilers.add(new GimpTiler());
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

    public void deactivate(ComponentContext context) throws Exception {
        endGC();
    }

    public static Map<String, PictureTilingCacheInfo> getCache() {
        return cache;
    }

    protected String getWorkingDirPath() {
        return workingDirPath;
    }

    protected String defaultWorkingDirPath() {
        String defaultPath = new File(Environment.getDefault().getTemp(),
                "nuxeo-tiling-cache").getAbsolutePath();
        String path = getEnvValue("WorkingDirPath",
                defaultPath);
        return normalizeWorkingDirPath(path);
    }

    protected String normalizeWorkingDirPath(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
        path = dir.getAbsolutePath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path;
    }

    public void setWorkingDirPath(String path) {
        workingDirPath = normalizeWorkingDirPath(path);
    }

    protected String getWorkingDirPathForRessource(ImageResource resource) {

        String pathForBlob = getWorkingDirPath();

        String digest;
        try {
            digest = resource.getHash();
        } catch (ClientException e) {
            digest = "tmp" + System.currentTimeMillis();
        }

        pathForBlob = pathForBlob + digest + "/";

        log.debug("WorkingDirPath for resource=" + pathForBlob);

        File wdir = new File(pathForBlob);
        if (!wdir.exists()) {
            wdir.mkdir();
        }

        return pathForBlob;
    }

    @Deprecated
    public PictureTiles getTilesFromBlob(Blob blob, int tileWidth,
            int tileHeight, int maxTiles) throws ClientException {
        return getTilesFromBlob(blob, tileWidth, tileHeight, maxTiles, 0, 0,
                false);
    }

    public PictureTiles getTiles(ImageResource resource, int tileWidth,
            int tileHeight, int maxTiles) throws ClientException {
        return getTiles(resource, tileWidth, tileHeight, maxTiles, 0, 0, false);
    }

    public PictureTiles completeTiles(PictureTiles existingTiles, int xCenter,
            int yCenter) throws ClientException {

        String outputDirPath = existingTiles.getTilesPath();

        long lastModificationTime = Long.parseLong(existingTiles.getInfo().get(
                PictureTilesImpl.LAST_MODIFICATION_DATE_KEY));
        return computeTiles(existingTiles.getSourceImageInfo(), outputDirPath,
                existingTiles.getTilesWidth(), existingTiles.getTilesHeight(),
                existingTiles.getMaxTiles(), xCenter, yCenter,
                lastModificationTime, false);
    }

    @Deprecated
    public PictureTiles getTilesFromBlob(Blob blob, int tileWidth,
            int tileHeight, int maxTiles, int xCenter, int yCenter,
            boolean fullGeneration) throws ClientException {

        ImageResource resource = new BlobResource(blob);
        return getTiles(resource, tileWidth, tileHeight, maxTiles, xCenter,
                yCenter, fullGeneration);
    }

    public PictureTiles getTiles(ImageResource resource, int tileWidth,
            int tileHeight, int maxTiles, int xCenter, int yCenter,
            boolean fullGeneration) throws ClientException {

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
                    throw new ClientException(
                            "Error while waiting for another tile processing on the same resource",
                            e);
                }
            }
        }

        PictureTiles tiles = getTilesWithSync(resource, tileWidth, tileHeight,
                maxTiles, xCenter, yCenter, fullGeneration);
        inprocessTiles.remove(cacheKey);

        return tiles;
    }

    protected PictureTiles getTilesWithSync(ImageResource resource,
            int tileWidth, int tileHeight, int maxTiles, int xCenter,
            int yCenter, boolean fullGeneration) throws ClientException {

        String cacheKey = resource.getHash();
        String inputFilePath;
        PictureTilingCacheInfo cacheInfo;

        if (cache.containsKey(cacheKey)) {
            cacheInfo = cache.get(cacheKey);

            PictureTiles pt = cacheInfo.getCachedPictureTiles(tileWidth,
                    tileHeight, maxTiles);

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
                inputFilePath = inputFilePath.replaceFirst("\\..*", ".jpg");
            }

            File inputFile = new File(inputFilePath);

            if (!inputFile.exists()) {
                try {
                    // create the empty file ASAP to avoid concurrent transfer
                    // and conversions
                    if (inputFile.createNewFile()) {
                        transferBlob(blob, inputFile);
                    }
                } catch (Exception e) {
                    String msg = String.format(
                            "Unable to transfer blob to file at '%s', "
                                    + "working directory path: '%s'",
                            inputFilePath, wdirPath);
                    log.error(msg, e);
                    throw new ClientException(msg, e);
                }
                inputFile = new File(inputFilePath);
            } else {
                while (System.currentTimeMillis() - inputFile.lastModified() < 200) {
                    try {
                        log.debug("Waiting concurrent convert / dump");
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new ClientException(
                                "Error while waiting for another converting"
                                        + " on the same resource", e);
                    }
                }

            }
            try {
                cacheInfo = new PictureTilingCacheInfo(cacheKey, wdirPath,
                        inputFilePath);
                cache.put(cacheKey, cacheInfo);
            } catch (Exception e) {
                throw new ClientException(e);
            }

        }

        // compute output dir
        String outDirPath = cacheInfo.getTilingDir(tileWidth, tileHeight,
                maxTiles);

        // try to see if a shrinked image can be used
        ImageInfo bestImageInfo = cacheInfo.getBestSourceImage(tileWidth,
                tileHeight, maxTiles);

        inputFilePath = bestImageInfo.getFilePath();
        log.debug("input source image path for tile computation="
                + inputFilePath);

        long lastModificationTime = resource.getModificationDate().getTimeInMillis();
        PictureTiles tiles = computeTiles(bestImageInfo, outDirPath, tileWidth,
                tileHeight, maxTiles, xCenter, yCenter, lastModificationTime,
                fullGeneration);

        tiles.getInfo().put(PictureTilesImpl.MAX_TILES_KEY,
                Integer.toString(maxTiles));
        tiles.getInfo().put(PictureTilesImpl.TILES_WIDTH_KEY,
                Integer.toString(tileWidth));
        tiles.getInfo().put(PictureTilesImpl.TILES_HEIGHT_KEY,
                Integer.toString(tileHeight));
        String lastModificationDate = Long.toString(lastModificationTime);
        tiles.getInfo().put(PictureTilesImpl.LAST_MODIFICATION_DATE_KEY,
                lastModificationDate);
        tiles.setCacheKey(cacheKey);
        tiles.setSourceImageInfo(bestImageInfo);
        tiles.setOriginalImageInfo(cacheInfo.getOriginalPictureInfos());

        cacheInfo.addPictureTilesToCache(tiles);
        return tiles;
    }

    protected void transferBlob(Blob blob, File file) throws Exception {
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

    protected void transferAndConvert(Blob blob, File file) throws Exception {
        File tmpFile = new File(file.getAbsolutePath() + ".tmp");
        blob.transferTo(tmpFile);
        ImageConverter.convert(tmpFile.getAbsolutePath(),
                file.getAbsolutePath());

        tmpFile.delete();
    }

    protected PictureTiles computeTiles(ImageInfo input, String outputDirPath,
            int tileWidth, int tileHeight, int maxTiles, int xCenter,
            int yCenter, long lastModificationTime, boolean fullGeneration)
            throws ClientException {

        PictureTiler pt = getDefaultTiler();
        return pt.getTilesFromFile(input, outputDirPath, tileWidth, tileHeight,
                maxTiles, xCenter, yCenter, lastModificationTime,
                fullGeneration);
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
    public Map<String, String> getBlobProperties() {
        return blobProperties;
    }

    public String getBlobProperty(String docType) {
        return blobProperties.get(docType);
    }

    public String getBlobProperty(String docType, String defaultValue) {
        String property = blobProperties.get(docType);
        if (property == null) {
            return defaultValue;
        }
        return property;
    }

    // EP management

    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

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

    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        // TODO
    }

    public void removeCacheEntry(ImageResource resource) throws ClientException {
        if (cache.containsKey(resource.getHash())) {
            PictureTilingCacheInfo cacheInfo = cache.remove(resource.getHash());
            cacheInfo.cleanUp();
        }
    }

}

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
package org.nuxeo.ecm.platform.threed;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.threed.service.RenderView;
import org.nuxeo.ecm.platform.threed.service.ThreeDService;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.platform.threed.ThreeDInfo.*;

/**
 * Helper class to take out renders and list of {@link TransmissionThreeD} form batch conversion
 *
 * @since 8.4
 */
public class BatchConverterHelper {

    public static final String WIDTH = "width";

    public static final String HEIGHT = "height";

    public static final String DEPTH = "depth";

    public static final String FORMAT = "format";

    private static final Log log = LogFactory.getLog(BatchConverterHelper.class);

    private BatchConverterHelper() {
    }

    protected static final BlobHolder convertTexture(BlobHolder resource, Integer percentage, String maxSize) {
        ImagingService imagingService = Framework.getService(ImagingService.class);
        float percScale = 1.0f;
        if (percentage != null) {
            percScale = (float) ((float) percentage / 100.0);
        }
        float maxScale = 1.0f;
        Map<String, Serializable> infoTexture = resource.getProperties();
        if (maxSize != null) {

            String[] size = maxSize.split("x");

            int width = Integer.parseInt(size[0]);
            int height = Integer.parseInt(size[1]);

            // calculate max size scale
            maxScale = Math.min((float) width / (int) infoTexture.get(WIDTH),
                    (float) height / (int) infoTexture.get(HEIGHT));
        }
        if (percScale >= 1.0 && maxScale >= 1.0) {
            return resource;
        }
        float scale = Math.min(maxScale, percScale);

        Map<String, Serializable> lodInfoTexture = new HashMap<>();
        lodInfoTexture.put(WIDTH, Math.round((int) infoTexture.get(WIDTH) * scale));
        lodInfoTexture.put(HEIGHT, Math.round((int) infoTexture.get(HEIGHT) * scale));
        lodInfoTexture.put(DEPTH, infoTexture.get(DEPTH));
        lodInfoTexture.put(FORMAT, infoTexture.get(FORMAT));
        Blob lodBlob = imagingService.resize(resource.getBlob(), (String) lodInfoTexture.get(FORMAT),
                (int) lodInfoTexture.get(WIDTH), (int) lodInfoTexture.get(HEIGHT), (int) lodInfoTexture.get(DEPTH));
        lodBlob.setFilename(resource.getBlob().getFilename());
        return new SimpleBlobHolderWithProperties(lodBlob, lodInfoTexture);
    }

    public static final List<TransmissionThreeD> getTransmissions(BlobHolder batch, List<BlobHolder> resources) {
        ThreeDService threeDService = Framework.getService(ThreeDService.class);

        List<Blob> blobs = batch.getBlobs();

        Map<String, Integer> lodIdIndexes = (Map<String, Integer>) batch.getProperty("lodIdIndexes");

        // start with automatic LODs so we get the transmission 3Ds correctly ordered
        return threeDService.getAutomaticLODs().stream().map(automaticLOD -> {
            Integer index = lodIdIndexes.get(automaticLOD.getId());

            if (index != null) {
                Blob dae = blobs.get(index);
                // resize texture accordingly with LOD text params
                List<BlobHolder> lodResources = resources.stream()
                                                         .map(resource -> convertTexture(resource,
                                                                 automaticLOD.getPercTex(), automaticLOD.getMaxTex()))
                                                         .collect(Collectors.toList());
                List<Blob> lodResourceBlobs = lodResources.stream()
                                                          .map(BlobHolder::getBlob)
                                                          .collect(Collectors.toList());
                Integer idx = ((Map<String, Integer>) batch.getProperty("infoIndexes")).get(automaticLOD.getId());
                if (idx == null) {
                    return null;
                }
                Blob infoBlob = batch.getBlobs().get(idx);
                ThreeDInfo info = null;
                try {
                    info = getInfo(infoBlob, lodResources);
                } catch (IOException e) {
                    log.warn(e);
                    info = null;
                }
                // create transmission 3D from blob and automatic LOD
                return new TransmissionThreeD(dae, lodResourceBlobs, info, automaticLOD.getPercPoly(),
                        automaticLOD.getMaxPoly(), automaticLOD.getPercTex(), automaticLOD.getMaxTex(),
                        automaticLOD.getName());
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static final ThreeDInfo getMainInfo(BlobHolder batch, List<BlobHolder> resources) {
        Integer idx = ((Map<String, Integer>) batch.getProperty("infoIndexes")).get("default");
        if (idx == null) {
            return null;
        }
        Blob infoBlob = batch.getBlobs().get(idx);
        ThreeDInfo info;
        try {
            info = getInfo(infoBlob, resources);
        } catch (IOException e) {
            log.warn(e);
            info = null;

        }
        return info;
    }

    protected static final ThreeDInfo getInfo(Blob blob, List<BlobHolder> resources) throws IOException {
        Map<String, Serializable> infoMap = convertToInfo(blob);

        int maxWidth = resources.stream().mapToInt(resource -> (Integer) resource.getProperty(WIDTH)).max().orElse(0);
        int maxHeight = resources.stream().mapToInt(resource -> (Integer) resource.getProperty(HEIGHT)).max().orElse(0);
        long resourcesSize = resources.stream()
                                      .mapToLong(resource -> resource.getBlob().getFile().length())
                                      .sum();

        infoMap.put(TEXTURE_LOD_SUCCESS, Boolean.TRUE);
        infoMap.put(TEXTURES_MAX_DIMENSION, String.valueOf(maxWidth) + "x" + String.valueOf(maxHeight));
        infoMap.put(TEXTURES_SIZE, resourcesSize);
        return new ThreeDInfo(infoMap);
    }

    protected static final Map<String, Serializable> convertToInfo(Blob blob) throws IOException {
        Map<String, Serializable> info;
        Map<String, Serializable> infoGlobal;
        Map<String, Serializable> geomInfo = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        info = mapper.readValue(blob.getFile(), Map.class);
        if (info.get("global") != null && info.get("global") instanceof Map) {
            infoGlobal = (Map<String, Serializable>) info.get("global");
            geomInfo.put(GEOMETRY_LOD_SUCCESS, infoGlobal.get(GEOMETRY_LOD_SUCCESS));
            geomInfo.put(NON_MANIFOLD_POLYGONS, ((Integer) infoGlobal.get(NON_MANIFOLD_POLYGONS)).longValue());
            geomInfo.put(NON_MANIFOLD_EDGES, ((Integer) infoGlobal.get(NON_MANIFOLD_EDGES)).longValue());
            geomInfo.put(NON_MANIFOLD_VERTICES, ((Integer) infoGlobal.get(NON_MANIFOLD_VERTICES)).longValue());
            geomInfo.put(POLYGONS, ((Integer) infoGlobal.get(POLYGONS)).longValue());
            geomInfo.put(EDGES, ((Integer) infoGlobal.get(EDGES)).longValue());
            geomInfo.put(VERTICES, ((Integer) infoGlobal.get(VERTICES)).longValue());
            geomInfo.put(POSITION_X, infoGlobal.get(POSITION_X));
            geomInfo.put(POSITION_Y, infoGlobal.get(POSITION_Y));
            geomInfo.put(POSITION_Z, infoGlobal.get(POSITION_Z));
            geomInfo.put(DIMENSION_X, infoGlobal.get(DIMENSION_X));
            geomInfo.put(DIMENSION_Y, infoGlobal.get(DIMENSION_Y));
            geomInfo.put(DIMENSION_Z, infoGlobal.get(DIMENSION_Z));
        }

        return geomInfo;
    }

    public static final List<BlobHolder> getResources(BlobHolder batch) {
        List<Blob> blobs = batch.getBlobs();
        return ((List<Integer>) batch.getProperty("resourceIndexes")).stream().map(blobs::get).map(resource -> {
            Map<String, Serializable> infoTexture = new HashMap<>();
            ImagingService imagingService = Framework.getService(ImagingService.class);
            ImageInfo imageInfo = imagingService.getImageInfo(resource);
            if (imageInfo == null) {
                return null;
            }
            infoTexture.put(WIDTH, imageInfo.getWidth());
            infoTexture.put(HEIGHT, imageInfo.getHeight());
            infoTexture.put(FORMAT, imageInfo.getFormat());
            infoTexture.put(DEPTH, imageInfo.getDepth());

            return new SimpleBlobHolderWithProperties(resource, infoTexture);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected static final Blob createThumbnail(Blob render) {
        ImagingService imagingService = Framework.getService(ImagingService.class);
        ImageInfo imageInfo = imagingService.getImageInfo(render);

        // calculate thumbnail size
        float scale = Math.min((float) AbstractPictureAdapter.SMALL_SIZE / imageInfo.getWidth(),
                (float) AbstractPictureAdapter.SMALL_SIZE / imageInfo.getHeight());

        return imagingService.resize(render, imageInfo.getFormat(), Math.round(imageInfo.getWidth() * scale),
                Math.round(imageInfo.getHeight() * scale), imageInfo.getDepth());
    }

    public static final List<ThreeDRenderView> getRenders(BlobHolder batch) {
        List<Blob> allBlobs = batch.getBlobs();
        List<Blob> blobs = allBlobs.subList((int) batch.getProperty("renderStartIndex"), allBlobs.size());
        ThreeDService threeDService = Framework.getService(ThreeDService.class);

        Collection<RenderView> orderedRV = new ArrayList<>(threeDService.getAutomaticRenderViews());
        Collection<RenderView> remainingRV = new ArrayList<>(threeDService.getAvailableRenderViews());
        remainingRV.removeAll(orderedRV);
        orderedRV.addAll(remainingRV);

        return orderedRV.stream().map(renderView -> {
            Blob png = blobs.stream().filter(blob -> {
                String[] fileNameArray = FilenameUtils.getBaseName(blob.getFilename()).split("-");
                return renderView.getId().equals(fileNameArray[1]);
            }).findFirst().orElse(null);

            if (png == null) {
                return null;
            }

            return new ThreeDRenderView(renderView.getName(), png, createThumbnail(png), renderView.getAzimuth(),
                    renderView.getZenith());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}

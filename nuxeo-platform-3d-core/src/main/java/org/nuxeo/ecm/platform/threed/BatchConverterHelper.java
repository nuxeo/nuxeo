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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.threed.service.RenderView;
import org.nuxeo.ecm.platform.threed.service.ThreeDService;
import org.nuxeo.runtime.api.Framework;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class to take out renders and list of {@link TransmissionThreeD} form batch conversion
 *
 * @since 8.4
 */
public class BatchConverterHelper {

    private BatchConverterHelper() {
    }

    protected static final Blob convertTexture(Blob resource, Integer percentage, String maxSize) {
        ImagingService imagingService = Framework.getService(ImagingService.class);
        ImageInfo imageInfo = imagingService.getImageInfo(resource);
        if (imageInfo == null) {
            return resource;
        }
        float percScale = 1.0f;
        if (percentage != null) {
            percScale = (float) ((float) percentage / 100.0);
        }
        float maxScale = 1.0f;
        if (maxSize != null) {

            String[] size = maxSize.split("x");

            int width = Integer.parseInt(size[0]);
            int height = Integer.parseInt(size[1]);

            // calculate max size scale
            maxScale = Math.min((float) width / imageInfo.getWidth(), (float) height / imageInfo.getHeight());
        }
        if (percScale >= 1.0 && maxScale >= 1.0) {
            return resource;
        }

        float scale = Math.min(maxScale, percScale);

        Blob lodResource = imagingService.resize(resource, imageInfo.getFormat(),
                Math.round(imageInfo.getWidth() * scale), Math.round(imageInfo.getHeight() * scale),
                imageInfo.getDepth());
        lodResource.setFilename(resource.getFilename());
        return lodResource;
    }

    public static final List<TransmissionThreeD> getTransmissons(BlobHolder batch) {
        ThreeDService threeDService = Framework.getService(ThreeDService.class);

        List<Blob> blobs = batch.getBlobs();
        Map<String, Integer> lodIdIndexes = (Map<String, Integer>) batch.getProperty("lodIdIndexes");
        List<Blob> resources = ((List<Integer>) batch.getProperty("resourceIndexes")).stream().map(blobs::get).collect(
                Collectors.toList());

        // start with automatic LODs so we get the transmission 3Ds correctly ordered
        return threeDService.getAutomaticLODs().stream().map(automaticLOD -> {
            Integer index = lodIdIndexes.get(automaticLOD.getId());

            if (index != null) {
                Blob dae = blobs.get(index);
                // resize texture accordingly with LOD text params
                List<Blob> lodResources = resources.stream()
                                                   .map(resource -> convertTexture(resource, automaticLOD.getPercTex(),
                                                           automaticLOD.getMaxTex()))
                                                   .collect(Collectors.toList());

                // create transmission 3D from blob and automatic LOD
                return new TransmissionThreeD(dae, lodResources, null, automaticLOD.getPercPoly(),
                        automaticLOD.getMaxPoly(), automaticLOD.getPercTex(), automaticLOD.getMaxTex(),
                        automaticLOD.getName());
            }
            return null;
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

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
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter;
import org.nuxeo.ecm.platform.threed.service.AutomaticLOD;
import org.nuxeo.ecm.platform.threed.service.RenderView;
import org.nuxeo.ecm.platform.threed.service.ThreeDService;
import org.nuxeo.runtime.api.Framework;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Helper class to take out renders and list of {@link TransmissionThreeD} form batch conversion
 *
 * @since 8.4
 */
public class BatchConverterHelper {

    private BatchConverterHelper() {
    }

    public static final List<TransmissionThreeD> getTransmissons(Collection<Blob> blobs) {
        ThreeDService threeDService = Framework.getService(ThreeDService.class);
        return blobs.stream().filter(blob -> "dae".equals(FilenameUtils.getExtension(blob.getFilename()))).map(blob -> {
            String[] fileNameArray = FilenameUtils.getBaseName(blob.getFilename()).split("-");
            if (fileNameArray.length != 4) {
                return null;
            }
            String id = fileNameArray[1];

            if (id.equals("original")) {
                int lod = Integer.valueOf(fileNameArray[2]);
                long maxPoly = Long.valueOf(fileNameArray[3]);
                return new TransmissionThreeD(blob, lod, maxPoly, id);
            } else {
                AutomaticLOD currentALOD = threeDService.getAutomaticLOD(id);
                if (currentALOD == null) {
                    return null;
                }
                return new TransmissionThreeD(blob, currentALOD.getPercentage(), currentALOD.getMaxPoly(),
                    currentALOD.getName());
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static final List<ThreeDRenderView> getRenders(Collection<Blob> blobs) {
        ThreeDService threeDService = Framework.getService(ThreeDService.class);
        return blobs.stream().filter(blob -> "png".equals(FilenameUtils.getExtension(blob.getFilename()))).map(blob -> {
            String[] fileNameArray = FilenameUtils.getBaseName(blob.getFilename()).split("-");
            if (fileNameArray.length != 7) {
                return null;
            }
            String id = fileNameArray[1];
            RenderView currentRV = threeDService.getRenderView(id);
            if (currentRV == null) {
                return null;
            }
            ImagingService imagingService = Framework.getService(ImagingService.class);
            ImageInfo imageInfo = imagingService.getImageInfo(blob);

            // calculate thumbnail size
            float scale = Math.min((float) AbstractPictureAdapter.SMALL_SIZE / imageInfo.getWidth(),
                    (float) AbstractPictureAdapter.SMALL_SIZE / imageInfo.getHeight());

            Blob thumbnail = imagingService.resize(blob, imageInfo.getFormat(),
                    Math.round(imageInfo.getWidth() * scale), Math.round(imageInfo.getHeight() * scale),
                    imageInfo.getDepth());
            return new ThreeDRenderView(currentRV.getName(), blob, thumbnail, currentRV.getAzimuth(),
                    currentRV.getZenith());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}

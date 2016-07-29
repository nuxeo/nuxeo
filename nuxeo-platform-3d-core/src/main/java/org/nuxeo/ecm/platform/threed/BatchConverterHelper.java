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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class to take out renders and list of {@link TransmissionThreeD} form batch conversion
 *
 * @since 8.4
 */
public class BatchConverterHelper {

    private BatchConverterHelper() {
    }

    public static final List<TransmissionThreeD> getTransmissons(List<Blob> blobs) {

        return blobs.stream().filter(blob -> "dae".equals(FilenameUtils.getExtension(blob.getFilename()))).map(blob -> {
            String baseName = FilenameUtils.getBaseName(blob.getFilename());
            String[] baseArray = baseName.split("-");
            float lod = Float.valueOf(baseArray[baseArray.length - 1]);
            return new TransmissionThreeD(blob, lod, blob.getFilename());
        }).collect(Collectors.toList());
    }

    public static final List<Blob> getRenders(List<Blob> blobs) {
        return blobs.stream().filter(blob -> "png".equals(FilenameUtils.getExtension(blob.getFilename()))).collect(
                Collectors.toList());
    }
}

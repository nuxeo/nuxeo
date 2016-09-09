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

import org.apache.commons.io.FilenameUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.core.convert.api.ConversionException;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nuxeo.ecm.platform.threed.convert.Constants.LOD_IDS_PARAMETER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.OUT_DIR_PARAMETER;

/**
 * Conversion from any supported 3D type to Collada
 *
 * @since 8.4
 */
public class ColladaConverter extends BaseBlenderConverter {

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        return null;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        return null;
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) throws ConversionException {
        String outDir = cmdParams.getParameter(OUT_DIR_PARAMETER);
        Map<String, Integer> lodBlobIndexes = new HashMap<>();
        List<Integer> resourceIndexes = new ArrayList<>();
        List<Blob> blobs = new ArrayList<>();

        String lodDir = outDir + File.separatorChar + "convert";
        List<String> conversions = getConversionLOD(lodDir);
        conversions.forEach(filename -> {
            File file = new File(lodDir + File.separatorChar + filename);
            Blob blob = new FileBlob(file);
            blob.setFilename(file.getName());
            if (FilenameUtils.getExtension(filename).toLowerCase().equals("dae")) {
                String[] filenameArray = filename.split("-");
                if (filenameArray.length != 4) {
                    throw new ConversionException(filenameArray + " incompatible with conversion file name schema.");
                }
                lodBlobIndexes.put(filenameArray[1], blobs.size());
            } else {
                resourceIndexes.add(blobs.size());
            }
            blobs.add(blob);
        });

        Map<String, Serializable> properties = new HashMap<>();
        properties.put("cmdOutput", (Serializable) cmdOutput);
        properties.put("resourceIndexes", (Serializable) resourceIndexes);
        properties.put("lodIdIndexes", (Serializable) lodBlobIndexes);

        return new SimpleBlobHolderWithProperties(blobs, properties);
    }
}

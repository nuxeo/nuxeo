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
import java.util.stream.Collectors;

import static org.nuxeo.ecm.platform.threed.convert.Constants.*;


/**
 * Batch conversion for 3D document types Generate thumbnail render, Collada version and LOD versions
 *
 * @since 8.4
 */
public class BatchConverter extends BaseBlenderConverter {

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        return null;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        Map<String, String> cmdStringParams = new HashMap<>();

        String percPoly = null;
        if (parameters.containsKey(PERC_POLY_PARAMETER)) {
            percPoly = (String) parameters.get(PERC_POLY_PARAMETER);
        } else if (initParameters.containsKey(PERC_POLY_PARAMETER)) {
            percPoly = initParameters.get(PERC_POLY_PARAMETER);
        }
        cmdStringParams.put(PERC_POLY_PARAMETER, percPoly);

        String maxPoly = null;
        if (parameters.containsKey(MAX_POLY_PARAMETER)) {
            maxPoly = (String) parameters.get(MAX_POLY_PARAMETER);
        } else if (initParameters.containsKey(MAX_POLY_PARAMETER)) {
            maxPoly = initParameters.get(MAX_POLY_PARAMETER);
        }
        cmdStringParams.put(MAX_POLY_PARAMETER, maxPoly);

        String dimensions = null;
        if (parameters.containsKey(DIMENSIONS_PARAMETER)) {
            dimensions = (String) parameters.get(DIMENSIONS_PARAMETER);
        } else if (initParameters.containsKey(DIMENSIONS_PARAMETER)) {
            dimensions = initParameters.get(DIMENSIONS_PARAMETER);
        }
        cmdStringParams.put(DIMENSIONS_PARAMETER, dimensions);

        return cmdStringParams;
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) throws ConversionException {
        String outDir = cmdParams.getParameter(OUT_DIR_PARAMETER);
        List<String> lodIdList = cmdParams.getParameters().get(LOD_IDS_PARAMETER).getValues();
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

        String renderDir = outDir + File.separatorChar + "render";
        List<String> renders = getRenders(renderDir);
        List<String> renderIdList = cmdParams.getParameters().get(RENDER_IDS_PARAMETER).getValues();
        if (renders.isEmpty() || renders.size() != renderIdList.size()) {
            throw new ConversionException("Unable get result render");
        }

        Map<String, Serializable> properties = new HashMap<>();
        properties.put("cmdOutput", (Serializable) cmdOutput);
        properties.put("resourceIndexes", (Serializable) resourceIndexes);
        properties.put("lodIdIndexes", (Serializable) lodBlobIndexes);
        properties.put("renderStartIndex", blobs.size());

        blobs.addAll(renders.stream().map(result -> {
            File file = new File(renderDir + File.separatorChar + result);
            Blob blob = new FileBlob(file);
            blob.setFilename(file.getName());
            return blob;
        }).collect(Collectors.toList()));

        return new SimpleBlobHolderWithProperties(blobs, properties);
    }
}

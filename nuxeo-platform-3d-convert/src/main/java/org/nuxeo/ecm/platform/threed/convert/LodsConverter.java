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

import org.apache.commons.lang.ArrayUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.platform.threed.convert.Constants.*;

/**
 * Level of details converter for 3D document type
 *
 * @since 8.4
 */
public class LodsConverter extends BaseBlenderConverter {

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        return null;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        Map<String, String> cmdStringParams = new HashMap<>();
        String lods = null;
        if (parameters.containsKey(LODS_PARAMETER)) {
            lods = (String) parameters.get(LODS_PARAMETER);
        } else if (initParameters.containsKey(LODS_PARAMETER)) {
            lods = initParameters.get(LODS_PARAMETER);
        }
        cmdStringParams.put(LODS_PARAMETER, lods);

        return cmdStringParams;
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) throws ConversionException {
        String outDir = cmdParams.getParameter(OUT_DIR_PARAMETER);
        List<String> conversions = getConversions(outDir);
        List<String> lodList = cmdParams.getParameters().get(LODS_PARAMETER).getValues();
        if (conversions.isEmpty() || conversions.size() != lodList.size()) {
            throw new ConversionException("Unable get correct number of lod versions");
        }

        List<Blob> blobs = conversions.stream().map(conversion -> {
            File file = new File(conversion);
            Blob blob = new FileBlob(file);
            blob.setFilename(file.getName());
            return blob;
        }).collect(Collectors.toList());

        Map<String, Serializable> properties = new HashMap<>();
        properties.put("cmdOutput", (Serializable) cmdOutput);
        return new SimpleBlobHolderWithProperties(blobs, properties);
    }
}

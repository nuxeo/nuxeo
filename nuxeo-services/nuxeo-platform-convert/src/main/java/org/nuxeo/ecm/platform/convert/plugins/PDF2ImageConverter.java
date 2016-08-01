/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;

/**
 * Pdf2Image converter based on imageMagick's convert command-line executable.
 *
 * @author ldoguin
 */
public class PDF2ImageConverter extends CommandLineBasedConverter {

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) {

        String outputPath = cmdParams.getParameter("outDirPath");
        File outputDir = new File(outputPath);
        File[] files = outputDir.listFiles();
        List<Blob> blobs = new ArrayList<Blob>();

        for (File file : files) {
            try {
                Blob blob = Blobs.createBlob(file);
                blob.setFilename(file.getName());
                blobs.add(blob);
            } catch (IOException e) {
                throw new ConversionException("Cannot create Blob", e);
            }
        }
        return new SimpleCachableBlobHolder(blobs);
    }

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {

        Map<String, Blob> cmdBlobParams = new HashMap<String, Blob>();
        cmdBlobParams.put("sourceFilePath", blobHolder.getBlob());
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {

        Map<String, String> cmdStringParams = new HashMap<String, String>();

        String baseDir = getTmpDirectory(parameters);
        Path tmpPath = new Path(baseDir).append("pdf2image_" + System.currentTimeMillis());

        File outDir = new File(tmpPath.toString());
        boolean dirCreated = outDir.mkdir();
        if (!dirCreated) {
            throw new ConversionException("Unable to create tmp dir for transformer output");
        }

        cmdStringParams.put("outDirPath", outDir.getAbsolutePath());
        cmdStringParams.put("targetFilePath", outDir.getAbsolutePath() + System.getProperty("file.separator")
                + parameters.get("targetFilePath").toString());
        return cmdStringParams;
    }

}

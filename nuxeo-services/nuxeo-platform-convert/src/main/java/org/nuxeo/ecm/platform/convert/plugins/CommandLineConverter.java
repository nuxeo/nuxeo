/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;

/**
 * Generic converter executing a contributed command line.
 * <p>
 * The command line to call is stored in the {@code CommandLineName} parameter.
 * <p>
 * The target file path is in the {@code targetFilePath} parameter. If it's null, a temporary one will be created.
 * <p>
 * All the converter parameters are passed to the command line.
 * <p>
 * A sample contribution using this converter:
 *
 * <pre>
 *     <converter name="pdf2image" class="org.nuxeo.ecm.platform.convert.plugins.CommandLineConverter">
 *       <sourceMimeType>application/pdf</sourceMimeType>
 *       <destinationMimeType>image/jpeg</destinationMimeType>
 *       <destinationMimeType>image/png</destinationMimeType>
 *       <destinationMimeType>image/gif</destinationMimeType>
 *       <parameters>
 *         <parameter name="CommandLineName">pdftoimage</parameter>
 *       </parameters>
 *     </converter>
 * </pre>
 *
 * @since 7.1
 */
public class CommandLineConverter extends CommandLineBasedConverter {

    public static final String SOURCE_FILE_PATH_KEY = "sourceFilePath";

    public static final String OUT_DIR_PATH_KEY = "outDirPath";

    public static final String TARGET_FILE_PATH_KEY = "targetFilePath";

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        Map<String, Blob> cmdBlobParams = new HashMap<>();
        try {
            cmdBlobParams.put(SOURCE_FILE_PATH_KEY, blobHolder.getBlob());
        } catch (ClientException e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        String tmpDir = getTmpDirectory(parameters);
        Path tmpDirPath = tmpDir != null ? Paths.get(tmpDir) : null;
        try {
            Path outDirPath = tmpDirPath != null ? Files.createTempDirectory(tmpDirPath, null)
                    : Files.createTempDirectory(null);

            Map<String, String> cmdStringParams = new HashMap<>();
            cmdStringParams.put(OUT_DIR_PATH_KEY, outDirPath.toString());

            String targetFilePathParam = (String) parameters.get(TARGET_FILE_PATH_KEY);
            Path targetFilePath;
            if (targetFilePathParam == null) {
                targetFilePath = tmpDirPath != null ? Files.createTempFile(tmpDirPath, null, null)
                        : Files.createTempFile(null, null);
            } else {
                targetFilePath = Paths.get(targetFilePathParam);
                if (!targetFilePath.isAbsolute()) {
                    targetFilePath = Paths.get(outDirPath.toString(), targetFilePath.toString());
                }
            }
            cmdStringParams.put(TARGET_FILE_PATH_KEY, targetFilePath.toString());

            // pass all converter parameters to the command line
            for (Map.Entry<String, Serializable> entry : parameters.entrySet()) {
                if (!TARGET_FILE_PATH_KEY.equals(entry.getKey())) {
                    cmdStringParams.put(entry.getKey(), (String) entry.getValue());
                }
            }

            return cmdStringParams;
        } catch (IOException e) {
            throw new ConversionException(e.getMessage(), e);
        }
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) throws ConversionException {
        String outputPath = cmdParams.getParameters().get(OUT_DIR_PATH_KEY);
        List<Blob> blobs = new ArrayList<>();
        File outputDir = new File(outputPath);
        if (outputDir.exists() && outputDir.isDirectory()) {
            File[] files = outputDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    Blob blob = new FileBlob(file);
                    blob.setFilename(file.getName());
                    blobs.add(blob);
                }
            }
        }
        return new SimpleCachableBlobHolder(blobs);
    }

}

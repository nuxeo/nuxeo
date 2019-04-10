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

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;
import org.nuxeo.runtime.api.Framework;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.platform.threed.convert.Constants.*;

/**
 * Conversion from Collada to glTF format
 *
 * @since 8.4
 */
public class Collada2glTFConverter extends CommandLineBasedConverter {

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

    // Let's override to make sure we keep the original blob names!
    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        String dataContainer = "data" + String.valueOf(Calendar.getInstance().getTime().getTime());
        String convertContainer = "convert" + String.valueOf(Calendar.getInstance().getTime().getTime());
        String commandName = getCommandName(blobHolder, parameters);
        if (commandName == null) {
            throw new ConversionException("Unable to determine target CommandLine name");
        }

        CmdParameters params = new CmdParameters();
        List<String> filesToDelete = new ArrayList<>();

        try {
            Path directory = new Path(getTmpDirectory(parameters)).append(
                    DAE2GLTF_PATH_PREFIX + UUID.randomUUID() + "_in");
            boolean dirCreated = new File(directory.toString()).mkdirs();
            if (!dirCreated) {
                throw new ConversionException("Unable to create tmp dir: " + directory);
            }

            List<Blob> blobs = blobHolder.getBlobs();
            // Add the main and assets as params
            // The params are not used by the command but the blobs are extracted to files and managed
            filesToDelete = blobs.stream().map(blob -> {
                File file = new File(directory.append(blob.getFilename()).toString());
                try {
                    blob.transferTo(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return file.getAbsolutePath();
            }).collect(Collectors.toList());
            Path inputFile = new Path(filesToDelete.get(0));
            params.addNamedParameter(INPUT_FILE_PARAMETER, inputFile.lastSegment());

            ExecResult createRes = DockerHelper.CreateContainer(dataContainer, "nuxeo/collada2gltf");
            if (createRes == null || !createRes.isSuccessful()) {
                throw new ConversionException("Unable to create data volume : " + dataContainer,
                        (createRes != null) ? createRes.getError() : null);
            }
            ExecResult copyRes = DockerHelper.CopyData(
                    inputFile.removeLastSegments(1).toString() + File.separatorChar + ".", dataContainer + ":/in/");
            if (copyRes == null || !copyRes.isSuccessful()) {
                throw new ConversionException("Unable to copy to data volume : " + dataContainer,
                        (copyRes != null) ? copyRes.getError() : null);
            }
            params.addNamedParameter(NAME_PARAM, convertContainer);
            params.addNamedParameter(DATA_PARAM, dataContainer);
            filesToDelete.add(directory.toString());

            String baseDir = getTmpDirectory(parameters);
            Path outPath = new Path(baseDir).append(DAE2GLTF_OUTPUT_PATH_PREFIX + UUID.randomUUID() + "_out");
            File outDir = new File(outPath.toString());
            dirCreated = outDir.mkdir();
            if (!dirCreated) {
                throw new ConversionException("Unable to create tmp dir for transformer output: " + outDir);
            }
            params.addNamedParameter(OUTPUT_FILE_PARAMETER, inputFile.removeFileExtension().lastSegment() + ".gltf");
            params.addNamedParameter(OUT_DIR_PARAMETER, outPath.toString());
            // params.addNamedParameter(USER_ID_PARAMETER, UserIdHelper.getUid());

            ExecResult result = Framework.getService(CommandLineExecutorService.class).execCommand(commandName, params);
            if (!result.isSuccessful()) {
                throw result.getError();
            }

            copyRes = DockerHelper.CopyData(dataContainer + ":/out/.", outPath.toString());
            if (copyRes == null || !copyRes.isSuccessful()) {
                throw new ConversionException("Unable to copy from data volume : " + dataContainer,
                        (copyRes != null) ? copyRes.getError() : null);
            }
            return buildResult(result.getOutput(), params);
        } catch (CommandNotAvailable e) {
            // XXX bubble installation instructions
            throw new ConversionException("Unable to find targetCommand", e);
        } catch (CommandException e) {
            throw new ConversionException("Error while converting via CommandLineService", e);
        } finally {
            for (String fileToDelete : filesToDelete) {
                FileUtils.deleteQuietly(new File(fileToDelete));
            }
            DockerHelper.RemoveContainer(dataContainer);
            DockerHelper.RemoveContainer(convertContainer);
        }
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParameters) throws ConversionException {
        File outputFile = new File(cmdParameters.getParameter(OUT_DIR_PARAMETER) + File.separator
                + cmdParameters.getParameter(OUTPUT_FILE_PARAMETER));
        List<Blob> blobs = new ArrayList<>();
        blobs.add(new FileBlob(outputFile));
        Map<String, Serializable> properties = new HashMap<>();
        properties.put("cmdOutput", (Serializable) cmdOutput);
        return new SimpleBlobHolderWithProperties(blobs, properties);
    }

}

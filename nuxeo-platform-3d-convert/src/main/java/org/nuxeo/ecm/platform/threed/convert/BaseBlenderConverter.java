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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.commandline.executor.api.*;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.plugins.CommandLineBasedConverter;
import org.nuxeo.runtime.api.Framework;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.platform.threed.convert.Constants.*;

/**
 * Base converter for blender pipeline. Processes scripts for operators and input blobs
 *
 * @since 8.4
 */
public abstract class BaseBlenderConverter extends CommandLineBasedConverter {

    protected Path tempDirectory(Map<String, Serializable> parameters) throws ConversionException {
        Path directory = new Path(getTmpDirectory(parameters)).append(BLENDER_PATH_PREFIX + UUID.randomUUID());
        boolean dirCreated = new File(directory.toString()).mkdirs();
        if (!dirCreated) {
            throw new ConversionException("Unable to create tmp dir: " + directory);
        }
        return directory;
    }

    protected List<String> blobsToTempDir(BlobHolder blobHolder) throws IOException {
        Path directory = tempDirectory(null);
        List<Blob> blobs = blobHolder.getBlobs();

        // Add the main and assets as params
        // The params are not used by the command but the blobs are extracted to files and managed!
        List<String> filesCreated = blobs.stream().map(blob -> {
            File file = new File(directory.append(blob.getFilename()).toString());
            try {
                blob.transferTo(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return file.getAbsolutePath();
        }).collect(Collectors.toList());

        filesCreated.add(directory.toString());
        return filesCreated;
    }

    private void createPath(Path path) throws ConversionException {
        File pathFile = new File(path.toString());
        if (!pathFile.exists()) {
            boolean dirCreated = pathFile.mkdir();
            if (!dirCreated) {
                throw new ConversionException("Unable to create tmp dir for scripts output: " + path);
            }
        }
    }

    private String copyScript(Path pathDst, Path source) throws IOException {
        String sourceFile = source.lastSegment();
        File script = new File(pathDst.append(sourceFile).toString());
        if (!script.exists()) {
            InputStream is = getClass().getResourceAsStream("/" + source.toString());
            FileUtils.copyInputStreamToFile(is, script);
        }
        return script.getAbsolutePath();
    }

    /**
     * Returns the absolute path to the main script (main and pipeline scripts). Copies the script to the filesystem if
     * needed. Copies needed {@code operators} script to the filesystem if missing.
     */
    protected String getScriptWith(List<String> operators) throws IOException {
        Path dataPath = new Path(Environment.getDefault().getData().getAbsolutePath());
        String source = initParameters.get(SCRIPT_PARAMETER);
        String sourceFile = new Path(source).lastSegment();

        // create scripts directory
        Path scriptsPath = dataPath.append(SCRIPTS_DIRECTORY);
        createPath(scriptsPath);

        // copy script resource
        String mainScriptPath = copyScript(scriptsPath, new Path(source));

        if (operators.isEmpty()) {
            return mainScriptPath;
        }

        // create pipeline scripts directory
        Path pipelinePath = scriptsPath.append(SCRIPTS_PIPELINE_DIRECTORY);
        createPath(pipelinePath);

        Path pipelineSourcePath = new Path(source).removeLastSegments(1).append("pipeline");
        // copy operators scripts resources
        operators.forEach(operator -> {
            try {
                copyScript(pipelinePath, pipelineSourcePath.append(operator + ".py"));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        return mainScriptPath;
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        String commandName = getCommandName(blobHolder, parameters);
        if (commandName == null) {
            throw new ConversionException("Unable to determine target CommandLine name");
        }

        List<String> filesToDelete = new ArrayList<>();
        List<Closeable> toClose = new ArrayList<>();
        try {
            CmdParameters params = new CmdParameters();

            // Deal with operators and script files (blender and pipeline)
            String operators = null;
            if (parameters.containsKey(OPERATORS_PARAMETER)) {
                operators = (String) parameters.get(OPERATORS_PARAMETER);
            } else if (initParameters.containsKey(OPERATORS_PARAMETER)) {
                operators = initParameters.get(OPERATORS_PARAMETER);
            }
            List<String> operatorsList = Arrays.asList(operators.split(" "));
            params.addNamedParameter(OPERATORS_PARAMETER, operatorsList);
            operatorsList = operatorsList.stream().distinct().collect(Collectors.toList());
            params.addNamedParameter(SCRIPT_PARAMETER, getScriptWith(operatorsList));

            // Initialize LOD params
            String lods = initParameters.getOrDefault(LODS_PARAMETER, "");
            List<String> lodList = Arrays.asList(lods.split(" "));
            params.addNamedParameter(LODS_PARAMETER, lodList);

            // Initialize spherical coordinates params
            String coords = "";
            if (parameters.containsKey(COORDS_PARAMETER)) {
                coords = (String) parameters.get(COORDS_PARAMETER);
            } else if (initParameters.containsKey(COORDS_PARAMETER)) {
                coords = initParameters.get(COORDS_PARAMETER);
            }
            List<String> coordList = Arrays.asList(coords.split(" "));
            params.addNamedParameter(COORDS_PARAMETER, coordList);

            // Deal with input blobs (main and assets)
            List<String> inputFiles = blobsToTempDir(blobHolder);
            params.addNamedParameter(INPUT_FILE_PATH_PARAMETER, new File(inputFiles.get(0)));
            filesToDelete = inputFiles;

            // Extra blob parameters
            Map<String, Blob> blobParams = getCmdBlobParameters(blobHolder, parameters);

            if (blobParams != null) {
                for (String blobParamName : blobParams.keySet()) {
                    Blob blob = blobParams.get(blobParamName);
                    // closed in finally block
                    CloseableFile closeable = blob.getCloseableFile(
                            "." + FilenameUtils.getExtension(blob.getFilename()));
                    params.addNamedParameter(blobParamName, closeable.getFile());
                    toClose.add(closeable);
                }
            }

            // Extra string parameters
            Map<String, String> strParams = getCmdStringParameters(blobHolder, parameters);

            if (strParams != null) {
                for (String paramName : strParams.keySet()) {
                    if (LODS_PARAMETER.equals(paramName) || COORDS_PARAMETER.equals(paramName)) {
                        params.addNamedParameter(paramName, Arrays.asList(strParams.get(paramName).split(" ")));
                    } else {
                        params.addNamedParameter(paramName, strParams.get(paramName));
                    }
                }
            }

            // Deal with output directory
            Path outDir = tempDirectory(null);
            params.addNamedParameter(OUT_DIR_PARAMETER, outDir.toString());

            ExecResult result = Framework.getService(CommandLineExecutorService.class).execCommand(commandName, params);
            if (!result.isSuccessful()) {
                throw result.getError();
            }
            return buildResult(result.getOutput(), params);
        } catch (CommandNotAvailable e) {
            // XXX bubble installation instructions
            throw new ConversionException("Unable to find targetCommand", e);
        } catch (IOException | CommandException e) {
            throw new ConversionException("Error while converting via CommandLineService", e);
        } finally {
            for (String fileToDelete : filesToDelete) {
                FileUtils.deleteQuietly(new File(fileToDelete));
            }
            for (Closeable closeable : toClose) {
                IOUtils.closeQuietly(closeable);
            }
        }

    }

    public List<String> getConversions(String outDir) {
        File directory = new File(outDir);
        String[] files = directory.list((dir, name) -> name.startsWith("conversion") && name.endsWith(".dae"));
        if (files == null) {
            return null;
        }
        return Arrays.asList(files);
    }

    public List<String> getRenders(String outDir) {
        File directory = new File(outDir);
        String[] files = directory.list((dir, name) -> name.startsWith("render") && name.endsWith(".png"));
        if (files == null) {
            return null;
        }
        return Arrays.asList(files);
    }

}

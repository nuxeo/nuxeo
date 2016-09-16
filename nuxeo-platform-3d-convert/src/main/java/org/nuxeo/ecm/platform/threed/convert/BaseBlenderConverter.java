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
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.*;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;
import org.nuxeo.runtime.api.Framework;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
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
        // xxx : find a way to check if the correct version is already there.
        File script = new File(pathDst.append(sourceFile).toString());
        InputStream is = getClass().getResourceAsStream("/" + source.toString());
        FileUtils.copyInputStreamToFile(is, script);
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

    private List<String> getParams(Map<String, Serializable> inParams, Map<String, String> initParams, String key) {
        String values = "";
        if (inParams.containsKey(key)) {
            values = (String) inParams.get(key);
        } else if (initParams.containsKey(key)) {
            values = initParams.get(key);
        }
        return Arrays.asList(values.split(" "));
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
            List<String> operatorsList = getParams(parameters, initParameters, OPERATORS_PARAMETER);
            params.addNamedParameter(OPERATORS_PARAMETER, operatorsList);

            operatorsList = operatorsList.stream().distinct().collect(Collectors.toList());
            params.addNamedParameter(SCRIPT_PARAMETER, getScriptWith(operatorsList));

            // Initialize render id params
            params.addNamedParameter(RENDER_IDS_PARAMETER, getParams(parameters, initParameters, RENDER_IDS_PARAMETER));

            // Initialize LOD id params
            params.addNamedParameter(LOD_IDS_PARAMETER, getParams(parameters, initParameters, LOD_IDS_PARAMETER));

            // Initialize percentage polygon params
            params.addNamedParameter(PERC_POLY_PARAMETER, getParams(parameters, initParameters, PERC_POLY_PARAMETER));

            // Initialize max polygon params
            params.addNamedParameter(MAX_POLY_PARAMETER, getParams(parameters, initParameters, MAX_POLY_PARAMETER));

            // Initialize spherical coordinates params
            params.addNamedParameter(COORDS_PARAMETER, getParams(parameters, initParameters, COORDS_PARAMETER));

            // Initialize dimension params
            params.addNamedParameter(DIMENSIONS_PARAMETER, getParams(parameters, initParameters, DIMENSIONS_PARAMETER));

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
                    if (RENDER_IDS_PARAMETER.equals(paramName) || LOD_IDS_PARAMETER.equals(paramName)
                            || PERC_POLY_PARAMETER.equals(paramName) || MAX_POLY_PARAMETER.equals(paramName)
                            || COORDS_PARAMETER.equals(paramName) || DIMENSIONS_PARAMETER.equals(paramName)) {
                        if (strParams.get(paramName) != null) {
                            params.addNamedParameter(paramName, Arrays.asList(strParams.get(paramName).split(" ")));
                        }
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

    public List<String> getConversionLOD(String outDir) {
        File directory = new File(outDir);
        String[] files = directory.list((dir, name) -> true);
        return (files == null) ? Collections.emptyList() : Arrays.asList(files);
    }

    public List<String> getRenders(String outDir) {
        File directory = new File(outDir);
        String[] files = directory.list((dir, name) -> name.startsWith("render") && name.endsWith(".png"));
        return (files == null) ? Collections.emptyList() : Arrays.asList(files);
    }

    public List<String> getInfos(String outDir) {
        File directory = new File(outDir);
        String[] files = directory.list((dir, name) -> name.endsWith(".info"));
        return (files == null) ? Collections.emptyList() : Arrays.asList(files);
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) throws ConversionException {
        String outDir = cmdParams.getParameter(OUT_DIR_PARAMETER);
        Map<String, Integer> lodBlobIndexes = new HashMap<>();
        List<Integer> resourceIndexes = new ArrayList<>();
        List<Integer> infoIndexes = new ArrayList<>();
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

        String infoDir = outDir + File.separatorChar + "info";
        List<String> infos = getInfos(infoDir);
        infos.forEach(filename -> {
            File file = new File(infoDir + File.separatorChar + filename);
            Blob blob = new FileBlob(file);
            blob.setFilename(file.getName());
            infoIndexes.add(blobs.size());
            blobs.add(blob);
        });

        String renderDir = outDir + File.separatorChar + "render";
        List<String> renders = getRenders(renderDir);

        Map<String, Serializable> properties = new HashMap<>();
        properties.put("cmdOutput", (Serializable) cmdOutput);
        properties.put("resourceIndexes", (Serializable) resourceIndexes);
        properties.put("infoIndexes", (Serializable) infoIndexes);
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

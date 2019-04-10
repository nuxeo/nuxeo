/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.video.convert;

import static org.nuxeo.ecm.platform.video.convert.Constants.INPUT_FILE_PATH_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_PATH_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.POSITION_PARAMETER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Converter to extract a list of equally spaced JPEG thumbnails to represent
 * the story-line of a movie file using the ffmpeg commandline tool.
 *
 * @author ogrisel
 */
public class StoryboardConverter extends BaseVideoConverter implements
        Converter {

    public static final Log log = LogFactory.getLog(StoryboardConverter.class);

    public static final String FFMPEG_INFO_COMMAND = "ffmpeg-info";

    public static final String FFMPEG_SCREENSHOT_RESIZE_COMMAND = "ffmpeg-screenshot-resize";

    public static final String WIDTH_PARAM = "width";

    public static final String HEIGHT_PARAM = "height";

    public static final String THUMBNAIL_NUMBER_PARAM = "thumbnail_number";

    protected int numberOfThumbnails = 9;

    protected Map<String, String> commonParams = new HashMap<String, String>();

    @Override
    public void init(ConverterDescriptor descriptor) {
        try {
            cleService = Framework.getService(CommandLineExecutorService.class);
        } catch (Exception e) {
            log.error(e, e);
            return;
        }
        commonParams = descriptor.getParameters();
        if (!commonParams.containsKey(WIDTH_PARAM)) {
            commonParams.put(WIDTH_PARAM, "100");
        }
        if (!commonParams.containsKey(HEIGHT_PARAM)) {
            commonParams.put(HEIGHT_PARAM, "62");
        }
        if (commonParams.containsKey(THUMBNAIL_NUMBER_PARAM)) {
            numberOfThumbnails = Integer.parseInt(commonParams.get(THUMBNAIL_NUMBER_PARAM));
        }
        if (numberOfThumbnails < 1) {
            numberOfThumbnails = 1;
        }
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        File outFolder = null;
        InputFile inputFile = null;
        Blob blob = null;

        // Build the empty output structure
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        List<Blob> blobs = new ArrayList<Blob>();
        List<Double> timecodes = new ArrayList<Double>();
        List<String> comments = new ArrayList<String>();
        properties.put("timecodes", (Serializable) timecodes);
        properties.put("comments", (Serializable) comments);
        SimpleBlobHolderWithProperties bh = new SimpleBlobHolderWithProperties(
                blobs, properties);
        try {
            blob = blobHolder.getBlob();
            inputFile = new InputFile(blob);

            outFolder = File.createTempFile("StoryboardConverter-out-", "-tmp");
            outFolder.delete();
            outFolder.mkdir();

            CmdParameters params = new CmdParameters();
            params.addNamedParameter(INPUT_FILE_PATH_PARAMETER,
                    inputFile.file.getAbsolutePath());

            Double duration = (Double) parameters.get("duration");
            if (duration == null) {
                log.warn(String.format(
                        "Cannot extract storyboard for file '%s'"
                                + " with missing duration info.",
                        blob.getFilename()));
                return bh;
            }
            if (duration < 10.0) {
                // do not extract a storyboard for so short videos
                return bh;
            }
            // add the command line parameters for the storyboard extraction and
            // run it
            for (int i = 0; i < numberOfThumbnails; i++) {
                long timecode = Double.valueOf(
                        Math.floor(i * duration / numberOfThumbnails)).longValue();
                File outSubFolder = new File(outFolder,
                        String.format("%04d", i));
                outSubFolder.mkdir();
                File outFile = new File(outSubFolder, "video-thumb-%04d.jpeg");
                params.addNamedParameter(OUTPUT_FILE_PATH_PARAMETER,
                        outFile.getAbsolutePath());
                params.addNamedParameter(POSITION_PARAMETER,
                        String.valueOf(timecode));
                params.addNamedParameter(WIDTH_PARAM,
                        commonParams.get(WIDTH_PARAM));
                params.addNamedParameter(HEIGHT_PARAM,
                        commonParams.get(HEIGHT_PARAM));
                ExecResult result = cleService.execCommand(
                        FFMPEG_SCREENSHOT_RESIZE_COMMAND, params);

                if (!result.isSuccessful()) {
                    throw result.getError();
                }

                List<File> thumbs = new ArrayList<File>(FileUtils.listFiles(
                        outSubFolder, new String[] { "jpeg" }, false));
                if (thumbs.isEmpty()) {
                    String commandLine = result.getCommandLine();
                    log.error("Error executing command: " + commandLine);
                    String output = StringUtils.join(result.getOutput(), "\n");
                    log.error(output);
                    throw new ConversionException(String.format(
                            "Failed taking storyboard screenshot for"
                                    + " video file '%s' with command: %s",
                            blob.getFilename(), commandLine));
                }
                Blob thumbBlob = StreamingBlob.createFromStream(
                        new FileInputStream(thumbs.get(0)), "image/jpeg").persist();
                thumbBlob.setFilename(String.format("%05d.000-seconds.jpeg",
                        timecode));
                blobs.add(thumbBlob);
                timecodes.add(Double.valueOf(timecode));
                comments.add(String.format("%s %d", blob.getFilename(), i));
            }
            return bh;
        } catch (IOException e) {
            String msg = getErrorMessage(blob);
            throw new ConversionException(msg, e);
        } catch (CommandNotAvailable e) {
            String msg = getErrorMessage(blob);
            throw new ConversionException(msg, e);
        } catch (ClientException e) {
            String msg = getErrorMessage(blob);
            throw new ConversionException(msg, e);
        } catch (CommandException e) {
            String msg = getErrorMessage(blob);
            throw new ConversionException(msg, e);
        } finally {
            FileUtils.deleteQuietly(outFolder);
            if (inputFile != null && inputFile.isTempFile) {
                FileUtils.deleteQuietly(inputFile.file);
            }
        }
    }

    private String getErrorMessage(Blob blob) {
        String msg;
        if (blob != null) {
            msg = "Error extracting story board from '" + blob.getFilename()
                    + "'";
        } else {
            msg = "conversion failed";
        }
        return msg;
    }
}

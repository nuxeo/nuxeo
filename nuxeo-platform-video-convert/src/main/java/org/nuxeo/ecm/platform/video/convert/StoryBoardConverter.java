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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
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

    public static final String FFMPEG_STORYBOARD_COMMAND = "ffmpeg-storyboard";

    public static final String RATE_PARAM = "rate";

    public static final String WIDTH_PARAM = "width";

    public static final String HEIGHT_PARAM = "height";

    public static final String THUMBNAIL_NUMBER_PARAM = "thumbnail_number";

    protected int numberOfThumbnails = 9;

    protected Map<String, String> commonParams = new HashMap<String, String>();

    public void init(ConverterDescriptor descriptor) {
        try {
            cleService = Framework.getService(CommandLineExecutorService.class);
        } catch (Exception e) {
            log.error(e, e);
            return;
        }
        commonParams = descriptor.getParameters();
        if (!commonParams.containsKey(RATE_PARAM)) {
            // NB: the minimum rate accepted by the current version of ffmpeg
            // (SVN-r19352-4:0.5+svn20090706-2ubuntu2) is 0.1, i.e. at least one
            // thumbnail every 10s
            commonParams.put(RATE_PARAM, "0.1");
        }
        if (!commonParams.containsKey(WIDTH_PARAM)) {
            commonParams.put(WIDTH_PARAM, "100");
        }
        if (!commonParams.containsKey(HEIGHT_PARAM)) {
            commonParams.put(HEIGHT_PARAM, "62");
        }
        if (commonParams.containsKey(THUMBNAIL_NUMBER_PARAM)) {
            numberOfThumbnails = Integer.parseInt(commonParams.get(THUMBNAIL_NUMBER_PARAM));
        }
    }

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        File outFolder = null;
        InputFile inputFile = null;
        Blob blob = null;
        try {
            blob = blobHolder.getBlob();
            inputFile = new InputFile(blob);

            outFolder = File.createTempFile("StoryboardConverter-out-", "-tmp");
            outFolder.delete();
            outFolder.mkdir();
            Map<String, Serializable> properties = new HashMap<String, Serializable>();

            CmdParameters params = new CmdParameters();
            params.addNamedParameter("inFilePath",
                    inputFile.file.getAbsolutePath());

            // read the duration with a first command to adjust the best rate:
            ExecResult result = cleService.execCommand(FFMPEG_INFO_COMMAND,
                    params);
            Double duration = extractDuration(result.getOutput());
            properties.put("duration", duration);


            if (duration < 3.0) {
                // do not extract a storyboard for so short videos
                return collectBlobs(outFolder, properties, blob.getFilename(), 0.0);
            }

            Double rate = numberOfThumbnails / duration;
            if (rate  < 0.1) {
                // NB: the minimum rate accepted by the current version of
                // ffmpeg (SVN-r19352-4:0.5+svn20090706-2ubuntu2) is 0.1,
                // i.e. at least one thumbnail every 10s
                rate = 0.1;
            }
            String rateParam = String.format(Locale.US, "%f", rate);

            // add the command line parameters for the storyboard extraction and
            // run it
            params.addNamedParameter("outFolderPath",
                    outFolder.getAbsolutePath());
            params.addNamedParameter(RATE_PARAM, rateParam);
            params.addNamedParameter(WIDTH_PARAM, commonParams.get(WIDTH_PARAM));
            params.addNamedParameter(HEIGHT_PARAM,
                    commonParams.get(HEIGHT_PARAM));
            result = cleService.execCommand(FFMPEG_STORYBOARD_COMMAND, params);
            if (!result.isSuccessful()) {
                Exception error = result.getError();
                if (error != null) {
                    throw error;
                } else {
                    throw new ConversionException(StringUtils.join(
                            result.getOutput(), " "));
                }
            }
            return collectBlobs(outFolder, properties, blob.getFilename(), rate);
        } catch (Exception e) {
            if (blob != null) {
                throw new ConversionException(
                        "error extracting story board from '"
                                + blob.getFilename() + "': " + e.getMessage(),
                        e);
            } else {
                throw new ConversionException(e.getMessage(), e);
            }
        } finally {
            FileUtils.deleteQuietly(outFolder);
            if (inputFile != null && inputFile.isTempFile) {
                FileUtils.deleteQuietly(inputFile.file);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected BlobHolder collectBlobs(File outFolder,
            Map<String, Serializable> properties, String filename, Double rate)
            throws IOException, FileNotFoundException {

        List<File> thumbs = new ArrayList<File>(FileUtils.listFiles(outFolder,
                new String[] { "jpeg" }, false));
        Collections.sort(thumbs);
        List<Blob> blobs = new ArrayList<Blob>();
        List<Double> timecodes = new ArrayList<Double>();
        List<String> comments = new ArrayList<String>();
        int skip = 1;
        if (thumbs.size() > numberOfThumbnails) {
            skip = thumbs.size() / numberOfThumbnails;
        }
        // skip the first screenshot which seems to be duplicated
        for (int i = 1; i < thumbs.size(); i += skip) {
            Blob keptBlob = StreamingBlob.createFromStream(
                    new FileInputStream(thumbs.get(i)), "image/jpeg").persist();
            // TODO: 10s is a match for the default rate of 0.1 fps: need to
            // make it dynamic
            int timecode = (int) Math.floor((i - 1) * (1.0 / rate));
            keptBlob.setFilename(String.format("%05d.000-seconds.jpeg",
                    timecode));
            blobs.add(keptBlob);
            timecodes.add(Double.valueOf(timecode));
            comments.add(String.format("%s %d", filename, ((i - 1) / skip) + 1));
            if (blobs.size() >= numberOfThumbnails) {
                // depending of the remainder of the Euclidean division we might
                // get an additional unwanted blob, skip the last
                break;
            }
        }
        properties.put("timecodes", (Serializable) timecodes);
        properties.put("comments", (Serializable) comments);
        SimpleBlobHolderWithProperties bh = new SimpleBlobHolderWithProperties(
                blobs, properties);
        return bh;
    }
}

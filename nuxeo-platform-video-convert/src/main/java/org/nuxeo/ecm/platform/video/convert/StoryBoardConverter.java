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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * Converter to extract a list of equally spaced JPEG thumbnails to represent
 * the story-line of a movie file using the ffmpeg commandline tool.
 *
 * @author ogrisel
 */
public class StoryBoardConverter implements Converter {

    public static final Log log = LogFactory.getLog(StoryBoardConverter.class);

    public static final String FFMPEG_STORYBOARD_CONVERTER = "ffmpeg-storyboard";

    public static final String RATE_PARAM = "rate";

    public static final String WIDTH_PARAM = "width";

    public static final String HEIGHT_PARAM = "height";

    public static final String THUMBNAIL_NUMBER_PARAM = "thumbnail_number";

    protected CommandLineExecutorService cleService;

    protected int numberOfThumbnails = 8;

    protected Map<String, String> commonParams = new HashMap<String, String>();

    protected static final Pattern DURATION_PATTERN = Pattern.compile("Duration: ([:\\d]+)");

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
            commonParams.put(WIDTH_PARAM, "176");
        }
        if (!commonParams.containsKey(HEIGHT_PARAM)) {
            commonParams.put(HEIGHT_PARAM, "144");
        }
        if (commonParams.containsKey(THUMBNAIL_NUMBER_PARAM)) {
            numberOfThumbnails = Integer.parseInt(commonParams.get(THUMBNAIL_NUMBER_PARAM));
        }
    }

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        boolean cleanInFile = false;
        File inFile = null;
        File outFolder = null;
        Blob blob = null;
        try {
            blob = blobHolder.getBlob();
            if (blob instanceof FileBlob) {
                // avoid creating temporary files when unnecessary
                FileBlob fileBlob = (FileBlob) blob;
                inFile = fileBlob.getFile();
            } else if (blob instanceof StreamingBlob) {
                StreamingBlob streamingBlob = (StreamingBlob) blob;
                StreamSource source = streamingBlob.getStreamSource();
                if (source instanceof FileSource) {
                    FileSource fileSource = (FileSource) source;
                    inFile = fileSource.getFile();
                }
            }

            if (inFile == null) {
                // create temporary dfile
                inFile = File.createTempFile("StoryBoardConverter-in-", "-"
                        + blob.getFilename());
                blob.transferTo(inFile);
                cleanInFile = true;
            }

            outFolder = File.createTempFile("StoryBoardConverter-out-", "-tmp");
            outFolder.delete();
            outFolder.mkdir();

            CmdParameters params = new CmdParameters();
            params.addNamedParameter("inFilePath", inFile.getAbsolutePath());
            params.addNamedParameter("outFolderPath",
                    outFolder.getAbsolutePath());
            params.addNamedParameter(RATE_PARAM, commonParams.get(RATE_PARAM));
            params.addNamedParameter(WIDTH_PARAM, commonParams.get(WIDTH_PARAM));
            params.addNamedParameter(HEIGHT_PARAM,
                    commonParams.get(HEIGHT_PARAM));
            ExecResult result = cleService.execCommand(
                    FFMPEG_STORYBOARD_CONVERTER, params);

            if (!result.isSuccessful()) {
                throw result.getError();
            }
            List<Blob> blobs = collectBlobs(outFolder);

            Map<String, Serializable> properties = new HashMap<String, Serializable>();
            properties.put("duration", extractDuration(result.getOutput()));
            return new SimpleBlobHolderWithProperties(blobs, properties);
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
            if (cleanInFile) {
                FileUtils.deleteQuietly(inFile);
            }
        }
    }

    protected Long extractDuration(List<String> output) {
        for (String line : output) {
            Matcher matcher = DURATION_PATTERN.matcher(line);
            if (matcher.find()) {
                String duration = matcher.group(1);
                String[] parts = duration.split(":");
                return Long.parseLong(parts[0]) * 3600
                        + Long.parseLong(parts[1]) * 60
                        + Long.parseLong(parts[2]);
            }
        }
        return null;
    }

    protected List<Blob> collectBlobs(File outFolder) throws IOException,
            FileNotFoundException {

        List<File> thumbs = new ArrayList<File>(FileUtils.listFiles(outFolder,
                new String[] { "jpeg" }, false));
        Collections.sort(thumbs);
        List<Blob> blobs = new ArrayList<Blob>();
        int skip = 1;
        if (thumbs.size() > numberOfThumbnails) {
            skip = thumbs.size() / numberOfThumbnails;
        }
        for (int i = 0; i < thumbs.size(); i += skip) {
            Blob keptBlob = StreamingBlob.createFromStream(
                    new FileInputStream(thumbs.get(i)), "image/jpeg").persist();
            // TODO: 10s is a match for the default rate of 0.1 fps: need to
            // make it dynamic
            String timecode = String.format("%06d", i * 10);
            keptBlob.setFilename(String.format("video-thumb-%s.jpeg", timecode));
            // abusing the encoding field to store the time code
            keptBlob.setEncoding(timecode);
            blobs.add(keptBlob);
            if (blobs.size() >= numberOfThumbnails) {
                // depending of the remainder of the euclidean division we might
                // get an additional unwanted blob, skip the last
                break;
            }
        }
        return blobs;
    }
}

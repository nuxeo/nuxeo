package org.nuxeo.ecm.platform.video.convert;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Converter to extract a list of equally spaced JPEG thumbnails to represent
 * the storyline of a movie file using the ffmpeg commandline tool.
 *
 * @author ogrisel
 */
public class StoryBoardConverter implements Converter {

    public static final String FFMPEG_STORYBOARD_CONVERTER = "ffmpeg-storyboard";

    public static final String RATE_PARAM = "rate";

    public static final String WIDTH_PARAM = "width";

    public static final String HEIGHT_PARAM = "height";

    public static final String THUMBNAIL_NUMBER_PARAM = "thumbnail_number";

    protected CommandLineExecutorService cleService;

    protected int numberOfThumbnails = 8;

    protected Map<String, String> commonParams = new HashMap<String, String>();

    public void init(ConverterDescriptor descriptor) {
        cleService = Framework.getLocalService(CommandLineExecutorService.class);
        commonParams = descriptor.getParameters();
        if (!commonParams.containsKey(RATE_PARAM)) {
            // NB: the minimum rate accepted by the current version of ffmpeg
            // (SVN-r19352-4:0.5+svn20090706-2ubuntu2) is 0.1, i.e. at least one
            // thumbnail every 10s
            commonParams.put(RATE_PARAM, "0.1");
        }
        if (!commonParams.containsKey(WIDTH_PARAM)) {
            commonParams.put(WIDTH_PARAM, "200");
        }
        if (!commonParams.containsKey(HEIGHT_PARAM)) {
            commonParams.put(HEIGHT_PARAM, "150");
        }
        if (!commonParams.containsKey(THUMBNAIL_NUMBER_PARAM)) {
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
            } else {
                // TODO: introspect source of StreamingBlobs to avoid creating
                // another temporary file unnecessary if the source is already
                // FileSource
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
            // TODO: make the following parameters configurable

            params.addNamedParameter(RATE_PARAM, commonParams.get(RATE_PARAM));
            params.addNamedParameter(WIDTH_PARAM, commonParams.get(WIDTH_PARAM));
            params.addNamedParameter(HEIGHT_PARAM,
                    commonParams.get(HEIGHT_PARAM));
            ExecResult result = cleService.execCommand(
                    FFMPEG_STORYBOARD_CONVERTER, params);

            if (!result.isSuccessful()) {
                throw result.getError();
            }

            List<File> thumbs = new ArrayList<File>(FileUtils.listFiles(
                    outFolder, new String[] { "jpeg" }, false));
            Collections.sort(thumbs);
            List<Blob> blobs = new ArrayList<Blob>();
            int skip = 1;
            int max = blobs.size();
            if (blobs.size() > numberOfThumbnails) {
                skip = blobs.size() / numberOfThumbnails;
                max = numberOfThumbnails;
            }
            for (int i = 0; i < max; i += skip) {
                Blob keptBlob = StreamingBlob.createFromStream(
                        new FileInputStream(thumbs.get(i)), "image/jpeg").persist();
                // abusing the encoding field to store the time code
                // TODO: 10s is a match for the default rate of 0.1 fps: need to
                // make it dynamic
                String timecode = String.format("%d", i * 10);
                keptBlob.setEncoding(timecode);
                blobs.add(keptBlob);
            }

            return new SimpleCachableBlobHolder(blobs);
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
}

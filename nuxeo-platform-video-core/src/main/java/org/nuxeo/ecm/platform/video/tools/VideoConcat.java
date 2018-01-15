/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */
package org.nuxeo.ecm.platform.video.tools;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;

/**
 * The {@link VideoTool} that joins two or more video blobs.
 *
 * @since 8.4
 */
public class VideoConcat extends VideoTool {

    public final static String NAME = "concatTool";

    protected final static String VIDEOS_FILE_PATH_PARAM = "listFilePath";

    protected final static String VIDEO_CONCAT_COMMANDLINE = "videoConcat";

    public VideoConcat() {
        super(VideoConcat.NAME, VIDEO_CONCAT_COMMANDLINE);
    }

    @Override
    public Map<String, String> setupParameters(BlobHolder blobHolder, Map<String, Object> parameters) {
        List<Blob> videos = blobHolder.getBlobs();
        List<File> sourceFiles = new ArrayList<>();
        File tempFile;
        try {
            //String list = "";
            List<String> lines = new ArrayList<>();
            for (Blob b : videos) {
                File blobFile = b.getFile();
                sourceFiles.add(blobFile);
                lines.add("file '" + blobFile.getAbsolutePath());
            }
            tempFile = Framework.createTempFile("NxVTcv-", ".txt");
            Files.write(tempFile.toPath(), lines, UTF_8, StandardOpenOption.CREATE);

            if (blobHolder.getBlobs().size() < 2) {
                throw new NuxeoException("VideoConcat requires at least two videos to proceed.");
            }

            Blob firstVideo = blobHolder.getBlobs().get(0);
            String outputFilename = firstVideo.getFilename();
            File outputFile = Framework.createTempFile(FilenameUtils.removeExtension(outputFilename),
                    "-concat." + FilenameUtils.getExtension(outputFilename));

            // Run the command line
            Map<String, String> params = new HashMap<>();
            params.put(VIDEOS_FILE_PATH_PARAM, tempFile.getAbsolutePath());
            params.put(OUTPUT_FILE_PATH_PARAM, outputFile.getAbsolutePath());
            return params;
        } catch (IOException e) {
            throw new NuxeoException("VideoConcat could not prepare the parameters.", e);
        }
    }

    @Override
    public void cleanupInputs(Map<String, String> parameters) {
        String path = parameters.get(VIDEOS_FILE_PATH_PARAM);
        FileUtils.deleteQuietly(new File(path));
    }

}

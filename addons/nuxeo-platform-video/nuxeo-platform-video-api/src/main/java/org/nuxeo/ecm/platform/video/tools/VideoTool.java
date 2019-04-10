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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * Common interface to setup the video tools.
 *
 * @since 8.4
 */
public abstract class VideoTool {

    protected String name;

    protected String commandLineName;

    protected final static String SOURCE_FILE_PATH_PARAM = "sourceFilePath";

    protected final static String OUTPUT_FILE_PATH_PARAM = "outFilePath";

    protected final static String OUTPUT_MIMETYPE_PARAM = "outputMimetype";

    protected final static String VIDEO_TOOLS_DIRECTORY = "NuxeoVideoTools";

    public VideoTool(String name, String commandLineName) {
        this.name = name;
        this.commandLineName = commandLineName;
    }

    public String getName() {
        return name;
    }

    public String getCommandLineName() {
        return commandLineName;
    }

    public Map<String, String> setupParameters(BlobHolder input, Map<String, Object> parameters) {
        Map<String, String> cmdParameters = new HashMap<>();
        cmdParameters.put(SOURCE_FILE_PATH_PARAM, input.getBlob().getFile().getAbsolutePath());
        return cmdParameters;
    }

    /**
     * Removes any temporary input files that were used for command execution.
     */
    public void cleanupInputs(Map<String, String> parameters) {
    }

    /**
     * Returns a {@link BlobHolder} containing the result of the command.
     *
     * @param mimeType the MIME type
     * @param parameters the parameters
     * @return the blob holder
     */
    public BlobHolder buildResult(String mimeType, Map<String, String> parameters) {
        String path = parameters.get(OUTPUT_FILE_PATH_PARAM);
        mimeType = parameters.getOrDefault(OUTPUT_MIMETYPE_PARAM, mimeType);
        Blob blob = getTemporaryBlob(path, mimeType);
        return new SimpleBlobHolder(blob);
    }

    /**
     * Gets a temporary blob for the given temporary path.
     * <p>
     * The temporary blob is backed by a temporary file in a new location. The old file is removed.
     *
     * @param path the path to a temporary file
     * @param mimeType the blob MIME type
     * @return a temporary {@link Blob}
     */
    public static Blob getTemporaryBlob(String path, String mimeType) {
        String ext = "." + FilenameUtils.getExtension(path);
        Blob blob;
        try {
            blob = new FileBlob(ext); // automatically tracked for removal
            Files.move(Paths.get(path), blob.getFile().toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        blob.setMimeType(mimeType);
        blob.setFilename(FilenameUtils.getName(path));
        return blob;
    }

}

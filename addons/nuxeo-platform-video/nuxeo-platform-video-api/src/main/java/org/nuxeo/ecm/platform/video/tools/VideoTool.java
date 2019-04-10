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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

    public BlobHolder buildResult(BlobHolder input, Map<String, String> cmdParams) {
        String outputFilename = cmdParams.get(OUTPUT_FILE_PATH_PARAM);
        Blob result = new FileBlob(new File(outputFilename));
        result.setMimeType(cmdParams.getOrDefault(OUTPUT_MIMETYPE_PARAM, input.getBlob().getMimeType()));
        result.setFilename(outputFilename);
        return new SimpleBlobHolder(result);
    }
}

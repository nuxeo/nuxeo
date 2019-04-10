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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.video.service.TestVideoService;

/**
 * @since 8.4
 */
public abstract class BaseVideoToolsTest {

    protected static final String TEST_VIDEO_WITH_CC = "test-data/ccdemo.mov";

    protected static final String TEST_VIDEO_SMALL = "DELTA.mp4";

    protected Blob getTestVideo(String filename) throws IOException {
        try (InputStream is = TestVideoService.class.getResourceAsStream("/" + filename)) {
            assertNotNull("Failed to load resource: " + filename, is);
            Blob blob = Blobs.createBlob(is, "video/mp4");
            blob.setFilename(FilenameUtils.getName(filename));
            return blob;
        }
    }

    protected String fileBlobToString(FileBlob inBlob) throws IOException {
        File f = inBlob.getFile();
        Path p = Paths.get(f.getAbsolutePath());
        StringBuilder builder = new StringBuilder();
        List<String> lines = Files.readAllLines(p, UTF_8);
        for (String line: lines) {
            builder.append(line);
        }
        return builder.toString();
    }
}

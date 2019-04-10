/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.importer.stream.consumer.watermarker;

import java.nio.file.Path;

/**
 * @since 10.2
 */
public interface Watermarker {

    /**
     * Creates a new file based on the input file by adding a watermark
     *
     * @param inputFile the source file
     * @param outputDir the directory to write the output file
     * @param watermark the watermark string to use
     * @return the path of the watermarked file
     */
    Path addWatermark(Path inputFile, Path outputDir, String watermark);

}

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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Append the watermark at the end of text file
 *
 * @since 10.2
 */
public class TextWatermarker extends AbstractWatermarker {

    @Override
    public Path addWatermark(Path inputFile, Path outputDir, String watermark) {
        try {
            Path output = getOutputPath(inputFile, outputDir, watermark);
            Files.copy(inputFile, output, StandardCopyOption.REPLACE_EXISTING);
            String footer = "\n" + watermark + "\n";
            Files.write(output, footer.getBytes(UTF_8), StandardOpenOption.APPEND);
            return output;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid text file: " + inputFile, e);
        }

    }
}

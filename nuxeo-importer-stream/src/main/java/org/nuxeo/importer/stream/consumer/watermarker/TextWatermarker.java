/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @since 10.1
 */
public class TextWatermarker extends AbstractWatermarker {

    @Override
    public Path addWatermark(Path inputFile, Path outputDir, String watermark) {
        try {
            byte[] encoded = Files.readAllBytes(inputFile);
            String content = new String(encoded, StandardCharsets.UTF_8);
            Path output = getOutputPath(inputFile, outputDir, watermark);
            String header = watermark + "\n";
            Files.write(output, header.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            Files.write(output, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            return output;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid text file: " + inputFile, e);
        }

    }
}

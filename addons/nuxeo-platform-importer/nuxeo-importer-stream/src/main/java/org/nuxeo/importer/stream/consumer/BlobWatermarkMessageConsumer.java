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
package org.nuxeo.importer.stream.consumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.importer.stream.consumer.watermarker.JpegWatermarker;
import org.nuxeo.importer.stream.consumer.watermarker.Mp4Watermarker;
import org.nuxeo.importer.stream.consumer.watermarker.TextWatermarker;
import org.nuxeo.importer.stream.consumer.watermarker.Watermarker;
import org.nuxeo.importer.stream.message.BlobMessage;

/**
 * Consume blobMessages and generate new blob by adding a unique watermark.
 *
 * @since 10.1
 */
public class BlobWatermarkMessageConsumer extends BlobMessageConsumer {

    protected final String prefix;

    protected final String generatedBlobPath;

    protected final Path outputPath;

    protected final Watermarker textWatermarker = new TextWatermarker();

    protected final Watermarker jpegWatermarker = new JpegWatermarker();

    protected final Watermarker mp4Watermarker = new Mp4Watermarker();

    /**
     * @param persistBlobPath when it is blank we don't delete generated blobs
     */
    public BlobWatermarkMessageConsumer(String consumerId, String blobProviderName, BlobInfoWriter blobInfoWriter,
            String watermarkPrefix, String persistBlobPath) {
        super(consumerId, blobProviderName, blobInfoWriter);
        this.prefix = watermarkPrefix;
        this.generatedBlobPath = persistBlobPath;
        if (StringUtils.isBlank(persistBlobPath)) {
            try {
                outputPath = Files.createTempDirectory("watermark");
            } catch (IOException e) {
                throw new NuxeoException("Failed to create temp dir", e);
            }
        } else {
            outputPath = Paths.get(persistBlobPath);
        }
    }

    @Override
    protected CloseableBlob getBlob(BlobMessage message) {
        String watermark = getWatermarkString();
        switch (message.getMimetype()) {
        case "text/plain":
            return addWatermark(message, watermark, textWatermarker);
        case "image/jpeg":
            return addWatermark(message, watermark, jpegWatermarker);
        case "video/mp4":
            return addWatermark(message, watermark, mp4Watermarker);
        default:
            return super.getBlob(message);
        }
    }

    protected CloseableBlob addWatermark(BlobMessage message, String watermark, Watermarker watermarker) {
        Path output = watermarker.addWatermark(Paths.get(message.getPath()), outputPath, watermark);
        Path fileToDelete = StringUtils.isBlank(generatedBlobPath) ? output : null;
        return new CloseableBlob(new FileBlob(output.toFile(), message.getMimetype()), fileToDelete);
    }

    protected String getWatermarkString() {
        return prefix + " " + System.currentTimeMillis();
    }
}

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.FileUtils;

/**
 * Adds a watermark to JPEG image by adding an exif metadata.
 * This trivial impl load the image in memory so don't use it on big pictures.
 * This is for testing purpose only.
 *
 * @since 10.2
 */
public class JpegWatermarker extends AbstractWatermarker {

    @Override
    public Path addWatermark(Path inputFile, Path outputDir, String watermark) {
        File jpegImageFile = inputFile.toFile();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            TiffOutputSet outputSet = null;
            ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (jpegMetadata != null) {
                TiffImageMetadata exif = jpegMetadata.getExif();
                if (exif != null) {
                    outputSet = exif.getOutputSet();
                }
            }
            if (outputSet == null) {
                outputSet = new TiffOutputSet();
            }
            final TiffOutputDirectory exifDirectory = outputSet.getOrCreateRootDirectory();
            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_SOFTWARE);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_SOFTWARE, watermark);
            new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
            os.flush();
            File output = getOutputPath(inputFile, outputDir, watermark).toFile();
            FileUtils.writeByteArrayToFile(output, os.toByteArray());
            return output.toPath();
        } catch (ImageReadException | ImageWriteException | IOException e) {
            throw new IllegalArgumentException("Unable to edit jpeg " + inputFile, e);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                // shade
            }
        }
    }
}

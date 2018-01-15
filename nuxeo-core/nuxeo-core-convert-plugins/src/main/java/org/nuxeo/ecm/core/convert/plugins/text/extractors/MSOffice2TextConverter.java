/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.poi.POITextExtractor;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.xmlbeans.XmlException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.runtime.api.Framework;

public class MSOffice2TextConverter implements Converter {

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        File f = null;

        try (POITextExtractor extractor = ExtractorFactory.createExtractor(blobHolder.getBlob().getStream())) {
            // TODO: find a way to distinguish headings from paragraphs using
            // WordExtractor#getParagraphText()?

            // Get extracted text with Unix end of line characters
            String extractedText = extractor.getText().replace("\r\n", "\n");

            byte[] bytes = extractedText.getBytes("UTF-8");
            f = Framework.createTempFile("po-msoffice2text", ".txt");
            try (OutputStream fas = new FileOutputStream(f)) {
                fas.write(bytes);
            }

            try (InputStream is = new FileInputStream(f)) {
                Blob blob = Blobs.createBlob(is, "text/plain", "UTF-8");
                return new SimpleCachableBlobHolder(blob);
            }
        } catch (IOException | OpenXML4JException | XmlException e) {
            throw new ConversionException("Error during MSOffice2Text conversion", e);
        } finally {
            if (f != null) {
                f.delete();
            }
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}

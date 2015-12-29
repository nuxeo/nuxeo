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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hwpf.extractor.WordExtractor;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * @deprecated subsumed by MSOffice2TextConverter
 */
@Deprecated
public class Word2TextConverter implements Converter {

    private static final Log log = LogFactory.getLog(Word2TextConverter.class);

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        File f = null;
        OutputStream fas = null;

        WordExtractor extractor = null;
        try {
            extractor = new WordExtractor(blobHolder.getBlob().getStream());
            byte[] bytes = extractor.getText().getBytes();
            f = File.createTempFile("po-word2text", ".txt");
            fas = new FileOutputStream(f);
            fas.write(bytes);

            Blob blob;
            try (InputStream in = new FileInputStream(f)) {
                blob = Blobs.createBlob(in);
            }
            blob.setMimeType("text/plain");

            return new SimpleCachableBlobHolder(blob);
        } catch (IOException e) {
            throw new ConversionException("Error during Word2Text conversion", e);
        } finally {
            if (extractor != null) {
                try {
                    extractor.close();
                } catch (IOException e) {
                    log.error(e, e);
                }
            }
            if (fas != null) {
                try {
                    fas.close();
                } catch (IOException e) {
                    log.error(e, e);
                }
            }
            if (f != null) {
                f.delete();
            }
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}

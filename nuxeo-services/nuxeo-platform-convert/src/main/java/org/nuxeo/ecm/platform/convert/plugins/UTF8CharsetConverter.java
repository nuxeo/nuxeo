/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class UTF8CharsetConverter implements Converter {

    private static final String TEXT_PREFIX = "text/";

    private static final String UTF_8 = "UTF-8";

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        Blob originalBlob = blobHolder.getBlob();
        String path = blobHolder.getFilePath();
        Blob transcodedBlob;
        try {
            transcodedBlob = convert(originalBlob);
        } catch (IOException | ConversionException e) {
            throw new ConversionException("Cannot transcode " + path + " to UTF-8", e);
        }
        return new SimpleBlobHolder(transcodedBlob);
    }

    protected Blob convert(Blob blob) throws IOException, ConversionException {
        String mimetype = blob.getMimeType();
        if (mimetype == null || !mimetype.startsWith(TEXT_PREFIX)) {
            return blob;
        }
        String encoding = blob.getEncoding();
        if (UTF_8.equals(encoding)) {
            return blob;
        }
        if (StringUtils.isEmpty(encoding)) {
            try (InputStream in = blob.getStream()) {
                encoding = detectEncoding(in);
            }
        }
        Blob newBlob;
        if (UTF_8.equals(encoding)) {
            // had no encoding previously, detected as UTF-8
            // just reuse the same blob
            try (InputStream in = blob.getStream()) {
                newBlob = Blobs.createBlob(in);
            }
        } else {
            // decode bytes as chars in the detected charset then encode chars as bytes in UTF-8
            try (InputStream in = new ReaderInputStream(new InputStreamReader(blob.getStream(), encoding), UTF_8)) {
                newBlob = Blobs.createBlob(in);
            }
        }
        newBlob.setMimeType(mimetype);
        newBlob.setEncoding(UTF_8);
        newBlob.setFilename(blob.getFilename());
        return newBlob;
    }

    protected String detectEncoding(InputStream in) throws IOException, ConversionException {
        if (!in.markSupported()) {
            // detector.setText requires mark
            in = new BufferedInputStream(in);
        }
        CharsetDetector detector = new CharsetDetector();
        detector.setText(in);
        CharsetMatch charsetMatch = detector.detect();
        if (charsetMatch == null) {
            throw new ConversionException("Cannot detect source charset.");
        }
        return charsetMatch.getName();
    }

}

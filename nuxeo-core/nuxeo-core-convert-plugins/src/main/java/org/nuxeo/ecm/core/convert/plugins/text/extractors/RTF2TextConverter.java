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
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import org.apache.commons.io.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.runtime.api.Framework;

public class RTF2TextConverter implements Converter {

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        File f = null;
        try {
            RTFEditorKit rtfParser = new RTFEditorKit();
            Document document = rtfParser.createDefaultDocument();
            rtfParser.read(blobHolder.getBlob().getStream(), document, 0);
            String text = document.getText(0, document.getLength());
            f = Framework.createTempFile("swing-rtf2text", ".txt");
            FileUtils.writeStringToFile(f, text, UTF_8);
            Blob blob;
            try (InputStream in = new FileInputStream(f)) {
                blob = Blobs.createBlob(in, "text/plain");
            }
            return new SimpleCachableBlobHolder(blob);
        } catch (IOException | BadLocationException e) {
            throw new ConversionException("Error during Word2Text conversion", e);
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

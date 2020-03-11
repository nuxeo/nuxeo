/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.xml.sax.SAXException;

public class XML2TextConverter implements Converter {

    @Override
    public BlobHolder convert(BlobHolder holder, Map<String, Serializable> parameters) throws ConversionException {
        return new SimpleBlobHolder(new StringBlob(convert(holder.getBlob())));
    }

    /**
     * @deprecated since 11.1. Use {@link #convert(Blob)} instead.
     */
    @Deprecated
    String convert(Blob blob, Map<String, Serializable> parameters) {
        return convert(blob);
    }

    protected String convert(Blob blob) {
        if (blob.getLength() == 0L) {
            return "";
        }
        try (InputStream stream = blob.getStream()) {
            Xml2TextHandler xml2text = new Xml2TextHandler();
            return xml2text.parse(stream);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new ConversionException("Error during XML2Text conversion", blob, e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }
}

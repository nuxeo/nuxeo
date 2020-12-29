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
import java.io.Serializable;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Base class that contains SAX based text extractor fallback
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public abstract class BaseOfficeXMLTextConverter implements Converter {

    public static final String MAX_SIZE = "MAX_SIZE";

    protected long maxSize4POI = 5 * 1024 * 1024L;

    public class FallbackXmlZip2TextConverter extends XmlZip2TextConverter {

        protected final String prefix;

        public FallbackXmlZip2TextConverter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        protected void readXmlZipContent(ZipInputStream zis, XMLReader reader, StringBuilder sb)
                throws IOException, SAXException {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                if ((zipEntry.getName().startsWith(prefix)) && (zipEntry.getName().endsWith(".xml"))) {
                    Xml2TextHandler xml2text;
                    try {
                        xml2text = new Xml2TextHandler();
                        sb.append(xml2text.parse(new InputSource(zis)));
                    } catch (ParserConfigurationException e) {
                        throw new IOException("Error during raw XML Text extraction", e);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }

    protected BlobHolder runFallBackConverter(BlobHolder blobHolder, final String prefix) throws ConversionException {
        Converter fallback = new FallbackXmlZip2TextConverter(prefix);
        return fallback.convert(blobHolder, new HashMap<String, Serializable>());
    }

    protected Blob runFallBackConverter(Blob blob, final String prefix) throws ConversionException {
        Converter fallback = new FallbackXmlZip2TextConverter(prefix);
        return fallback.convert(blob, new HashMap<String, Serializable>());
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        String max = descriptor.getParameters().get(MAX_SIZE);
        if (max != null) {
            maxSize4POI = Long.parseLong(max);
        }
    }

}

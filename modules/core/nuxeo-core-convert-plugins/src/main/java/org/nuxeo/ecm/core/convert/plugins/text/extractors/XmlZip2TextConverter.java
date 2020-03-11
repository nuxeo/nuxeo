/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * XML zip to text converter: parses the XML zip entries to read their content.
 */
public abstract class XmlZip2TextConverter implements Converter {

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setValidating(false);

        try {
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setFeature("http://xml.org/sax/features/validation", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            StringBuilder sb = new StringBuilder();
            UnclosableZipInputStream zis = new UnclosableZipInputStream(blobHolder.getBlob().getStream());
            try {
                readXmlZipContent(zis, reader, sb);
            } finally {
                zis.close(); // appease code analyzers
                zis.doClose(); // real close
            }
            return new SimpleCachableBlobHolder(Blobs.createBlob(sb.toString()));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new ConversionException("Error during OpenXml2Text conversion", blobHolder, e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

    protected abstract void readXmlZipContent(ZipInputStream zis, XMLReader reader, StringBuilder sb)
            throws IOException, SAXException;
}

/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
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

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setValidating(false);

        try {
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setFeature("http://xml.org/sax/features/validation", false);
            reader.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);

            StringBuilder sb = new StringBuilder();
            ZipInputStream zis = new ZipInputStream(
                    blobHolder.getBlob().getStream());
            try {
                readXmlZipContent(zis, reader, sb);
            } finally {
                zis.close();
            }
            return new SimpleCachableBlobHolder(new StringBlob(sb.toString()));
        } catch (Exception e) {
            throw new ConversionException(
                    "Error during OpenXml2Text conversion", e);
        }
    }

    public void init(ConverterDescriptor descriptor) {
    }

    protected abstract void readXmlZipContent(ZipInputStream zis,
            XMLReader reader, StringBuilder sb) throws IOException,
            SAXException;
}

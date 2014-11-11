/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.Serializable;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Based on Apache JackRabbit OOo converter.
 */
public class OOo2TextConverter implements Converter {

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        OOoXmlContentHandler contentHandler = new OOoXmlContentHandler();
        parserFactory.setValidating(false);

        try {
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setFeature("http://xml.org/sax/features/validation", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            ZipInputStream zis = new ZipInputStream(blobHolder.getBlob().getStream());
            ZipEntry zipEntry = zis.getNextEntry();
            while (!zipEntry.getName().equals("content.xml")) {
                zipEntry = zis.getNextEntry();
            }
            reader.setContentHandler(contentHandler);
            try {
                reader.parse(new InputSource(zis));
            } finally {
                zis.close();
            }
            return new SimpleCachableBlobHolder(new StringBlob(contentHandler.getContent()));
        }
        catch (Exception e) {
            throw new ConversionException("Error during OOo2Text conversion", e);
        }
    }

    public void init(ConverterDescriptor descriptor) {
        // TODO Auto-generated method stub
    }

}

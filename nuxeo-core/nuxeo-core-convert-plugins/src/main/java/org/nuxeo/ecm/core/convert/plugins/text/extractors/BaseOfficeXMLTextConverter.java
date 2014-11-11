/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

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
 *
 */
public abstract class BaseOfficeXMLTextConverter implements Converter {

    public static final String MAX_SIZE = "MAX_SIZE";

    protected long maxSize4POI = 5*1024*1014;

    protected BlobHolder runFallBackConverter(BlobHolder blobHolder, final String prefix) throws ConversionException{

        Converter fallback = new XmlZip2TextConverter() {
            @Override
            protected void readXmlZipContent(ZipInputStream zis, XMLReader reader,
                    StringBuilder sb) throws IOException, SAXException {
                ZipEntry zipEntry = zis.getNextEntry();

                while (zipEntry != null) {
                    if ((zipEntry.getName().startsWith(prefix)) && (zipEntry.getName().endsWith(".xml"))) {
                        Xml2TextHandler xml2text;
                        try {
                            xml2text = new Xml2TextHandler();
                            sb.append(xml2text.parse(new InputSource(zis)));
                        } catch (ParserConfigurationException    e) {
                            throw new IOException("Error during raw XML Text extraction", e);
                        }
                    }
                    zipEntry = zis.getNextEntry();
                }
            }
        };
        return fallback.convert(blobHolder, new HashMap<String, Serializable>());
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        String max = descriptor.getParameters().get(MAX_SIZE);
        if (max!=null) {
            maxSize4POI = Long.parseLong(max);
        }
    }

}

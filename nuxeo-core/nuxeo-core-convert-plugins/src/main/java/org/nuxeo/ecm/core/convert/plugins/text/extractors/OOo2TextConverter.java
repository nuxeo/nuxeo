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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Based on Apache JackRabbit OOo converter.
 */
public class OOo2TextConverter extends XmlZip2TextConverter {

    private static final String CONTENT_ZIP_ENTRY_NAME = "content.xml";

    protected void readXmlZipContent(ZipInputStream zis, XMLReader reader,
            StringBuilder sb) throws IOException, SAXException {

        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            if (CONTENT_ZIP_ENTRY_NAME.equals(zipEntry.getName())) {
                OOoXmlContentHandler contentHandler = new OOoXmlContentHandler();
                reader.setContentHandler(contentHandler);
                reader.parse(new InputSource(zis));
                sb.append(contentHandler.getContent());
                break;
            }
            zipEntry = zis.getNextEntry();
        }
    }
}

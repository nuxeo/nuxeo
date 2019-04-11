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

    @Override
    protected void readXmlZipContent(ZipInputStream zis, XMLReader reader, StringBuilder sb) throws IOException,
            SAXException {

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

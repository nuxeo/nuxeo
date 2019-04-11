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
 *
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Docx to text converter: parses the Open XML text document to read its content.
 */
public class DOCX2TextConverter extends XmlZip2TextConverter {

    private static final String WORD_DOCUMENT_ZIP_ENTRY_NAME = "word/document.xml";

    @Override
    protected void readXmlZipContent(ZipInputStream zis, XMLReader reader, StringBuilder sb) throws IOException,
            SAXException {

        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            if (WORD_DOCUMENT_ZIP_ENTRY_NAME.equals(zipEntry.getName())) {
                OpenXmlContentHandler contentHandler = new OpenXmlContentHandler();
                reader.setContentHandler(contentHandler);
                reader.parse(new InputSource(zis));
                sb.append(contentHandler.getContent());
                break;
            }
            zipEntry = zis.getNextEntry();
        }
    }
}

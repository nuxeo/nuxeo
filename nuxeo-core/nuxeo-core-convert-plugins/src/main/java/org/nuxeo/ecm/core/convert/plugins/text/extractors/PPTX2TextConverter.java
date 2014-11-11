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
 *
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.presentation.PresentationSlide;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Pptx to text converter: parses the Open XML presentation document to read its
 * content.
 */
public class PPTX2TextConverter extends XmlZip2TextConverter {

    protected static final Log log = LogFactory.getLog(PPTX2TextConverter.class);

    private static final String PRESENTATION_SLIDE_ZIP_ENTRY_NAME_PREFIX = "ppt/slides/slide";

    protected void readXmlZipContent(ZipInputStream zis, XMLReader reader,
            StringBuilder sb) throws IOException, SAXException {

        Set<PresentationSlide> slides = new TreeSet<PresentationSlide>();

        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            String zipEntryName = zipEntry.getName();
            if (zipEntryName.startsWith(PRESENTATION_SLIDE_ZIP_ENTRY_NAME_PREFIX)
                    && zipEntryName.length() > PRESENTATION_SLIDE_ZIP_ENTRY_NAME_PREFIX.length()) {
                char slideNumberChar = zipEntryName.charAt(PRESENTATION_SLIDE_ZIP_ENTRY_NAME_PREFIX.length());
                int slideNumber = -1;
                try {
                    slideNumber = Integer.parseInt(String.valueOf(slideNumberChar));
                } catch (NumberFormatException nfe) {
                    log.warn("Slide number is not an non integer, won't take this slide into account.");
                }
                if (slideNumber > -1) {
                    OpenXmlContentHandler contentHandler = new OpenXmlContentHandler();
                    reader.setContentHandler(contentHandler);
                    reader.parse(new InputSource(new ByteArrayInputStream(
                            IOUtils.toByteArray(zis))));
                    slides.add(new PresentationSlide(
                            contentHandler.getContent(), slideNumber));
                }
            }
            zipEntry = zis.getNextEntry();
        }
        if (!slides.isEmpty()) {
            Iterator<PresentationSlide> slidesIt = slides.iterator();
            while (slidesIt.hasNext()) {
                PresentationSlide slide = slidesIt.next();
                sb.append(slide.getContent());
                sb.append("\n");
            }
        }
    }
}

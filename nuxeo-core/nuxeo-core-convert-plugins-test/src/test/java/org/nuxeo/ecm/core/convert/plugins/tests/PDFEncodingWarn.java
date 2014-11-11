/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.core.convert.plugins.tests;

import java.io.IOException;
import java.net.URL;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.junit.Test;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.PDF2TextConverter;

/**
 * @author matic
 *
 */
public class PDFEncodingWarn {

    @Test
    public void extract() throws IOException {
        URL url = getClass().getResource("/test-docs/nutcracker.pdf");
        PDDocument doc = PDDocument.load(url);
        PDFTextStripper stripper = new PDF2TextConverter.PatchedPDFTextStripper();
        stripper.getText(doc);
        stripper.getText(doc);
    }

}

/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.core.convert.plugins.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.PDF2TextConverter;

/**
 * @author matic
 */
public class PDFEncodingWarn {

    @Test
    public void extract() throws IOException, URISyntaxException {
        URL url = getClass().getResource("/test-docs/nutcracker.pdf");
        File file = new File(url.toURI());
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDF2TextConverter.PatchedPDFTextStripper();
            String extractedText = stripper.getText(doc);
            assertTrue(extractedText.contains("The Nutcracker and the Mouse King"));
        }
    }

}

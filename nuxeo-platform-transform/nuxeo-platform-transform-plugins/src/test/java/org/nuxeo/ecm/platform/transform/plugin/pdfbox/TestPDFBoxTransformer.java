/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TestPDFBoxTransformer.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.plugin.pdfbox;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.DocumentTestUtils;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;

/**
 * Test the PDFBoxplugin for pdf to text transformation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestPDFBoxTransformer extends AbstractPluginTestCase {

    private Transformer transformer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        transformer = service.getTransformerByName("pdf2text");
    }

    @Override
    public void tearDown() throws Exception {
        transformer = null;
        super.tearDown();
    }

    public void testPDF2textConversion() throws Exception {
        String path = "test-data/hello.pdf";

        TransformDocumentImpl transformDocument = new TransformDocumentImpl(
                getBlobFromPath(path), "application/pdf");
        List<TransformDocument> results = transformer.transform(null,
                transformDocument);

        File textFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "txt");
        textFile.deleteOnExit();
        assertEquals("text content", "Hello  from  a PDF Document!",
                DocumentTestUtils.readContent(textFile));
        textFile.delete();
    }

}

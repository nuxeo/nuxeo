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
 * $Id: TestPDFBoxTransformService.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.pdfbox;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.DocumentTestUtils;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;

/**
 * Test the PDFBoxplugin for pdf to text requesting the transformer service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestPDFBoxTransformService extends AbstractPluginTestCase {

    private static final String TRANSFORMER_NAME = "pdf2text";

    public void testPDF2textConversion() throws Exception {
        String path = "test-data/hello.pdf";

        TransformDocumentImpl transformDocument = new TransformDocumentImpl(
                getBlobFromPath(path), "application/pdf");
        List<TransformDocument> results = service.transform(TRANSFORMER_NAME,
                null, transformDocument);

        File textFile = getFileFromInputStream(results.get(0).getBlob().getStream(),
                "txt");
        assertEquals("text content", "Hello  from  a PDF Document!",
                DocumentTestUtils.readContent(textFile));
    }

}

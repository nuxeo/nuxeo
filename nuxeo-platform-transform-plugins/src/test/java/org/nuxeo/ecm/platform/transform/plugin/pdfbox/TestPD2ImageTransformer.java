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
 * $Id: TestPDFBoxTransformer.java 18430 2007-05-09 13:48:36Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.plugin.pdfbox;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;

/**
 * Test the PDFBoxplugin for pdf to text transformation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestPD2ImageTransformer extends AbstractPluginTestCase {

    private Transformer transformer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        transformer = service.getTransformerByName("pdf2png");
    }

    @Override
    public void tearDown() throws Exception {
        transformer = null;
        super.tearDown();
    }

    public void testPDF2ImageConversion() throws Exception {
        String path = "test-data/hello.pdf";

        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path),
                        "application/pdf"));

        File outFile = getFileFromInputStream(results.get(0).getBlob().getStream(), "png");
        assertNotNull(outFile);
        outFile.delete();
    }

}

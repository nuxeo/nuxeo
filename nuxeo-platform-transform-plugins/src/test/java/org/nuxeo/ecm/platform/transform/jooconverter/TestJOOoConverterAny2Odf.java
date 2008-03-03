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
 * $Id: TestJOOoConverterAny2Odf.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.jooconverter;

import java.util.List;

import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;

/**
 *
 * Testing the joooconverter.
 * <p>
 * We request the transformation service directly here.
 * <p>
 * These tests are conveting basic documents to pdf
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestJOOoConverterAny2Odf extends AbstractPluginTestCase {

    public void testDoc2OdtConversion() throws Exception {
        String path = "test-data/hello.doc";
        List<TransformDocument> results = service.transform("any2odt", null,
                new TransformDocumentImpl(getBlobFromPath(path)));

        assertTrue(!results.isEmpty());
        assertEquals("application/vnd.oasis.opendocument.text",
                results.get(0).getMimetype());
    }

    public void testXls2OdsConversion() throws Exception {
        String path = "test-data/hello.xls";
        List<TransformDocument> results = service.transform("any2ods", null,
                new TransformDocumentImpl(getBlobFromPath(path)));

        assertTrue(!results.isEmpty());
        assertEquals("application/vnd.oasis.opendocument.spreadsheet",
                results.get(0).getMimetype());
    }

    public void testPpt2OdpConversion() throws Exception {
        String path = "test-data/hello.ppt";
        List<TransformDocument> results = service.transform("any2odp", null,
                new TransformDocumentImpl(getBlobFromPath(path)));

        assertTrue(!results.isEmpty());
        assertEquals("application/vnd.oasis.opendocument.presentation",
                results.get(0).getMimetype());
    }

    public void testSxw2OdtConversion() throws Exception {
        String path = "test-data/hello.sxw";
        List<TransformDocument> results = service.transform("any2odt", null,
                new TransformDocumentImpl(getBlobFromPath(path)));

        assertTrue(!results.isEmpty());
        assertEquals("application/vnd.oasis.opendocument.text",
                results.get(0).getMimetype());
    }

    public void testSxc2OdsConversion() throws Exception {
        String path = "test-data/hello.sxc";
        List<TransformDocument> results = service.transform("any2ods", null,
                new TransformDocumentImpl(getBlobFromPath(path)));

        assertTrue(!results.isEmpty());
        assertEquals("application/vnd.oasis.opendocument.spreadsheet",
                results.get(0).getMimetype());
    }

    public void xtestSxi2OdpConversion() throws Exception {
        String path = "test-data/hello.sxi";
        List<TransformDocument> results = service.transform("any2odp", null,
                new TransformDocumentImpl(getBlobFromPath(path)));

        assertTrue(!results.isEmpty());
        assertEquals("application/vnd.oasis.opendocument.presentation",
                results.get(0).getMimetype());
    }

}

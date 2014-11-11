/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestExportedDocument.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.dom4j.io.XMLWriter;
import org.jmock.Mock;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Tests ExportedDocument using fake DocumentModel class.
 */
public class TestExportedDocument extends NXRuntimeTestCase {

    public void testExportedDocument() throws Exception {

        Mock documentModelMock = mock(DocumentModel.class);
        documentModelMock.expects(atLeastOnce()).method("getId").will(
                returnValue("My id"));
        documentModelMock.expects(atLeastOnce()).method("getType").will(
                returnValue("My type"));
        documentModelMock.expects(atLeastOnce()).method("getRef").will(
                returnValue(new IdRef("My id")));
        documentModelMock.expects(atLeastOnce()).method("getName").will(
                returnValue(null));
        documentModelMock.expects(atLeastOnce()).method(
                "getCurrentLifeCycleState").will(returnValue(null));
        documentModelMock.expects(atLeastOnce()).method("getLifeCyclePolicy").will(
                returnValue(null));
        documentModelMock.expects(atLeastOnce()).method("getACP").will(
                returnValue(null));
        documentModelMock.expects(atLeastOnce()).method("getSchemas").will(
                returnValue(new String[0]));
        documentModelMock.expects(atLeastOnce()).method("getRepositoryName").will(
                returnValue(null));
        documentModelMock.expects(atLeastOnce()).method("getPath").will(
                returnValue(new Path("my-path")));
        documentModelMock.expects(atLeastOnce()).method("getPathAsString").will(
                returnValue("/my/path/"));

        DocumentModel model = (DocumentModel) documentModelMock.proxy();
        ExportedDocument exportedDoc = new ExportedDocumentImpl(model);

        assertEquals("My id", exportedDoc.getId());
        assertEquals("My type", exportedDoc.getType());
        assertEquals("my-path", exportedDoc.getPath().toString());

        // Check XML output.
        Writer writer = new StringWriter();
        XMLWriter xmlWriter = new XMLWriter(writer);
        xmlWriter.write(exportedDoc.getDocument());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<document id=\"My id\"><system><type>My type</type>"
                + "<path>my-path</path><access-control/></system></document>",
                writer.toString());

        // Check ZIP output.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NuxeoArchiveWriter archWriter = new NuxeoArchiveWriter(out);
        archWriter.write(exportedDoc);

        // Reimport exported stuff.
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        NuxeoArchiveReader archReader = new NuxeoArchiveReader(in);
        ExportedDocument newExportedDoc = archReader.read();
        assertEquals(exportedDoc.getId(), newExportedDoc.getId());
        assertEquals(exportedDoc.getPath(), newExportedDoc.getPath());
        assertEquals(exportedDoc.getType(), newExportedDoc.getType());
    }

}

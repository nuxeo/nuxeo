/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestExportedDocument.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.dom4j.io.XMLWriter;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Tests ExportedDocument using fake DocumentModel class.
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestExportedDocument {

    protected Mockery jmcontext = new JUnit4Mockery();

    @Test
    public void testExportedDocument() throws Exception {

        final DocumentModel model = jmcontext.mock(DocumentModel.class);
        jmcontext.checking(new Expectations() {
            {
                atLeast(1).of(model).getId();
                will(returnValue("My id"));
                atLeast(1).of(model).getType();
                will(returnValue("My type"));
                atLeast(1).of(model).getRef();
                will(returnValue(new IdRef("My id")));
                atLeast(1).of(model).getName();
                will(returnValue(null));
                atLeast(1).of(model).getCurrentLifeCycleState();
                will(returnValue(null));
                atLeast(1).of(model).getLifeCyclePolicy();
                will(returnValue(null));
                atLeast(1).of(model).getACP();
                will(returnValue(null));
                atLeast(1).of(model).getSchemas();
                will(returnValue(new String[0]));
                atLeast(1).of(model).getRepositoryName();
                will(returnValue(null));
                atLeast(1).of(model).getPath();
                will(returnValue(new Path("my-path")));
                atLeast(1).of(model).getPathAsString();
                will(returnValue("/my/path/"));
                atLeast(1).of(model).getFacets();
            }
        });

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
                + "<path>my-path</path><access-control/></system></document>", writer.toString());

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

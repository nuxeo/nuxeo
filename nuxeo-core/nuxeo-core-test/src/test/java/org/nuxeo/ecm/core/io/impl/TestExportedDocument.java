/*
 * (C) Copyright 2006-2021 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core.io.impl;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.inject.Inject;

import org.dom4j.io.XMLWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests ExportedDocument using fake DocumentModel class.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestExportedDocument {

    @Inject
    protected CoreSession session;

    @Test
    public void testExportedDocument() throws Exception {
        doTestExportedDocument();
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-archive-exporter-extra-disable.xml")
    public void testExportedDocumentWithoutExtraField() throws Exception {
        doTestExportedDocument();
    }

    public void doTestExportedDocument() throws Exception {
        DocumentModel model = session.createDocumentModel("/", "myfile", "File");
        model.setPropertyValue("dc:title", "hello world");
        model.setPropertyValue("dc:description", "foo\nbar");
        model = session.createDocument(model);

        ExportedDocument exportedDoc = new ExportedDocumentImpl(model);

        assertEquals(model.getId(), exportedDoc.getId());
        assertEquals("File", exportedDoc.getType());
        assertEquals("myfile", exportedDoc.getPath().toString());

        // Check XML output.
        Writer writer = new StringWriter();
        XMLWriter xmlWriter = new XMLWriter(writer);
        xmlWriter.write(exportedDoc.getDocument());

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
                "<document repository=\"test\" id=\"" + model.getId() + "\">" + //
                "  <system>" + //
                "    <type>File</type>" + //
                "    <path>myfile</path>" + //
                "    <lifecycle-state>project</lifecycle-state>" + //
                "    <lifecycle-policy>default</lifecycle-policy>" + //
                "    <facet>Versionable</facet>" + //
                "    <facet>Publishable</facet>" + //
                "    <facet>Commentable</facet>" + //
                "    <facet>HasRelatedText</facet>" + //
                "    <facet>Downloadable</facet>" + //
                "    <access-control>" + //
                "      <acl name=\"inherited\">" + //
                "        <entry principal=\"administrators\" permission=\"Everything\" grant=\"true\"/>" + //
                "        <entry principal=\"Administrator\" permission=\"Everything\" grant=\"true\"/>" + //
                "        <entry principal=\"members\" permission=\"Read\" grant=\"true\"/>" + //
                "      </acl>" + //
                "    </access-control>" + //
                "  </system>" + //
                "  <schema xmlns:uid=\"http://project.nuxeo.com/geide/schemas/uid/\" name=\"uid\">" + //
                "    <uid:major_version><![CDATA[0]]></uid:major_version>" + //
                "    <uid:minor_version><![CDATA[0]]></uid:minor_version>" + //
                "  </schema>" + //
                "  <schema xmlns:file=\"http://www.nuxeo.org/ecm/schemas/file/\" name=\"file\"></schema>" + //
                "  <schema xmlns:common=\"http://www.nuxeo.org/ecm/schemas/common/\" name=\"common\"></schema>" + //
                "  <schema xmlns:files=\"http://www.nuxeo.org/ecm/schemas/files/\" name=\"files\">" + //
                "    <files:files/>" + //
                "  </schema>" + //
                "  <schema xmlns:dc=\"http://www.nuxeo.org/ecm/schemas/dublincore/\" name=\"dublincore\">" + //
                "    <dc:description><![CDATA[foo\nbar]]></dc:description>" + //
                "    <dc:title><![CDATA[hello world]]></dc:title>" + //
                "  </schema>" + //
                "  <schema xmlns:relatedtext=\"http://www.nuxeo.org/ecm/schemas/relatedtext/\" name=\"relatedtext\">" + //
                "    <relatedtext:relatedtextresources/>" + //
                "  </schema>" + //
                "</document>" + //
                "";
        assertEquals(expected.replace("  ", ""), writer.toString());

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

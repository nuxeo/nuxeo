/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.context.extensions.AuditExtensionFactory;
import org.nuxeo.template.processors.xdocreport.ZipXmlHelper;

public class TestAuditEntriesODTProcessing extends SimpleTemplateDocTestCase {

    @Test
    public void testRenderWithAuditEntries() throws Exception {

        // build fake AuditEntries
        ArrayList<LogEntry> auditEntries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LogEntryImpl entry = new LogEntryImpl();
            entry.setId(i);
            entry.setComment("Comment" + i);
            entry.setCategory("Testing");
            entry.setEventId("TestEvent" + i);
            entry.setPrincipalName("TestingUser");
            auditEntries.add(entry);
        }
        AuditExtensionFactory.testAuditEntries = auditEntries;

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        TemplateSourceDocument template = adapter.getSourceTemplate(TEMPLATE_NAME);
        assertNotNull(template);

        List<TemplateInput> params = template.getParams();
        assertEquals(0, params.size());

        Blob rendered = adapter.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(rendered);

        String xmlContent = ZipXmlHelper.readXMLContent(rendered, ZipXmlHelper.OOO_MAIN_FILE);

        assertTrue(xmlContent.contains("TestEvent0"));
        assertTrue(xmlContent.contains("TestEvent1"));
        assertTrue(xmlContent.contains("TestEvent2"));
        assertTrue(xmlContent.contains("TestEvent3"));
        assertTrue(xmlContent.contains("TestEvent4"));
    }

    @Override
    protected Blob getTemplateBlob() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/auditTest.odt");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("auditTest.odt");
        return fileBlob;
    }

}

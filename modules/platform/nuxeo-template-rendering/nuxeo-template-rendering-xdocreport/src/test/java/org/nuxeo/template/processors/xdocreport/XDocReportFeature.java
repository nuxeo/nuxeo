/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.template.processors.xdocreport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * @since 2025.14
 */
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.template.manager.api")
@Deploy("org.nuxeo.template.manager")
@Deploy("org.nuxeo.template.manager.xdocreport")
@Features(CoreFeature.class)
public class XDocReportFeature implements RunnerFeature {

    public TemplateSourceDocument createTemplateDocument(CoreSession session, String templateName, Blob templateBlob) {
        // create template
        DocumentModel templateDoc = session.createDocumentModel("/", templateName, "TemplateSource");
        templateDoc.setPropertyValue("dc:title", "Template for test: " + templateName);
        templateDoc.setPropertyValue("tmpl:templateName", templateName);
        templateDoc.setPropertyValue("file:content", (Serializable) templateBlob);
        templateDoc = session.createDocument(templateDoc);

        // assert the template has been created properly
        var templateSource = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(templateSource);
        assertEquals(templateName, templateSource.getName());

        return templateSource;
    }
}

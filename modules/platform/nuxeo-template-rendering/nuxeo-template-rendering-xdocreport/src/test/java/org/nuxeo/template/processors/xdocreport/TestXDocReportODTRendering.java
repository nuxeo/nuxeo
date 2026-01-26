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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

/**
 * @since 2025.14
 */
@RunWith(FeaturesRunner.class)
@Features(XDocReportFeature.class)
public class TestXDocReportODTRendering {

    @Inject
    protected CoreSession session;

    @Inject
    protected XDocReportFeature xDocReportFeature;

    @Test
    public void testSafeRendering() throws IOException {
        // instantiate the template
        var templateBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext("data/safeDoc.odt"));
        var templateDocument = xDocReportFeature.createTemplateDocument(session, "WhoAmI", templateBlob);

        // create the source document
        var sourceDocument = session.createDocumentModel("/", "sourceDocument", "TemplateBasedFile");
        sourceDocument = session.createDocument(sourceDocument);
        var templateSourceAdapter = sourceDocument.getAdapter(TemplateBasedDocument.class);
        templateSourceAdapter.setTemplate(templateDocument.getAdaptedDoc(), false);

        var exception = assertThrows(NuxeoException.class, () -> templateSourceAdapter.renderWithTemplate("WhoAmI"));
        assertEquals("Failed to render template: WhoAmI", exception.getMessage());
        Throwable cause = exception.getCause();
        assertTrue("Cause is not an IOException", cause instanceof IOException);
        String causeMessage = cause.getMessage();
        assertTrue("Failure is not the expected one, cause: " + cause, causeMessage.contains(
                "Instantiating freemarker.template.utility.Execute is not allowed in the template for security reasons."));
    }
}

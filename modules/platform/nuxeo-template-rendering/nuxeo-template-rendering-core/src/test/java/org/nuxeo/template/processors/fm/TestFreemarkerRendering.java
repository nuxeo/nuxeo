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
package org.nuxeo.template.processors.fm;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
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
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.template.RenderingCoreFeature;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

import freemarker.template.TemplateException;

/**
 * @since 2025.14
 */
@RunWith(FeaturesRunner.class)
@Features(RenderingCoreFeature.class)
public class TestFreemarkerRendering {

    @Inject
    protected CoreSession session;

    @Inject
    protected RenderingCoreFeature renderingFeature;

    // NXP-33482
    @Test
    @LoggerLevel(name = "freemarker.runtime", level = "FATAL") // hide inline template error
    public void testSafeRendering() throws IOException {
        // instantiate the template
        var templateBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext("data/safeDoc.ftl"));
        var templateDocument = renderingFeature.createTemplateDocument(session, "WhoAmI", templateBlob);

        // create the source document
        var sourceDocument = session.createDocumentModel("/", "sourceDocument", "TemplateBasedFile");
        sourceDocument = session.createDocument(sourceDocument);
        var templateSourceAdapter = sourceDocument.getAdapter(TemplateBasedDocument.class);
        templateSourceAdapter.setTemplate(templateDocument.getAdaptedDoc(), false);

        var exception = assertThrows(NuxeoException.class, () -> templateSourceAdapter.renderWithTemplate("WhoAmI"));
        assertTrue("Message is not the expected one: " + exception.getMessage(),
                exception.getMessage().startsWith("Unable to render the template"));
        assertEquals(SC_BAD_REQUEST, exception.getStatusCode());
        Throwable cause = exception.getCause();
        assertTrue("Cause is not a TemplateException", cause instanceof TemplateException);
        String causeMessage = cause.getMessage();
        assertTrue("Failure is not the expected one, cause: " + cause, causeMessage.contains(
                "Instantiating freemarker.template.utility.Execute is not allowed in the template for security reasons."));
    }
}

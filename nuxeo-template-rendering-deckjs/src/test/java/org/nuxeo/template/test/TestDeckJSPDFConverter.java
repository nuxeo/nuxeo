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
 *     ldoguin
 */
package org.nuxeo.template.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.deckjs.DeckJSConverterConstants;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.template.manager.api")
@Deploy("org.nuxeo.template.manager")
@Deploy("org.nuxeo.template.manager.jaxrs")
@Deploy("org.nuxeo.template.deckjs")
@Deploy("org.nuxeo.template.manager.samples")
@Deploy("studio.extensions.template-module-demo")
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
public class TestDeckJSPDFConverter {

    @Inject
    protected CoreSession session;

    @Inject
    protected CommandLineExecutorService cles;

    @Test
    public void testSampleDocument() throws Exception {
        CommandAvailability commandAvailability = cles.getCommandAvailability(DeckJSConverterConstants.PHANTOM_JS_COMMAND_NAME);
        assumeTrue(DeckJSConverterConstants.PHANTOM_JS_COMMAND_NAME + " not available",
                commandAvailability.isAvailable());

        PathRef ref = new PathRef("/default-domain/workspaces/templatesamples/rawsamples/");
        DocumentModel sampleFolder = session.getDocument(ref);
        assertNotNull(sampleFolder);

        ref = new PathRef("/default-domain/workspaces/templatesamples/rawsamples/roadmap");
        DocumentModel sampleDoc = session.getDocument(ref);

        TemplateBasedDocument sampleTemplate = sampleDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(sampleTemplate);

        List<String> templateNames = sampleTemplate.getTemplateNames();
        assertEquals(1, templateNames.size());
        assertEquals("NuxeoWorld2K12HtmlSlides", templateNames.get(0));

        Blob blob = sampleTemplate.renderWithTemplate("NuxeoWorld2K12HtmlSlides");
        assertNotNull(blob);
        assertTrue(blob.getLength() > 0);
    }

}

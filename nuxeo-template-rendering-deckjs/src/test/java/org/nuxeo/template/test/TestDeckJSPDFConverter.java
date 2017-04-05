/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 */
package org.nuxeo.template.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.deckjs.DeckJSConverterConstants;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.content.template",
        "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.core.event",
        "org.nuxeo.ecm.core.convert.api", "org.nuxeo.ecm.core.convert",
        "org.nuxeo.ecm.core.convert.plugins",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.template.manager.api", "org.nuxeo.template.manager",
        "org.nuxeo.template.manager.jaxrs", "org.nuxeo.template.deckjs" })
public class TestDeckJSPDFConverter {

    @Inject
    protected CoreSession session;

    @Test
    @Ignore("NXP-22051")
    public void testSampleDocument() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);
        CommandAvailability commandAvailability = cles.getCommandAvailability(DeckJSConverterConstants.PHANTOM_JS_COMMAND_NAME);
        assumeTrue(DeckJSConverterConstants.PHANTOM_JS_COMMAND_NAME + " not available",
                commandAvailability.isAvailable());

        PathRef ref = new PathRef("default-domain/workspaces/templatesamples/");
        DocumentModel sampleFolder = session.getDocument(ref);
        assertNotNull(sampleFolder);

        ref = new PathRef("default-domain/workspaces/templatesamples/roadmap");
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

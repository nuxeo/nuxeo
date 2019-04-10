/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.suggestbox.service.registries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionService;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionServiceImpl;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggestionHandlerDescriptor;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({
        "org.nuxeo.ecm.platform.suggestbox.core:OSGI-INF/suggestbox-service.xml",
        "org.nuxeo.ecm.platform.suggestbox.jsf:OSGI-INF/suggestbox-operations-contrib.xml",
        "org.nuxeo.ecm.platform.suggestbox.jsf.test:OSGI-INF/test-suggestion-handlers-contrib.xml" })
public class SuggestionHandlerMergeTest {

    @Inject
    protected SuggestionService suggestionService;

    /**
     * Tests the suggestionHandler contribution merge.
     */
    @Test
    public void testSuggestionHandlerMerge() throws ClientException {

        // check suggestionHandlers registry
        SuggestionHandlerRegistry suggestionHandlers = ((SuggestionServiceImpl) suggestionService).getSuggestionHandlers();
        assertNotNull(suggestionHandlers);

        // check suggestionHandlers count
        assertNotNull(suggestionHandlers.getFragments());
        assertEquals(5, suggestionHandlers.getFragments().length);

        // check 'jsfSearchDocuments' suggestionHandler (must be merged:
        // different operation)
        SuggestionHandlerDescriptor shd = suggestionHandlers.getSuggestionHandlerDescriptor("jsfSearchDocuments");
        assertNotNull(shd);
        assertEquals("searchDocuments", shd.getType());
        assertEquals("searchbox", shd.getSuggesterGroup());
        assertEquals("Custom.Suggestion.JSF.NavigateToFacetedSearch",
                shd.getOperation());
        assertNull(shd.getOperationChain());

        // check 'jsfNavigateToDocument' suggestionHandler (must be merged:
        // different type)
        shd = suggestionHandlers.getSuggestionHandlerDescriptor("jsfNavigateToDocument");
        assertNotNull(shd);
        assertEquals("searchDocuments", shd.getType());
        assertEquals("searchbox", shd.getSuggesterGroup());
        assertEquals("Suggestion.JSF.NavigateToDocument", shd.getOperation());
        assertNull(shd.getOperationChain());

        // check 'jsfNavigateToUser' suggestionHandler (no change)
        shd = suggestionHandlers.getSuggestionHandlerDescriptor("jsfNavigateToUser");
        assertNotNull(shd);
        assertEquals("user", shd.getType());
        assertEquals("searchbox", shd.getSuggesterGroup());
        assertEquals("Suggestion.JSF.NavigateToUser", shd.getOperation());
        assertNull(shd.getOperationChain());

        // check 'jsfNavigateToGroup' suggestionHandler (no change)
        shd = suggestionHandlers.getSuggestionHandlerDescriptor("jsfNavigateToGroup");
        assertNotNull(shd);
        assertEquals("group", shd.getType());
        assertEquals("searchbox", shd.getSuggesterGroup());
        assertEquals("Suggestion.JSF.NavigateToGroup", shd.getOperation());
        assertNull(shd.getOperationChain());

        // check 'newHandler' suggestionHandler (new congtribution)
        shd = suggestionHandlers.getSuggestionHandlerDescriptor("jsfNavigateToGroup");
        assertNotNull(shd);
    }
}

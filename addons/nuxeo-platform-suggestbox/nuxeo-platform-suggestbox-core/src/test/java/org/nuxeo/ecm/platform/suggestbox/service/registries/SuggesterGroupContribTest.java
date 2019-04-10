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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.suggestbox.service.registries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionService;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionServiceImpl;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupItemDescriptor;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.suggestbox.core" })
public class SuggesterGroupContribTest {

    @Inject
    protected SuggestionService suggestionService;

    /**
     * Tests the 'searchbox' suggesterGroup contribution.
     */
    @Test
    // TODO change the test when the redirection to the new search tab will be handled
    public void testSuggesterGroupContrib() throws ClientException {

        // check service implementation
        assertTrue(suggestionService instanceof SuggestionServiceImpl);

        // check suggesterGroup registry
        SuggesterGroupRegistry suggesterGroups = ((SuggestionServiceImpl) suggestionService).getSuggesterGroups();
        assertNotNull(suggesterGroups);

        // check suggesterGroup count
        assertNotNull(suggesterGroups.getFragments());
        assertEquals(1, suggesterGroups.getFragments().length);

        // check 'searchbox' suggesterGroup
        SuggesterGroupDescriptor sgd = suggesterGroups.getSuggesterGroupDescriptor("searchbox");
        assertNotNull(sgd);

        // check 'searchbox' suggesterGroup's suggesters
        List<SuggesterGroupItemDescriptor> suggesters = sgd.getSuggesters();
        List<SuggesterGroupItemDescriptor> expectedSuggesters = new ArrayList<SuggesterGroupItemDescriptor>();
        /*expectedSuggesters.add(new SuggesterGroupItemDescriptor(
                "searchByKeywords"));*/
        expectedSuggesters.add(new SuggesterGroupItemDescriptor(
                "documentLookupByTitle"));
        expectedSuggesters.add(new SuggesterGroupItemDescriptor(
                "searchByUsersAndGroups"));
        /*expectedSuggesters.add(new SuggesterGroupItemDescriptor("searchByDate"));*/
        assertEquals(expectedSuggesters, suggesters);

    }
}

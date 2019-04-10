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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.suggestbox.service.registries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionService;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionServiceImpl;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupItemDescriptor;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.suggestbox.core")
public class SuggesterGroupContribTest {

    @Inject
    protected SuggestionService suggestionService;

    /**
     * Tests the 'searchbox' suggesterGroup contribution.
     */
    @Test
    // TODO change the test when the redirection to the new search tab will be handled
    public void testSuggesterGroupContrib() {

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
        /*
         * expectedSuggesters.add(new SuggesterGroupItemDescriptor( "searchByKeywords"));
         */
        expectedSuggesters.add(new SuggesterGroupItemDescriptor("documentLookupByTitle"));
        expectedSuggesters.add(new SuggesterGroupItemDescriptor("searchByUsersAndGroups"));
        /* expectedSuggesters.add(new SuggesterGroupItemDescriptor("searchByDate")); */
        assertEquals(expectedSuggesters, suggesters);

    }
}

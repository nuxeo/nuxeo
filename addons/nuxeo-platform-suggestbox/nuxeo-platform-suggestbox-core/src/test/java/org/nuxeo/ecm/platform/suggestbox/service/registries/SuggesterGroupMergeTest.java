/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
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
@Deploy("org.nuxeo.ecm.platform.suggestbox.core.test")
public class SuggesterGroupMergeTest {

    @Inject
    protected SuggestionService suggestionService;

    /**
     * Tests the merge of the 'searchbox' suggesterGroup contribution.
     * <p>
     * Before merge, the list of suggesters for this suggesterGroup is: ["searchByKeywords", "documentLookupByTitle",
     * "searchByUsersAndGroups", "searchByDate"]
     * <p>
     * After merge, it should be: ["myNewSuggesterBegin", "searchByKeywords", "myNewSuggesterBeforeUsers",
     * "searchByUsersAndGroups", "myNewSuggesterAfterUsers", "searchByDate", "myNewSuggesterEnd",
     * "myNewSuggesterVeryEnd"]
     * <p>
     * See following steps for details.
     * <p>
     * 1/ Remove a non-existent suggester 'nonExistentSuggester' => nothing should be done.
     * <p>
     * 2/ Remove an existing suggester 'documentLookupByTitle' => should be removed.
     * <p>
     * 3/ Append a suggester with the name of an existing suggester 'searchByUsersAndGroups' => nothing should be done.
     * <p>
     * 4/ Append a suggester 'myNewSuggesterBegin' before a non-existent suggester => should be appended at the
     * beginning of the suggesters list.
     * <p>
     * 5/ Append a suggester 'myNewSuggesterBeforeUsers' before an existing suggester 'searchByUsersAndGroups' => should
     * be appended before the existing suggester.
     * <p>
     * 6/ Append a suggester 'myNewSuggesterEnd' after a non-existent suggester => should be appended at the end of the
     * suggesters list.
     * <p>
     * 7/ Append a suggester 'myNewSuggesterAfterUsers' after an existing suggester 'searchByUsersAndGroups' => should
     * be appended after the existing suggester.
     * <p>
     * 8/ Append a suggester 'myNewSuggesterVeryEnd' with no particular attributes => should be appended at the end of
     * the suggesters list.
     */
    @Test
    // TODO change the test when the redirection to the new search tab will be handled
    public void testSuggesterGroupMerge() {

        // check service implementation
        assertTrue(suggestionService instanceof SuggestionServiceImpl);

        // check suggesterGroup registry
        SuggesterGroupRegistry suggesterGroups = ((SuggestionServiceImpl) suggestionService).getSuggesterGroups();
        assertNotNull(suggesterGroups);

        // check service supports merge
        assertTrue(suggesterGroups.isSupportingMerge());

        // check 'searchbox' suggesterGroup
        SuggesterGroupDescriptor sgd = suggesterGroups.getSuggesterGroupDescriptor("searchbox");
        assertNotNull(sgd);

        // check 'searchbox' suggesterGroup's merged suggesters
        List<SuggesterGroupItemDescriptor> suggesters = sgd.getSuggesters();
        List<SuggesterGroupItemDescriptor> expectedSuggesters = new ArrayList<SuggesterGroupItemDescriptor>();
        expectedSuggesters.add(new SuggesterGroupItemDescriptor("myNewSuggesterBegin"));
        /*
         * expectedSuggesters.add(new SuggesterGroupItemDescriptor( "searchByKeywords"));
         */
        expectedSuggesters.add(new SuggesterGroupItemDescriptor("myNewSuggesterBeforeUsers"));
        expectedSuggesters.add(new SuggesterGroupItemDescriptor("searchByUsersAndGroups"));
        expectedSuggesters.add(new SuggesterGroupItemDescriptor("myNewSuggesterAfterUsers"));
        // expectedSuggesters.add(new SuggesterGroupItemDescriptor("searchByDate"));
        expectedSuggesters.add(new SuggesterGroupItemDescriptor("myNewSuggesterEnd"));
        expectedSuggesters.add(new SuggesterGroupItemDescriptor("myNewSuggesterVeryEnd"));
        assertEquals(expectedSuggesters, suggesters);
    }
}

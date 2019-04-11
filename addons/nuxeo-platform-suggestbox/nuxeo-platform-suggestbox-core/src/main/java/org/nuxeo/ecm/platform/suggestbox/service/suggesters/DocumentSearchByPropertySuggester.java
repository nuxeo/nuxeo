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
package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;
import org.nuxeo.ecm.platform.suggestbox.service.SearchDocumentsSuggestion;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;

/**
 * Simple stateless document search suggester that propose to use the user input for searching a specific field.
 *
 * @deprecated since 6.0. This suggester is not used anymore with the new search tab.
 */
@Deprecated
public class DocumentSearchByPropertySuggester implements Suggester {

    protected String searchField = "fsd:ecm_fulltext";

    protected String suggesterId = "DocumentSearchByPropertySuggester";

    /**
     * @since 5.8
     */
    protected String[] searchFields;

    protected String label = "label.searchDocumentsByKeywords";

    protected String description = "";

    protected String iconURL = "/img/facetedSearch.png";

    protected boolean disabled;

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context) throws SuggestionException {
        I18nHelper i18n = I18nHelper.instanceFor(context.messages);
        String i18nLabel = i18n.translate(label, userInput);
        SearchDocumentsSuggestion suggestion = new SearchDocumentsSuggestion(suggesterId, i18nLabel, iconURL).withSearchCriterion(
                searchField, userInput);
        if (searchFields != null) {
            for (String field : searchFields) {
                suggestion.withSearchCriterion(field, userInput);
            }
        }
        if (disabled) {
            suggestion.disable();
        }
        if (description != null) {
            suggestion.withDescription(i18n.translate(description, userInput));
        }
        return Collections.<Suggestion> singletonList(suggestion);
    }

    @Override
    public void initWithParameters(SuggesterDescriptor descriptor) throws ComponentInitializationException {
        Map<String, String> params = descriptor.getParameters();
        searchField = params.get("searchField");
        label = params.get("label");
        String iconURL = params.get("iconURL");
        if (iconURL != null) {
            this.iconURL = iconURL;
        }
        description = params.get("description");
        String disabled = params.get("disabled");
        if (disabled != null) {
            this.disabled = Boolean.parseBoolean(disabled);
        }
        String psearchFields = params.get("searchFields");
        if (psearchFields != null) {
            searchFields = psearchFields.split(", *");
        }
        if (label == null || (searchField == null && searchFields == null)) {
            throw new ComponentInitializationException(String.format("Could not initialize suggester '%s': "
                    + "label, searchField (or searchFields)" + " are mandatory parameters", descriptor.getName()));
        }
    }

}

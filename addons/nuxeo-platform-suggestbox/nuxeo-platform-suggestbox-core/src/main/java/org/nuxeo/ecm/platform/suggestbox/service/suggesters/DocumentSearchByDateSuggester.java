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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.suggestbox.service.CommonSuggestionTypes;
import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;
import org.nuxeo.ecm.platform.suggestbox.service.SearchDocumentsSuggestion;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.suggestbox.utils.DateMatcher;

/**
 * Simple stateless suggester that parses the input and suggest to search document by date if the input can be
 * interpreted as a date in the user locale.
 *
 * @deprecated since 6.0. This suggester is not used anymore with the new search tab.
 */
@Deprecated
public class DocumentSearchByDateSuggester implements Suggester {

    static final String type = CommonSuggestionTypes.SEARCH_DOCUMENTS;

    static final String LABEL_BEFORE_PREFIX = "label.search.beforeDate_";

    static final String LABEL_AFTER_PREFIX = "label.search.afterDate_";

    protected String[] searchFields;

    protected String label;

    protected String iconURL;

    protected String suggesterId = "DocumentSearchByDateSuggester";

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context) throws SuggestionException {
        List<Suggestion> suggestions = new ArrayList<>();
        I18nHelper i18n = I18nHelper.instanceFor(context.messages);

        // TODO: use SimpleDateFormat and use the locale information from the
        // context
        DateMatcher matcher = DateMatcher.fromInput(userInput);
        DateFormat labelDateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM, context.locale);

        if (matcher.hasMatch()) {
            Date date = matcher.getDateSuggestion().getTime();
            String formattedDateLabel = labelDateFormatter.format(date);

            for (String field : searchFields) {
                String searchFieldAfter = field + "_min";
                String labelAfterPrefix = LABEL_AFTER_PREFIX + field.replace(':', '_');
                String labelAfter = i18n.translate(labelAfterPrefix, formattedDateLabel);
                suggestions.add(new SearchDocumentsSuggestion(suggesterId, labelAfter, iconURL).withSearchCriterion(
                        searchFieldAfter, date));

                String searchFieldBefore = field + "_max";
                String labelBeforePrefix = LABEL_BEFORE_PREFIX + field.replace(':', '_');
                String labelBefore = i18n.translate(labelBeforePrefix, formattedDateLabel);
                suggestions.add(new SearchDocumentsSuggestion(suggesterId, labelBefore, iconURL).withSearchCriterion(
                        searchFieldBefore, date));
            }
        }
        return suggestions;
    }

    @Override
    public void initWithParameters(SuggesterDescriptor descriptor) throws ComponentInitializationException {
        Map<String, String> params = descriptor.getParameters();
        iconURL = params.get("iconURL");
        String searchFields = params.get("searchFields");
        if (searchFields == null || iconURL == null) {
            throw new ComponentInitializationException(String.format("Could not initialize suggester '%s': "
                    + "searchFields and iconURL" + " are mandatory parameters", descriptor.getName()));
        }
        this.searchFields = searchFields.split(", *");
    }

}

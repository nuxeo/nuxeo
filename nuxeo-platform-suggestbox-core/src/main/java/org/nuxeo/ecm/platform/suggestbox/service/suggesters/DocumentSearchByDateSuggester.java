package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.suggestbox.service.CommonSuggestionTypes;
import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.suggestbox.utils.DateMatcher;

/**
 * Simple static suggester that parses the input and suggest to search document
 * by date if the input can be interpreted as a date in the user locale.
 */
public class DocumentSearchByDateSuggester implements Suggester {

    final static String type = CommonSuggestionTypes.SEARCH_DOCUMENTS;

    final static String LABEL_BEFORE_PREFIX = "label.search.beforeDate";

    final static String LABEL_AFTER_PREFIX = "label.search.afterDate";

    protected String[] searchFields;

    protected String label;

    protected String iconURL;

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException {
        List<Suggestion> suggestions = new ArrayList<Suggestion>();

        // TODO: use SimpleDateFormat and use the locale information from the
        // context
        DateMatcher matcher = DateMatcher.fromInput(userInput);
        if (matcher.hasMatch()) {
            for (String field : searchFields) {
                String valueAfter = field + "_min";
                String labelAfter = context.messages.get(LABEL_AFTER_PREFIX
                        + field.replace(':', '_'));
                suggestions.add(new Suggestion(type, valueAfter, labelAfter,
                        iconURL));

                String valueBefore = field + "_max";
                String labelBefore = context.messages.get(LABEL_BEFORE_PREFIX
                        + field.replace(':', '_'));
                suggestions.add(new Suggestion(type, valueBefore, labelBefore,
                        iconURL));
            }
        }
        return suggestions;
    }

    @Override
    public void initWithParameters(SuggesterDescriptor descriptor)
            throws ComponentInitializationException {
        Map<String, String> params = descriptor.getParameters();
        String searchFields = params.get("searchFields");
        iconURL = params.get("iconURL");

        if (searchFields == null || iconURL == null) {
            throw new ComponentInitializationException(
                    String.format("Could not initialize suggester '%s': "
                            + "searchFields and iconURL"
                            + " are mandatory parameters", descriptor.getName()));
        }
        this.searchFields = searchFields.split(", *");
    }

}

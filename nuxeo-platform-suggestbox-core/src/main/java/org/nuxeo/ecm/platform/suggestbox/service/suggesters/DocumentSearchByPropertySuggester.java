package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.suggestbox.service.CommonSuggestionTypes;
import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;

/**
 * Simple stateless document search suggester that propose to use the user input
 * for searching a specific field.
 */
public class DocumentSearchByPropertySuggester implements Suggester {

    protected String type = CommonSuggestionTypes.SEARCH_DOCUMENTS;

    protected String searchField = "fsd:ecm_fulltext";

    protected String label = "label.searchDocumentsByKeywords";

    protected String description = "";

    protected String iconURL = "/img/facetedSearch.png";

    protected boolean disabled;

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException {
        I18nHelper i18n = I18nHelper.instanceFor(context.messages);
        Suggestion suggestion = new Suggestion(type, searchField + ":"
                + userInput, i18n.translate(label, userInput), iconURL);
        if (disabled) {
            suggestion.disable();
        }
        if (description != null) {
            suggestion.withDescription(i18n.translate(description, userInput));
        }
        return Collections.singletonList(suggestion);
    }

    @Override
    public void initWithParameters(SuggesterDescriptor descriptor)
            throws ComponentInitializationException {
        Map<String, String> params = descriptor.getParameters();
        type = params.get("type");
        searchField = params.get("searchField");
        label = params.get("label");
        iconURL = params.get("iconURL");
        description = params.get("description");
        String disabled = params.get("disabled");
        if (disabled != null) {
            this.disabled = Boolean.valueOf(disabled);
        }
        if (type == null || searchField == null || label == null
                || iconURL == null) {
            throw new ComponentInitializationException(
                    String.format("Could not initialize suggester '%s': "
                            + "type, propertyPath, label and iconURL"
                            + " are mandatory parameters", descriptor.getName()));
        }
    }

}

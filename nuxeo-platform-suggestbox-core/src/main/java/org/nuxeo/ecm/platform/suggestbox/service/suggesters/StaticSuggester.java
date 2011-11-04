package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;


/**
 * Simple static suggester that always output the same suggestion based on the
 * descriptor parameters.
 */
public class StaticSuggester implements Suggester {

    protected String type;

    protected String value;

    protected String label;

    protected String description;

    protected String iconURL;

    protected boolean disabled;

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException {
        Suggestion suggestion = new Suggestion(type, value,
                context.messages.get(label), iconURL);
        if (disabled) {
            suggestion.disable();
        }
        if (description != null) {
            suggestion.withDescription(context.messages.get(description));
        }
        return Collections.singletonList(suggestion);
    }

    @Override
    public void initWithParameters(SuggesterDescriptor descriptor)
            throws ComponentInitializationException {
        Map<String, String> params = descriptor.getParameters();
        type = params.get("type");
        value = params.get("value");
        label = params.get("label");
        iconURL = params.get("iconURL");
        description = params.get("description");
        String disabledValue = params.get("disabled");
        if (disabledValue != null) {
            disabled = Boolean.valueOf(disabledValue);
        }

        if (type == null || value == null || label == null || iconURL == null) {
            throw new ComponentInitializationException(
                    String.format("Could not initialize suggester '%s': "
                            + "type, value, label and iconURL"
                            + " are mandatory parameters", descriptor.getName()));
        }
    }

}

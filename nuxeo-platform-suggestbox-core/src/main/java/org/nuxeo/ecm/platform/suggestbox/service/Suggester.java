package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.List;
import java.util.Map;

public interface Suggester {

    /**
     * Compute a list of possible user actions or intents given their input and
     * context.
     * 
     * @param userInput text typed by the user
     * @param context user context
     * @return generated suggestion for the given input and context
     */
    public List<Suggestion> suggest(String userInput, SuggestionContext context);

    /**
     * Configure the Suggester instance with the parameters from the XML
     * descriptor.
     * 
     * @param parameters a map of string valued configuration parameters.
     */
    public void setParameters(Map<String, String> parameters);
}

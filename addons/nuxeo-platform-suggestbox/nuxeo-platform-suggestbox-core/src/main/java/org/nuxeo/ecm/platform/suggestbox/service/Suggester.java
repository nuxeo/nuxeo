package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.List;

import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;

public interface Suggester {

    /**
     * Compute a list of possible user actions or intents given their input and
     * context.
     * 
     * @param userInput text typed by the user
     * @param context user context
     * @return generated suggestion for the given input and context
     * @throws SuggestionException if the configuration or context are
     *             inconsistent, or a backing service is failing.
     */
    List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException;

    /**
     * Configure the Suggester instance with the parameters from the XML
     * descriptor.
     * 
     * @param descriptor XMap descriptor with the aggregate configuration
     *            information of the component.
     */
    void initWithParameters(SuggesterDescriptor descriptor)
            throws ComponentInitializationException;
}

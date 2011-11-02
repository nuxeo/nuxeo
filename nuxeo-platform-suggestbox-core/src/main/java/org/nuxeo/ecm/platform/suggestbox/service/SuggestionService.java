package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.List;

/**
 * Pluggable service to generate user action suggestions based on text input and
 * contextual data.
 * 
 * This services aims to build more natural user interfaces for search and
 * navigation by trying to interpret and make explicit possible user intents.
 * 
 * Possible usages of this service:
 * 
 * <ul>
 * <li>make the top right JSF search box more useful with ajax auto-completion
 * that leads to typed suggestions</li>
 * <li>custom auto-complete field in layouts</li>
 * <li>smart mobile application using a Content Automation operation for
 * suggesting next operations / chains</li>
 * </ul>
 * 
 * @author ogrisel
 */
public interface SuggestionService {

    /**
     * Call the suggesters registered for the given suggestion point mentioned
     * in the context and aggregate the results.
     * 
     * @param userInput text typed by the user
     * @param context user context (with suggestPoint name and more)
     * @return generated suggestion for the given input and context
     */
    public List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException;

}

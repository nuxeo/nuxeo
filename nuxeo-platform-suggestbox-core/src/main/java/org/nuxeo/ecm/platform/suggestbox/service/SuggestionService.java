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
 * @since 5.5
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

    /**
     * Call a single suggester registered under the provided name.
     *
     * @param userInput text typed by the user
     * @param context user context (with suggestPoint name and more)
     * @param suggester the registration name of the suggester to use
     * @return generated suggestion for the given input and context
     */
    public List<Suggestion> suggest(String searchKeywords,
            SuggestionContext suggestionContext, String suggester)
            throws SuggestionException;

    /**
     * Call the Content Automation Operation chain matching the suggestion
     * selected by the user.
     *
     * @param suggestion the selected suggestion to execute
     * @param context the suggestion context that is also passed to as Content
     *            Automation context.
     * @return the outcome of the selected operation chain. In a JSF / Seam
     *         environment, the ouctome is passed as a String to the JSF
     *         runtime. In a JAX-RS environment the outcome could be passed as a
     *         resource path for redirect navigation.
     */
    Object handleSelection(Suggestion suggestion, SuggestionContext context)
            throws SuggestionHandlingException;

}

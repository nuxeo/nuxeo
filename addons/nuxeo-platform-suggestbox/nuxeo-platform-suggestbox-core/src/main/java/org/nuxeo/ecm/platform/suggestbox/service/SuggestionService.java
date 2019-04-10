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
package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.List;

/**
 * Pluggable service to generate user action suggestions based on text input and contextual data.
 * <p>
 * This services aims to build more natural user interfaces for search and navigation by trying to interpret and make
 * explicit possible user intents.
 * <p>
 * Possible usages of this service:
 * <ul>
 * <li>make the top right JSF search box more useful with ajax auto-completion that leads to typed suggestions</li>
 * <li>custom auto-complete field in layouts</li>
 * <li>smart mobile application using a Content Automation operation for suggesting next operations / chains</li>
 * </ul>
 *
 * @since 5.5
 * @author ogrisel
 */
public interface SuggestionService {

    /**
     * Call the suggesters registered for the given suggestion point mentioned in the context and aggregate the results.
     *
     * @param userInput text typed by the user
     * @param context user context (with suggestPoint name and more)
     * @return generated suggestion for the given input and context
     */
    List<Suggestion> suggest(String userInput, SuggestionContext context) throws SuggestionException;

    /**
     * Call a single suggester registered under the provided name.
     *
     * @param userInput text typed by the user
     * @param suggestionContext user context (with suggestPoint name and more)
     * @param suggester the registration name of the suggester to use
     * @return generated suggestion for the given input and context
     */
    List<Suggestion> suggest(String searchKeywords, SuggestionContext suggestionContext, String suggester)
            throws SuggestionException;

}

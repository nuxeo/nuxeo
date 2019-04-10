/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.List;

/**
 * Pluggable service to generate user action suggestions based on text input and
 * contextual data.
 * <p>
 * This services aims to build more natural user interfaces for search and
 * navigation by trying to interpret and make explicit possible user intents.
 * <p>
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
    List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException;

    /**
     * Call a single suggester registered under the provided name.
     *
     * @param userInput text typed by the user
     * @param suggestionContext user context (with suggestPoint name and more)
     * @param suggester the registration name of the suggester to use
     * @return generated suggestion for the given input and context
     */
    List<Suggestion> suggest(String searchKeywords,
            SuggestionContext suggestionContext, String suggester)
            throws SuggestionException;

}

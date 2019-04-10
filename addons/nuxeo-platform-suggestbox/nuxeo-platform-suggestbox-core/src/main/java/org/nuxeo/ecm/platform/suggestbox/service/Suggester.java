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

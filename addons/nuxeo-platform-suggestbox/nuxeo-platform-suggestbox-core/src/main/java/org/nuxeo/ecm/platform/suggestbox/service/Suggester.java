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

import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;

public interface Suggester {

    /**
     * Compute a list of possible user actions or intents given their input and context.
     *
     * @param userInput text typed by the user
     * @param context user context
     * @return generated suggestion for the given input and context
     * @throws SuggestionException if the configuration or context are inconsistent, or a backing service is failing.
     */
    List<Suggestion> suggest(String userInput, SuggestionContext context) throws SuggestionException;

    /**
     * Configure the Suggester instance with the parameters from the XML descriptor.
     *
     * @param descriptor XMap descriptor with the aggregate configuration information of the component.
     */
    void initWithParameters(SuggesterDescriptor descriptor) throws ComponentInitializationException;
}

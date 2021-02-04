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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupItemDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggesterGroupRegistry;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The Class SuggestionServiceImpl.
 */
public class SuggestionServiceImpl extends DefaultComponent implements SuggestionService {

    private static final Logger log = LogManager.getLogger(SuggestionServiceImpl.class);

    protected static final String SUGGESTERS_XP = "suggesters";

    protected static final String SUGGESTERGROUPS_XP = "suggesterGroups";

    protected Map<String, Suggester> suggesters;

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context) throws SuggestionException {
        List<Suggestion> suggestions = new ArrayList<>();
        var suggesterGroup = getSuggesterGroups().getSuggesterGroupDescriptor(context.suggesterGroup);
        if (suggesterGroup == null) {
            log.warn("No registered SuggesterGroup with id: {}", context.suggesterGroup);
            return suggestions;
        }

        for (SuggesterGroupItemDescriptor suggesterGroupItem : suggesterGroup.getSuggesters()) {
            String suggesterName = suggesterGroupItem.getName();
            Suggester suggester = suggesters.get(suggesterName);
            if (suggester == null) {
                log.warn("No suggester available with name '{}'", suggesterName);
                continue;
            }
            suggestions.addAll(suggester.suggest(userInput, context));
        }
        return suggestions;
    }

    @Override
    public List<Suggestion> suggest(String input, SuggestionContext context, String suggesterName)
            throws SuggestionException {
        Suggester suggester = suggesters.get(suggesterName);
        if (suggester == null) {
            throw new SuggestionException(String.format("No suggester available with name '%s'.", suggesterName));
        }
        return suggester.suggest(input, context);
    }

    // Nuxeo Runtime Component API

    @Override
    public void start(ComponentContext context) {
        suggesters = new HashMap<>();
        this.<SuggesterDescriptor> getRegistryContributions(SUGGESTERS_XP).forEach(desc -> {
            try {
                suggesters.put(desc.getName(), desc.getSuggester());
            } catch (ComponentInitializationException e) {
                addRuntimeMessage(Level.ERROR, e.getMessage());
                log.error(e, e);
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        suggesters = null;
    }

    /**
     * Gets the suggester groups registry. Only for test purpose.
     *
     * @return the suggester groups
     */
    public SuggesterGroupRegistry getSuggesterGroups() {
        return getExtensionPointRegistry(SUGGESTERGROUPS_XP);
    }

}

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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupItemDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggesterGroupRegistry;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggesterRegistry;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The Class SuggestionServiceImpl.
 */
public class SuggestionServiceImpl extends DefaultComponent implements
        SuggestionService {

    private static final Log log = LogFactory.getLog(SuggestionServiceImpl.class);

    protected SuggesterGroupRegistry suggesterGroups;

    protected SuggesterRegistry suggesters;

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException {
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        SuggesterGroupDescriptor suggesterGroup = suggesterGroups.getSuggesterGroupDescriptor(context.suggesterGroup);
        if (suggesterGroup == null) {
            log.warn("No registered SuggesterGroup with id: "
                    + context.suggesterGroup);
            return suggestions;
        }

        for (SuggesterGroupItemDescriptor suggesterGroupItem : suggesterGroup.getSuggesters()) {
            String suggesterId = suggesterGroupItem.getName();
            SuggesterDescriptor suggesterDescritor = suggesters.getSuggesterDescriptor(suggesterId);
            if (suggesterDescritor == null) {
                log.warn("No suggester registered with id: " + suggesterId);
                continue;
            }
            if (!suggesterDescritor.isEnabled()) {
                continue;
            }
            Suggester suggester = suggesterDescritor.getSuggester();
            if (suggester == null) {
                log.warn("Suggester with id '" + suggesterId
                        + "' has a configuration that prevents instanciation"
                        + " (no className in aggregate descriptor)");
                continue;
            }
            suggestions.addAll(suggester.suggest(userInput, context));
        }
        return suggestions;
    }

    @Override
    public List<Suggestion> suggest(String input, SuggestionContext context,
            String suggesterName) throws SuggestionException {
        SuggesterDescriptor suggesterDescriptor = suggesters.getSuggesterDescriptor(suggesterName);
        if (suggesterDescriptor == null) {
            throw new SuggestionException(String.format(
                    "No suggester registered under the name '%s'.",
                    suggesterName));
        }
        if (!suggesterDescriptor.isEnabled()) {
            throw new SuggestionException(String.format(
                    "Suggester registered under the name '%s' is disabled.",
                    suggesterName));
        }
        Suggester suggester = suggesterDescriptor.getSuggester();
        if (suggester == null) {
            String message = "Suggester with id '" + suggesterName
                    + "' has a configuration that prevents instanciation"
                    + " (no className in aggregate descriptor)";
            throw new SuggestionException(message);
        }
        return suggester.suggest(input, context);
    }

    // Nuxeo Runtime Component API

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        suggesters = new SuggesterRegistry();
        suggesterGroups = new SuggesterGroupRegistry();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof SuggesterDescriptor) {
            SuggesterDescriptor suggesterDescriptor = (SuggesterDescriptor) contribution;
            log.info(String.format("Registering suggester '%s'",
                    suggesterDescriptor.getName()));
            suggesterDescriptor.setRuntimeContext(contributor.getRuntimeContext());
            suggesters.addContribution(suggesterDescriptor);
        } else if (contribution instanceof SuggesterGroupDescriptor) {
            SuggesterGroupDescriptor suggesterGroupDescriptor = (SuggesterGroupDescriptor) contribution;
            log.info(String.format("Registering suggester group '%s'",
                    suggesterGroupDescriptor.getName()));
            suggesterGroups.addContribution(suggesterGroupDescriptor);
        } else {
            log.error(String.format(
                    "Unknown contribution to the SuggestionService "
                            + "styling service, extension point '%s': '%s",
                    extensionPoint, contribution));
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof SuggesterDescriptor) {
            SuggesterDescriptor suggesterDescriptor = (SuggesterDescriptor) contribution;
            log.info(String.format("Unregistering suggester '%s'",
                    suggesterDescriptor.getName()));
            suggesters.removeContribution(suggesterDescriptor);
        } else if (contribution instanceof SuggesterGroupDescriptor) {
            SuggesterGroupDescriptor suggesterGroupDescriptor = (SuggesterGroupDescriptor) contribution;
            log.info(String.format("Unregistering suggester group '%s'",
                    suggesterGroupDescriptor.getName()));
            suggesterGroups.removeContribution(suggesterGroupDescriptor);
        } else {
            log.error(String.format(
                    "Unknown contribution to the SuggestionService "
                            + "styling service, extension point '%s': '%s",
                    extensionPoint, contribution));
        }
    }

    /**
     * Gets the suggester groups registry. Only for test purpose.
     *
     * @return the suggester groups
     */
    public SuggesterGroupRegistry getSuggesterGroups() {
        return suggesterGroups;
    }

}

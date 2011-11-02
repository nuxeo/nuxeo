package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggesterRegistry;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggesterGroupRegistry;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class SuggestionServiceImpl extends DefaultComponent implements
        SuggestionService {

    private static final Log log = LogFactory.getLog(SuggestionServiceImpl.class);

    protected SuggesterGroupRegistry suggesterGroups;

    protected SuggesterRegistry suggesters;

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException {
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        SuggesterGroupDescriptor suggestionPoint = suggesterGroups.getContribution(context.suggestionPoint);
        if (suggestionPoint == null) {
            log.warn("No registered SuggestionPoint with id: "
                    + context.suggestionPoint);
            return suggestions;
        }

        for (String suggesterId : suggestionPoint.getSuggesters()) {
            SuggesterDescriptor suggesterDescritor = suggesters.getContribution(suggesterId);
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
            SuggesterGroupDescriptor suggestionPointDescriptor = (SuggesterGroupDescriptor) contribution;
            log.info(String.format("Registering suggester group '%s'",
                    suggestionPointDescriptor.getName()));
            suggesterGroups.addContribution(suggestionPointDescriptor);
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
}

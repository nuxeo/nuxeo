package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggestionPointDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggesterRegistry;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggestionPointRegistry;
import org.nuxeo.runtime.model.DefaultComponent;

public class SuggestionServiceImpl extends DefaultComponent implements
        SuggestionService {

    private static final Log log = LogFactory.getLog(SuggestionServiceImpl.class);

    protected SuggestionPointRegistry suggestionPoints = new SuggestionPointRegistry();

    protected SuggesterRegistry suggesters = new SuggesterRegistry();

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context) {
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        SuggestionPointDescriptor suggestionPoint = suggestionPoints.getContribution(context.suggestionPoint);
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

}

package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupItemDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggestionHandlerDescriptor;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggesterGroupRegistry;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggesterRegistry;
import org.nuxeo.ecm.platform.suggestbox.service.registries.SuggestionHandlerRegistry;
import org.nuxeo.runtime.api.Framework;
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

    protected SuggestionHandlerRegistry suggestionHandlers;

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

    @Override
    public Object handleSelection(Suggestion suggestion,
            SuggestionContext suggestionContext)
            throws SuggestionHandlingException {
        AutomationService automation = Framework.getLocalService(AutomationService.class);

        for (SuggestionHandlerDescriptor handler : suggestionHandlers.getHandlers()) {
            if (handler.isEnabled()
                    && suggestion.getType().equals(handler.getType())
                    && suggestionContext.suggesterGroup.equals(handler.getSuggesterGroup())) {
                OperationContext operationContext = new OperationContext(
                        suggestionContext.session);
                operationContext.putAll(suggestionContext);
                operationContext.setInput(suggestion);

                String chainName = handler.getOperationChain();
                String operationName = handler.getOperation();
                if (chainName != null && !chainName.isEmpty()) {
                    try {
                        return automation.run(operationContext, chainName);
                    } catch (Throwable t) {
                        throw new SuggestionHandlingException(String.format(
                                "Error executing chain '%s' on %s", chainName,
                                suggestion.toString()), t);
                    }
                } else if (operationName != null && !operationName.isEmpty()) {
                    try {
                        Map<String, Object> emptyContext = Collections.emptyMap();
                        return automation.run(operationContext, operationName,
                                emptyContext);
                    } catch (Throwable t) {
                        throw new SuggestionHandlingException(String.format(
                                "Error executing operation '%s' on %s",
                                operationName, suggestion.toString()), t);
                    }
                } else {
                    throw new SuggestionHandlingException(String.format(
                            "SuggestionHandlerDescriptor %s should have either"
                                    + " operation or operationChain defined",
                            handler.getName()));
                }
            }
        }
        throw new SuggestionHandlingException(String.format(
                "No suggestion handler registered for type %s and group %s",
                suggestion.getType(), suggestionContext.suggesterGroup));
    }

    // Nuxeo Runtime Component API

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        suggesters = new SuggesterRegistry();
        suggesterGroups = new SuggesterGroupRegistry();
        suggestionHandlers = new SuggestionHandlerRegistry();
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
        } else if (contribution instanceof SuggestionHandlerDescriptor) {
            SuggestionHandlerDescriptor suggestionHandler = (SuggestionHandlerDescriptor) contribution;
            log.info(String.format("Registering suggestion handler '%s'",
                    suggestionHandler.getName()));
            suggestionHandlers.addContribution(suggestionHandler);
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
        } else if (contribution instanceof SuggestionHandlerDescriptor) {
            SuggestionHandlerDescriptor suggestionHandler = (SuggestionHandlerDescriptor) contribution;
            log.info(String.format("Unregistering suggestion handler '%s'",
                    suggestionHandler.getName()));
            suggestionHandlers.removeContribution(suggestionHandler);
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

    /**
     * Gets the suggestion handlers registry. Only for test purpose.
     *
     * @return the suggestion handlers
     */
    public SuggestionHandlerRegistry getSuggestionHandlers() {
        return suggestionHandlers;
    }
}

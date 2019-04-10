package org.nuxeo.ecm.platform.suggestbox.service.registries;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggestionHandlerDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class SuggestionHandlerRegistry extends
        ContributionFragmentRegistry<SuggestionHandlerDescriptor> {

    protected Map<String, SuggestionHandlerDescriptor> suggestionHandlerDescriptors = new LinkedHashMap<String, SuggestionHandlerDescriptor>();

    public Collection<SuggestionHandlerDescriptor> getHandlers() {
        return suggestionHandlerDescriptors.values();
    }

    @Override
    public void contributionRemoved(String id,
            SuggestionHandlerDescriptor descriptor) {
        suggestionHandlerDescriptors.remove(id);
    }

    @Override
    public String getContributionId(SuggestionHandlerDescriptor descriptor) {
        return descriptor.getName();
    }

    @Override
    public void contributionUpdated(String id,
            SuggestionHandlerDescriptor contrib,
            SuggestionHandlerDescriptor newOrigContrib) {
        suggestionHandlerDescriptors.put(id, contrib);
    }

    @Override
    public SuggestionHandlerDescriptor clone(
            SuggestionHandlerDescriptor suggester) {
        try {
            return (SuggestionHandlerDescriptor) suggester.clone();
        } catch (CloneNotSupportedException e) {
            // this should never occur since clone implements Cloneable
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(SuggestionHandlerDescriptor src,
            SuggestionHandlerDescriptor dst) {
    }
}

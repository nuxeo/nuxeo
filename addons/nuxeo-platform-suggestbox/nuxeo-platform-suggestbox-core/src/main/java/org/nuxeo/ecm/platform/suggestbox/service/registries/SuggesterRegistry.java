package org.nuxeo.ecm.platform.suggestbox.service.registries;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class SuggesterRegistry extends
        ContributionFragmentRegistry<SuggesterDescriptor> {

    protected Map<String, SuggesterDescriptor> suggesterDescriptors = new HashMap<String, SuggesterDescriptor>();

    public SuggesterDescriptor getSuggesterDescriptor(String name) {
        return suggesterDescriptors.get(name);
    }
    @Override
    public void contributionRemoved(String id, SuggesterDescriptor descriptor) {
        suggesterDescriptors.remove(id);
    }

    @Override
    public String getContributionId(SuggesterDescriptor descriptor) {
        return descriptor.getName();
    }

    @Override
    public void contributionUpdated(String id, SuggesterDescriptor contrib,
            SuggesterDescriptor newOrigContrib) {
        suggesterDescriptors.put(id, contrib);
    }

    @Override
    public SuggesterDescriptor clone(SuggesterDescriptor suggester) {
        try {
            return (SuggesterDescriptor) suggester.clone();
        } catch (CloneNotSupportedException e) {
            // this should never occur since clone implements Cloneable
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(SuggesterDescriptor src, SuggesterDescriptor dst) {
        try {
            dst.mergeFrom(src);
        } catch (Exception e) {
            // it should be possible to throw a non runtime exception here
            throw new RuntimeException(e);
        }
    }

}

package org.nuxeo.ecm.platform.suggestbox.service.registries;

import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggestionPointDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class SuggestionPointRegistry extends
        ContributionFragmentRegistry<SuggestionPointDescriptor> {

    @Override
    public SuggestionPointDescriptor clone(SuggestionPointDescriptor arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void contributionRemoved(String arg0, SuggestionPointDescriptor arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void contributionUpdated(String arg0,
            SuggestionPointDescriptor arg1, SuggestionPointDescriptor arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getContributionId(SuggestionPointDescriptor arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void merge(SuggestionPointDescriptor arg0,
            SuggestionPointDescriptor arg1) {
        // TODO Auto-generated method stub

    }

}

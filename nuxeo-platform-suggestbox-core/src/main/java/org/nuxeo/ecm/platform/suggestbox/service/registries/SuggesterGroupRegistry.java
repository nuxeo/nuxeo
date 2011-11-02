package org.nuxeo.ecm.platform.suggestbox.service.registries;

import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class SuggesterGroupRegistry extends
        ContributionFragmentRegistry<SuggesterGroupDescriptor> {

    @Override
    public SuggesterGroupDescriptor clone(SuggesterGroupDescriptor arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void contributionRemoved(String arg0, SuggesterGroupDescriptor arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void contributionUpdated(String arg0,
            SuggesterGroupDescriptor arg1, SuggesterGroupDescriptor arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getContributionId(SuggesterGroupDescriptor arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void merge(SuggesterGroupDescriptor arg0,
            SuggesterGroupDescriptor arg1) {
        // TODO Auto-generated method stub

    }

}

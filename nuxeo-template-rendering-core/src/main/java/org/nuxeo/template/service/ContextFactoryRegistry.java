package org.nuxeo.template.service;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.template.api.descriptor.ContextExtensionFactoryDescriptor;

public class ContextFactoryRegistry extends ContributionFragmentRegistry<ContextExtensionFactoryDescriptor> {

    protected Map<String, ContextExtensionFactoryDescriptor> factories = new HashMap<String, ContextExtensionFactoryDescriptor>();

    @Override
    public ContextExtensionFactoryDescriptor clone(ContextExtensionFactoryDescriptor desc) {
        return desc.clone();
    }

    @Override
    public void contributionRemoved(String id, ContextExtensionFactoryDescriptor desc) {
        factories.remove(id);
    }

    @Override
    public void contributionUpdated(String id, ContextExtensionFactoryDescriptor mergedDesc,
            ContextExtensionFactoryDescriptor orgDesc) {

        if (mergedDesc == null || !mergedDesc.isEnabled()) {
            factories.remove(id);
        } else {
            factories.put(id, mergedDesc);
        }
    }

    @Override
    public String getContributionId(ContextExtensionFactoryDescriptor desc) {
        return desc.getName();
    }

    @Override
    public void merge(ContextExtensionFactoryDescriptor src, ContextExtensionFactoryDescriptor dest) {
        dest.merge(src);
    }

    public Map<String, ContextExtensionFactoryDescriptor> getExtensionFactories() {
        return factories;
    }
}

package org.nuxeo.template.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.template.api.descriptor.TemplateProcessorDescriptor;

public class TemplateProcessorRegistry extends
        ContributionFragmentRegistry<TemplateProcessorDescriptor> {

    protected Map<String, TemplateProcessorDescriptor> processors = new HashMap<String, TemplateProcessorDescriptor>();

    @Override
    public TemplateProcessorDescriptor clone(TemplateProcessorDescriptor tpd) {
        return tpd.clone();
    }

    public TemplateProcessorDescriptor getProcessorByName(String name) {
        return processors.get(name);
    }

    public Collection<TemplateProcessorDescriptor> getRegistredProcessors() {
        return processors.values();
    }

    @Override
    public void contributionRemoved(String id, TemplateProcessorDescriptor tpd) {
        processors.remove(id);
    }

    @Override
    public void contributionUpdated(String id, TemplateProcessorDescriptor tpd,
            TemplateProcessorDescriptor newTpd) {
        if (tpd == null || !tpd.isEnabled()) {
            processors.remove(id);
        } else {
            if (tpd.init()) {
                processors.put(id, tpd);
            } else {
                throw new ClientRuntimeException("Unable to register processor");
            }
        }
    }

    @Override
    public String getContributionId(TemplateProcessorDescriptor tpd) {
        return tpd.getName();
    }

    @Override
    public void merge(TemplateProcessorDescriptor srcTpd,
            TemplateProcessorDescriptor descTpd) {
        descTpd.merge(srcTpd);
    }

}

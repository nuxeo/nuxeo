package org.nuxeo.template.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

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
        if (tpd == null || !tpd.enabled) {
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

        descTpd.defaultProcessor = srcTpd.defaultProcessor;
        if (srcTpd.className != null) {
            descTpd.className = srcTpd.className;
        }
        if (srcTpd.label != null) {
            descTpd.label = srcTpd.label;
        }
        if (srcTpd.supportedExtensions != null
                && srcTpd.supportedExtensions.size() > 0) {
            descTpd.supportedExtensions = srcTpd.supportedExtensions;
        }
        if (srcTpd.supportedMimeTypes != null
                && srcTpd.supportedMimeTypes.size() > 0) {
            descTpd.supportedMimeTypes = srcTpd.supportedMimeTypes;
        }
    }

}

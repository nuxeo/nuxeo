/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.template.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.template.api.descriptor.TemplateProcessorDescriptor;

public class TemplateProcessorRegistry extends ContributionFragmentRegistry<TemplateProcessorDescriptor> {

    protected Map<String, TemplateProcessorDescriptor> processors = new HashMap<>();

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
    public void contributionUpdated(String id, TemplateProcessorDescriptor tpd, TemplateProcessorDescriptor newTpd) {
        if (tpd == null || !tpd.isEnabled()) {
            processors.remove(id);
        } else {
            if (tpd.init()) {
                processors.put(id, tpd);
            } else {
                throw new NuxeoException("Unable to register processor");
            }
        }
    }

    @Override
    public String getContributionId(TemplateProcessorDescriptor tpd) {
        return tpd.getName();
    }

    @Override
    public void merge(TemplateProcessorDescriptor srcTpd, TemplateProcessorDescriptor descTpd) {
        descTpd.merge(srcTpd);
    }

}

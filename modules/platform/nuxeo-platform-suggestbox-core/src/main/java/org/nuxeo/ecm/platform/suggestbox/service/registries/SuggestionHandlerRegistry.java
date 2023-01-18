/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.registries;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggestionHandlerDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class SuggestionHandlerRegistry extends ContributionFragmentRegistry<SuggestionHandlerDescriptor> {

    private static final Logger log = LogManager.getLogger(SuggestionHandlerRegistry.class);

    protected final Map<String, SuggestionHandlerDescriptor> suggestionHandlerDescriptors = new LinkedHashMap<>();

    public Collection<SuggestionHandlerDescriptor> getHandlers() {
        return suggestionHandlerDescriptors.values();
    }

    public SuggestionHandlerDescriptor getSuggestionHandlerDescriptor(String name) {
        return suggestionHandlerDescriptors.get(name);
    }

    @Override
    public void contributionRemoved(String id, SuggestionHandlerDescriptor descriptor) {
        log.trace("Removing contribution with id: {} from suggestion handler descriptors", id);
        suggestionHandlerDescriptors.remove(id);
    }

    @Override
    public String getContributionId(SuggestionHandlerDescriptor descriptor) {
        return descriptor.getName();
    }

    @Override
    public void contributionUpdated(String id, SuggestionHandlerDescriptor contrib,
            SuggestionHandlerDescriptor newOrigContrib) {
        if (contrib.isEnabled()) {
            log.trace("Putting contribution: {} with id: {} in suggestion handler descriptors", contrib, id);
            suggestionHandlerDescriptors.put(id, contrib);
        } else {
            log.trace("Removing disabled contribution with id: {} from suggestion handler descriptors", id);
            suggestionHandlerDescriptors.remove(id);
        }
    }

    @Override
    public SuggestionHandlerDescriptor clone(SuggestionHandlerDescriptor suggester) {
        try {
            return (SuggestionHandlerDescriptor) suggester.clone();
        } catch (CloneNotSupportedException e) {
            // this should never occur since clone implements Cloneable
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(SuggestionHandlerDescriptor src, SuggestionHandlerDescriptor dst) {
        log.trace("Merging contribution with id: {} to contribution with id: {}", src::getName, dst::getName);
        // Enabled
        if (src.isEnabled() != dst.isEnabled()) {
            dst.setEnabled(src.isEnabled());
        }
        // Type
        if (!StringUtils.isEmpty(src.getType()) && !src.getType().equals(dst.getType())) {
            dst.setType(src.getType());
        }
        // Suggester group
        if (!StringUtils.isEmpty(src.getSuggesterGroup()) && !src.getSuggesterGroup().equals(dst.getSuggesterGroup())) {
            dst.setSuggesterGroup(src.getSuggesterGroup());
        }
        // Operation
        if (!StringUtils.isEmpty(src.getOperation()) && !src.getOperation().equals(dst.getOperation())) {
            dst.setOperation(src.getOperation());
        }
        // Operation chain
        if (!StringUtils.isEmpty(src.getOperationChain()) && !src.getOperationChain().equals(dst.getOperationChain())) {
            dst.setOperationChain(src.getOperationChain());
        }
    }
}

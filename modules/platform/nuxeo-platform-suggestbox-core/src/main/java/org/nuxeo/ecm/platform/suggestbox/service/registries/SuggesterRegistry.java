/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class SuggesterRegistry extends ContributionFragmentRegistry<SuggesterDescriptor> {

    protected final Map<String, SuggesterDescriptor> suggesterDescriptors = new HashMap<>();

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
    public void contributionUpdated(String id, SuggesterDescriptor contrib, SuggesterDescriptor newOrigContrib) {
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
        } catch (ComponentInitializationException e) {
            throw new NuxeoException(e);
        }
    }

}

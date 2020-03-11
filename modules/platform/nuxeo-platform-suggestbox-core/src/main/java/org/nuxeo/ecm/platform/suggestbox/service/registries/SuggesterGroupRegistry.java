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
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterGroupDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class SuggesterGroupRegistry extends ContributionFragmentRegistry<SuggesterGroupDescriptor> {

    protected final Map<String, SuggesterGroupDescriptor> suggesterGroupDescriptors = new HashMap<>();

    public SuggesterGroupDescriptor getSuggesterGroupDescriptor(String name) {
        return suggesterGroupDescriptors.get(name);
    }

    @Override
    public SuggesterGroupDescriptor clone(SuggesterGroupDescriptor suggestGroup) {
        try {
            return (SuggesterGroupDescriptor) suggestGroup.clone();
        } catch (CloneNotSupportedException e) {
            // this should never occur since clone implements Cloneable
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contributionRemoved(String id, SuggesterGroupDescriptor arg1) {
        suggesterGroupDescriptors.remove(id);
    }

    @Override
    public void contributionUpdated(String id, SuggesterGroupDescriptor contrib, SuggesterGroupDescriptor newOrigContrib) {
        suggesterGroupDescriptors.put(id, contrib);
    }

    @Override
    public String getContributionId(SuggesterGroupDescriptor suggestGroup) {
        return suggestGroup.getName();
    }

    @Override
    public void merge(SuggesterGroupDescriptor src, SuggesterGroupDescriptor dst) {
        try {
            dst.mergeFrom(src);
        } catch (ComponentInitializationException e) {
            throw new NuxeoException(e);
        }
    }

}

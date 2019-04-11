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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.template.api.descriptor.ContextExtensionFactoryDescriptor;

public class ContextFactoryRegistry extends ContributionFragmentRegistry<ContextExtensionFactoryDescriptor> {

    protected Map<String, ContextExtensionFactoryDescriptor> factories = new HashMap<>();

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

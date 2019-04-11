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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@link DisabledPropertyRefDescriptor} instances
 * <p>
 * Only supports merging of contributions on "enabled" information.
 * <p>
 * Stores contribution in the registration order, in case some conflicting criteria are found, the first one matching
 * will apply.
 *
 * @since 5.6
 */
public class DisabledPropertyRefRegistry extends ContributionFragmentRegistry<DisabledPropertyRefDescriptor> {

    protected final Map<String, DisabledPropertyRefDescriptor> refs;

    public DisabledPropertyRefRegistry() {
        super();
        // use linked has map since there is no ordering support, not make sure
        this.refs = new LinkedHashMap<>();
    }

    @Override
    public String getContributionId(DisabledPropertyRefDescriptor contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, DisabledPropertyRefDescriptor contrib,
            DisabledPropertyRefDescriptor newOrigContrib) {
        refs.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, DisabledPropertyRefDescriptor origContrib) {
        refs.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return true;
    }

    @Override
    public DisabledPropertyRefDescriptor clone(DisabledPropertyRefDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(DisabledPropertyRefDescriptor source, DisabledPropertyRefDescriptor dest) {
        if (source.getEnabled() != dest.getEnabled()) {
            dest.setEnabled(source.getEnabled());
        }
    }

    public Collection<DisabledPropertyRefDescriptor> getDisabledPropertyRefs() {
        return Collections.unmodifiableCollection(refs.values());
    }

}

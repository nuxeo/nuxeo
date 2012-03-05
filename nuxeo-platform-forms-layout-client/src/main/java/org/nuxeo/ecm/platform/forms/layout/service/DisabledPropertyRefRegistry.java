/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Stores contribution in the registration order, in case some conflicting
 * criteria are found, the first one matching will apply.
 *
 * @since 5.6
 */
public class DisabledPropertyRefRegistry extends
        ContributionFragmentRegistry<DisabledPropertyRefDescriptor> {

    protected final Map<String, DisabledPropertyRefDescriptor> refs;

    public DisabledPropertyRefRegistry() {
        super();
        // use linked has map since there is no ordering support, not make sure
        this.refs = new LinkedHashMap<String, DisabledPropertyRefDescriptor>();
    }

    @Override
    public String getContributionId(DisabledPropertyRefDescriptor contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id,
            DisabledPropertyRefDescriptor contrib,
            DisabledPropertyRefDescriptor newOrigContrib) {
        refs.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id,
            DisabledPropertyRefDescriptor origContrib) {
        refs.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return true;
    }

    @Override
    public DisabledPropertyRefDescriptor clone(
            DisabledPropertyRefDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(DisabledPropertyRefDescriptor source,
            DisabledPropertyRefDescriptor dest) {
        if (source.getEnabled() != dest.getEnabled()) {
            dest.setEnabled(source.getEnabled());
        }
    }

    public Collection<DisabledPropertyRefDescriptor> getDisabledPropertyRefs() {
        return Collections.unmodifiableCollection(refs.values());
    }

}

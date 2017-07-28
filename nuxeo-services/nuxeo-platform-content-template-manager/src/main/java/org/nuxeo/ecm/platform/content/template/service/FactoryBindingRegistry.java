/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.content.template.service;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 9.3
 */
public class FactoryBindingRegistry extends ContributionFragmentRegistry<FactoryBindingDescriptor> {

    @Override
    public String getContributionId(FactoryBindingDescriptor contrib) {
        if (contrib.getTargetType() == null) {
            return contrib.getTargetFacet();
        }
        return contrib.getTargetType();
    }

    @Override
    public void contributionUpdated(String id, FactoryBindingDescriptor contrib,
            FactoryBindingDescriptor newOrigContrib) {
        // nothing to do, we rely on map in ContributionFragmentRegistry
    }

    @Override
    public void contributionRemoved(String id, FactoryBindingDescriptor origContrib) {
        // nothing to do, we rely on map in ContributionFragmentRegistry
    }

    @Override
    public FactoryBindingDescriptor clone(FactoryBindingDescriptor orig) {
        return new FactoryBindingDescriptor(orig);
    }

    @Override
    public void merge(FactoryBindingDescriptor src, FactoryBindingDescriptor dst) {
        dst.merge(src);
    }

    @Override
    public FactoryBindingDescriptor getContribution(String docTypeOrFacet) {
        return super.getContribution(docTypeOrFacet);
    }

}

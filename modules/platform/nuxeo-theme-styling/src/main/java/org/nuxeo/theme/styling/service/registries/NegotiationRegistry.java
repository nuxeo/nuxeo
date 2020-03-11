/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.theme.styling.service.registries;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.theme.styling.service.descriptors.NegotiationDescriptor;

/**
 * Registry for negotiations.
 *
 * @since 7.4
 */
public class NegotiationRegistry extends ContributionFragmentRegistry<NegotiationDescriptor> {

    protected Map<String, NegotiationDescriptor> negotations = new HashMap<>();

    @Override
    public String getContributionId(NegotiationDescriptor contrib) {
        return contrib.getTarget();
    }

    @Override
    public void contributionUpdated(String id, NegotiationDescriptor contrib, NegotiationDescriptor newOrigContrib) {
        negotations.put(id, contrib);
    }

    @Override
    public synchronized void removeContribution(NegotiationDescriptor contrib) {
        removeContribution(contrib, true);
    }

    @Override
    public void contributionRemoved(String id, NegotiationDescriptor origContrib) {
        negotations.remove(id);
    }

    @Override
    public NegotiationDescriptor clone(NegotiationDescriptor orig) {
        if (orig == null) {
            return null;
        }
        return orig.clone();
    }

    @Override
    public void merge(NegotiationDescriptor src, NegotiationDescriptor dst) {
        dst.merge(src);
    }

    public NegotiationDescriptor getNegotiation(String id) {
        return negotations.get(id);
    }

}

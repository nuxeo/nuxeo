/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    protected Map<String, NegotiationDescriptor> negotations = new HashMap<String, NegotiationDescriptor>();

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

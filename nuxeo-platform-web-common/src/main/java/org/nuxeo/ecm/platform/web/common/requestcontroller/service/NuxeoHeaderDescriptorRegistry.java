/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Martins
 */

package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Header description registry model.
 *
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 * @since6.0
 */
public class NuxeoHeaderDescriptorRegistry extends
        ContributionFragmentRegistry<NuxeoHeaderDescriptor> {

    protected Map<String, NuxeoHeaderDescriptor> descs = new HashMap<>();

    @Override
    public String getContributionId(NuxeoHeaderDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id, NuxeoHeaderDescriptor contrib,
            NuxeoHeaderDescriptor newOrigContrib) {
        if (descs.containsKey(id)) {
            descs.remove(id);
        }
        if (contrib.enabled) {
            descs.put(id, contrib);
        }
    }

    @Override
    public void contributionRemoved(String id, NuxeoHeaderDescriptor origContrib) {
        if (descs.containsKey(id)) {
            descs.remove(id);
        }
    }

    @Override
    public NuxeoHeaderDescriptor clone(NuxeoHeaderDescriptor orig) {
        try {
            return orig.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happens...
            return null;
        }
    }

    @Override
    public void merge(NuxeoHeaderDescriptor src, NuxeoHeaderDescriptor dst) {
        dst.merge(src);
    }

}

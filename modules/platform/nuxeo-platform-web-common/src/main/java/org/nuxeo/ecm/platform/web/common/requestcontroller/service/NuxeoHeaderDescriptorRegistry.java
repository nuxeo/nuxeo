/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 * @since 6.0
 */
public class NuxeoHeaderDescriptorRegistry extends ContributionFragmentRegistry<NuxeoHeaderDescriptor> {

    protected Map<String, NuxeoHeaderDescriptor> descs = new HashMap<>();

    @Override
    public String getContributionId(NuxeoHeaderDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id, NuxeoHeaderDescriptor contrib, NuxeoHeaderDescriptor newOrigContrib) {
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

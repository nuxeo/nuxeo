/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.service;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ContributionFragmentRegistry} to register {@link RenderView}.
 *
 * @since 8.4
 */
public class RenderViewContributionHandler extends ContributionFragmentRegistry<RenderView> {

    public final Map<String, RenderView> registry;

    public RenderViewContributionHandler() {
        registry = new HashMap<>();
    }

    @Override
    public String getContributionId(RenderView contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, RenderView contrib, RenderView newOrigContrib) {
        if (contrib.isEnabled()) {
            registry.put(id, contrib);
        } else {
            registry.remove(id);
        }
    }

    @Override
    public void contributionRemoved(String id, RenderView origContrib) {
        registry.remove(id);
    }

    @Override
    public RenderView clone(RenderView orig) {
        return new RenderView(orig);
    }

    @Override
    public void merge(RenderView src, RenderView dst) {
        dst.merge(src);
    }
}

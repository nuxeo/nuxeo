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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.registries;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.routing.api.RouteModelResourceType;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for route templates
 *
 * @since 5.6
 */
public class RouteTemplateResourceRegistry extends SimpleContributionRegistry<RouteModelResourceType> {

    public List<URL> getRouteModelTemplateResources() {
        List<URL> urls = new ArrayList<>();
        for (RouteModelResourceType res : currentContribs.values()) {
            urls.add(res.getUrl());
        }
        return urls;
    }

    @Override
    public String getContributionId(RouteModelResourceType contrib) {
        return contrib.getId();
    }

    public RouteModelResourceType getResource(String id) {
        return getCurrentContribution(id);
    }

}

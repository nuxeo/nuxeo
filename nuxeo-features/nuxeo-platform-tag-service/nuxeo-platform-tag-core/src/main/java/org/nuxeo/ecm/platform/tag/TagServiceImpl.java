/*
 * (C) Copyright 2009-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Radu Darlea
 *     Catalin Baican
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * the tag component.
 */
public class TagServiceImpl extends DefaultComponent {

    public static final String FACETED_TAG_SERVICE_ENABLED = "nuxeo.faceted.tag.service.enabled";

    protected TagService tagService;

    @Override
    public void start(ComponentContext context) {
        if (Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(FACETED_TAG_SERVICE_ENABLED)) {
            tagService = new FacetedTagService();
        } else {
            tagService = new RelationTagService();
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        // should deploy before repository service because the tag service is indirectly used (through a listener) by
        // the repository init handlers
        Component component = (Component) Framework.getRuntime()
                                                   .getComponentInstance(
                                                           "org.nuxeo.ecm.core.repository.RepositoryServiceComponent")
                                                   .getInstance();
        return component.getApplicationStartedOrder() - 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        return (T) tagService;
    }

}

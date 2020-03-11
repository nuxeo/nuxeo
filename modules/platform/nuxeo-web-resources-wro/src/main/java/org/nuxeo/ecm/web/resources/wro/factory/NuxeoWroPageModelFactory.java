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
package org.nuxeo.ecm.web.resources.wro.factory;

import java.util.List;

import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceContextImpl;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.PageDescriptor;

import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.group.Group;

/**
 * Generates a {@link WroModel} using contributions to the {@link ThemeStylingService} service.
 * <p>
 * Maps {@link PageDescriptor} elements to wro groups, and map their ordered resources (with resolved dependencies)
 * to wro resources.
 *
 * @since 7.10
 */
public class NuxeoWroPageModelFactory extends NuxeoWroModelFactory implements WroModelFactory {

    @Override
    public WroModel create() {
        WroModel model = new WroModel();
        ThemeStylingService ts = Framework.getService(ThemeStylingService.class);
        WebResourceManager ws = Framework.getService(WebResourceManager.class);
        ResourceContextImpl rcontext = new ResourceContextImpl();

        List<PageDescriptor> pages = ts.getPages();
        for (PageDescriptor page : pages) {
            String groupName = page.getName();
            Group group = new Group(groupName);
            List<String> bundleNames = page.getResourceBundles();
            for (String bundleName : bundleNames) {
                List<Resource> resources = ws.getResources(rcontext, bundleName, ResourceType.any.name());
                if (resources != null) {
                    for (Resource resource : resources) {
                        ro.isdc.wro.model.resource.Resource wr = toWroResource(bundleName, resource);
                        if (wr != null) {
                            group.addResource(wr);
                        }
                    }
                }
            }
            model.addGroup(group);
        }

        return model;
    }

    @Override
    public void destroy() {
    }

}

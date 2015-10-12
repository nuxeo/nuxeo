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
 * Maps {@link PageResourceBundle} elements to wro groups, and map their ordered resources (with resolved dependencies)
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

    public void destroy() {
    }

}

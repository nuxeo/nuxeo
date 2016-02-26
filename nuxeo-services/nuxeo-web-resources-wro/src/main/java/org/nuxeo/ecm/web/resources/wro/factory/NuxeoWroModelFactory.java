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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.ResourceContextImpl;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.wro.provider.NuxeoUriLocator;
import org.nuxeo.runtime.api.Framework;

import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.group.Group;

/**
 * Generates a {@link WroModel} using contributions to the {@link WebResourceManager} service.
 * <p>
 * Maps {@link ResourceBundle} elements to wro groups, and map their ordered resources (with resolved dependencies) to
 * wro resources.
 *
 * @since 7.3
 */
public class NuxeoWroModelFactory implements WroModelFactory {

    private static final Log log = LogFactory.getLog(NuxeoWroModelFactory.class);

    @Override
    public WroModel create() {
        WroModel model = new WroModel();
        WebResourceManager service = Framework.getService(WebResourceManager.class);
        ResourceContextImpl rcontext = new ResourceContextImpl();
        List<ResourceBundle> bundles = service.getResourceBundles();
        for (ResourceBundle bundle : bundles) {
            String groupName = bundle.getName();
            Group group = new Group(groupName);
            List<Resource> resources = service.getResources(rcontext, groupName, ResourceType.any.name());
            if (resources != null) {
                for (Resource resource : resources) {
                    ro.isdc.wro.model.resource.Resource wr = toWroResource(groupName, resource);
                    if (wr != null) {
                        group.addResource(wr);
                    }
                }
            }
            model.addGroup(group);
        }
        return model;
    }

    protected ro.isdc.wro.model.resource.Resource toWroResource(String bundle, Resource resource) {
        ro.isdc.wro.model.resource.ResourceType type = toWroResourceType(resource.getType());
        if (type == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot handle resource type '" + resource.getType() + "' for resource '" + resource.getName()
                        + "'");
            }
            return null;
        }
        String uri = NuxeoUriLocator.getUri(resource);
        if (uri == null) {
            log.error(
                    "Cannot handle resource '" + resource.getName() + "' for bundle '" + bundle + "': no uri resolved");
            return null;
        }
        ro.isdc.wro.model.resource.Resource res = ro.isdc.wro.model.resource.Resource.create(uri, type);
        res.setMinimize(resource.isShrinkable());
        return res;
    }

    protected ro.isdc.wro.model.resource.ResourceType toWroResourceType(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        if (ResourceType.js.name().equals(type.toLowerCase())) {
            return ro.isdc.wro.model.resource.ResourceType.JS;
        }
        if (ResourceType.css.name().equals(type.toLowerCase())) {
            return ro.isdc.wro.model.resource.ResourceType.CSS;
        }
        return null;
    }

    public void destroy() {
    }

}

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
package org.nuxeo.ecm.web.resources.jsf.handler;

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceContextImpl;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.facelets.tag.TagAttributeImpl;
import com.sun.faces.facelets.tag.TagAttributesImpl;
import com.sun.faces.facelets.tag.jsf.html.ScriptResourceHandler;
import com.sun.faces.facelets.tag.jsf.html.StylesheetResourceHandler;
import com.sun.faces.facelets.tag.ui.IncludeHandler;

/**
 * Tag handler for resource bundles, resolving early resources that need to be included at build time (e.g JSF and XHTML
 * resources for now).
 *
 * @since 7.4
 */
public class ResourceBundleHandler extends ScriptResourceHandler {

    protected final TagAttribute name;

    public ResourceBundleHandler(ComponentConfig config) {
        super(config);
        this.name = getRequiredAttribute("name");
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        String bundleName = name.getValue(ctx);
        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        // first include handlers that match JSF resources
        List<Resource> jsfjsr = wrm.getResources(new ResourceContextImpl(), bundleName, ResourceType.jsfjs.name());
        if (jsfjsr != null && !jsfjsr.isEmpty()) {
            String rendererType = "javax.faces.resource.Script";
            for (Resource resource : jsfjsr) {
                ComponentConfig config = getJSFResourceComponentConfig(resource.getURI(), rendererType);
                new ScriptResourceHandler(config).apply(ctx, parent);
            }
        }
        List<Resource> jsfcssr = wrm.getResources(new ResourceContextImpl(), bundleName, ResourceType.jsfjs.name());
        if (jsfcssr != null && !jsfcssr.isEmpty()) {
            String rendererType = "javax.faces.resource.Stylesheet";
            for (Resource resource : jsfcssr) {
                ComponentConfig config = getJSFResourceComponentConfig(resource.getURI(), rendererType);
                new StylesheetResourceHandler(config).apply(ctx, parent);
            }
        }
        // then include xhtml templates
        List<Resource> xhtmlr = wrm.getResources(new ResourceContextImpl(), bundleName, ResourceType.xhtml.name());
        if (xhtmlr != null && !xhtmlr.isEmpty()) {
            for (Resource resource : xhtmlr) {
                ComponentConfig tagConfig = getComponentConfig();
                TagAttributeImpl srcAttr = getTagAttribute("src", resource.getURI());
                TagAttributesImpl attributes = new TagAttributesImpl(new TagAttribute[] { srcAttr });
                TagConfig config = TagConfigFactory.createTagConfig(tagConfig, tagConfig.getTagId(), attributes, null);
                new IncludeHandler(config).apply(ctx, parent);
            }
        }
        // then let other resources (css, js, html) be processed by the component at render time
        super.apply(ctx, parent);
    }

    protected ComponentConfig getJSFResourceComponentConfig(String resourceName, String rendererType) {
        ComponentConfig tagConfig = getComponentConfig();
        String componentType = UIOutput.COMPONENT_TYPE;
        TagAttributeImpl nameAttr = getTagAttribute("name", resourceName);
        TagAttributeImpl targetAttr = getTagAttribute("target", "head");
        TagAttributesImpl attributes = new TagAttributesImpl(new TagAttribute[] { nameAttr, targetAttr });
        ComponentConfig config = TagConfigFactory.createComponentConfig(tagConfig, tagConfig.getTagId(), attributes,
                nextHandler, componentType, rendererType);
        return config;
    }

    protected TagAttributeImpl getTagAttribute(String name, String value) {
        return new TagAttributeImpl(getComponentConfig().getTag().getLocation(), "", name, name, value);
    }

}

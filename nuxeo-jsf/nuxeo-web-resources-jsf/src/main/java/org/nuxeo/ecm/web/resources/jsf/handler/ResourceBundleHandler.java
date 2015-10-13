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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.MetaTagHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceContextImpl;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.jsf.ResourceBundleRenderer;
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
public class ResourceBundleHandler extends MetaTagHandler {

    private static final Log log = LogFactory.getLog(ResourceBundleHandler.class);

    protected final TagConfig config;

    protected final TagAttribute name;

    protected final TagAttribute type;

    protected final TagAttribute items;

    protected final TagAttribute target;

    protected final TagAttribute[] vars;

    protected final ResourceType[] handledTypesArray = { ResourceType.css, ResourceType.js, ResourceType.jsfcss,
            ResourceType.jsfjs, ResourceType.html, ResourceType.xhtml, ResourceType.xhtmlfirst };

    public ResourceBundleHandler(TagConfig config) {
        super(config);
        this.config = config;
        name = getAttribute("name");
        items = getAttribute("items");
        type = getAttribute("type");
        target = getAttribute("target");
        vars = tag.getAttributes().getAll();
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        String typeValue = null;
        if (type != null) {
            typeValue = type.getValue(ctx);
        }
        ResourceType rtype = resolveType(typeValue);
        if (rtype == null) {
            log.error(String.format("Unsupported type '%s' on tag nxr:resourceBundle at %s", typeValue,
                    tag.getLocation()));
            return;
        }

        List<String> bundles = new ArrayList<String>();
        if (name != null) {
            String bundleName = name.getValue(ctx);
            if (!StringUtils.isBlank(bundleName)) {
                bundles.add(bundleName);
            }
        }
        if (items != null) {
            Object itemsValue = items.getObject(ctx);
            if (itemsValue instanceof Collection) {
                bundles.addAll((Collection<String>) itemsValue);
            } else if (itemsValue instanceof Object[]) {
                bundles.addAll(Arrays.asList((String[]) itemsValue));
            } else if (itemsValue instanceof String) {
                bundles.add((String) itemsValue);
            } else {
                log.error(String.format("Unsupported value '%s' for attribute 'items' on tag nxr:resourceBundle at %s",
                        itemsValue, tag.getLocation()));
            }
        }

        if (bundles.isEmpty()) {
            return;
        }

        String targetValue = null;
        if (target != null) {
            targetValue = target.getValue(ctx);
        }

        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        LeafFaceletHandler leaf = new LeafFaceletHandler();
        if (rtype == ResourceType.any) {
            for (String bundle : bundles) {
                String cssTarget = targetValue;
                String jsTarget = targetValue;
                String htmlTarget = targetValue;
                if (vars != null) {
                    for (TagAttribute var : vars) {
                        if ("target_css".equalsIgnoreCase(var.getLocalName())) {
                            cssTarget = var.getValue(ctx);
                        } else if ("target_js".equalsIgnoreCase(var.getLocalName())) {
                            jsTarget = var.getValue(ctx);
                        } else if ("target_html".equalsIgnoreCase(var.getLocalName())) {
                            htmlTarget = var.getValue(ctx);
                        }
                    }
                }
                // first include handlers that match JSF resources
                applyBundle(ctx, parent, wrm, bundle, ResourceType.jsfcss, cssTarget, leaf);
                applyBundle(ctx, parent, wrm, bundle, ResourceType.jsfjs, jsTarget, leaf);
                // then include xhtmlfirst templates
                applyBundle(ctx, parent, wrm, bundle, ResourceType.xhtmlfirst, null, leaf);
                // then let other resources (css, js, html) be processed by the component at render time
                applyBundle(ctx, parent, wrm, bundle, ResourceType.css, cssTarget, nextHandler);
                applyBundle(ctx, parent, wrm, bundle, ResourceType.js, jsTarget, nextHandler);
                applyBundle(ctx, parent, wrm, bundle, ResourceType.html, htmlTarget, nextHandler);
                // then include xhtml templates
                applyBundle(ctx, parent, wrm, bundle, ResourceType.xhtml, null, leaf);
            }
        } else {
            for (String bundle : bundles) {
                applyBundle(ctx, parent, wrm, bundle, rtype, targetValue, leaf);
            }
        }
    }

    protected void applyBundle(FaceletContext ctx, UIComponent parent, WebResourceManager wrm, String bundle,
            ResourceType type, String targetValue, FaceletHandler nextHandler) throws IOException {
        List<Resource> rs = wrm.getResources(new ResourceContextImpl(), bundle, type.name());
        if (rs != null && !rs.isEmpty()) {
            switch (type) {
            case jsfjs:
                for (Resource r : rs) {
                    ComponentConfig config = getJSFResourceComponentConfig(r, "javax.faces.resource.Script",
                            targetValue, nextHandler);
                    new ScriptResourceHandler(config).apply(ctx, parent);
                }
                break;
            case jsfcss:
                for (Resource resource : rs) {
                    ComponentConfig config = getJSFResourceComponentConfig(resource, "javax.faces.resource.Stylesheet",
                            targetValue, nextHandler);
                    new StylesheetResourceHandler(config).apply(ctx, parent);
                }
                break;
            case xhtmlfirst:
                includeXHTML(ctx, parent, rs, nextHandler);
                break;
            case xhtml:
                includeXHTML(ctx, parent, rs, nextHandler);
                break;
            case js:
                includeResourceBundle(ctx, parent, bundle, type, targetValue, nextHandler);
                break;
            case css:
                includeResourceBundle(ctx, parent, bundle, type, targetValue, nextHandler);
                break;
            case html:
                includeResourceBundle(ctx, parent, bundle, type, targetValue, nextHandler);
                break;
            default:
                break;
            }
        }
    }

    protected ResourceType resolveType(String type) {
        if (StringUtils.isBlank(type)) {
            return ResourceType.any;
        }
        ResourceType parsed = ResourceType.parse(type);
        if (parsed != null) {
            List<ResourceType> handled = Arrays.asList(handledTypesArray);
            if (handled.contains(parsed)) {
                return parsed;
            }
        }
        return null;
    }

    protected TagAttributeImpl getTagAttribute(String name, String value) {
        return new TagAttributeImpl(tag.getLocation(), "", name, name, value);
    }

    protected ComponentConfig getJSFResourceComponentConfig(Resource resource, String rendererType, String target,
            FaceletHandler nextHandler) {
        String componentType = UIOutput.COMPONENT_TYPE;
        String uri = resource.getURI();
        String resourceName;
        String resourceLib;
        int i = uri != null ? uri.indexOf(":") : -1;
        if (i > 0) {
            resourceLib = uri.substring(0, i);
            resourceName = uri.substring(i + 1);
        } else {
            resourceLib = null;
            resourceName = uri;
        }
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        attrs.add(getTagAttribute("name", resourceName));
        if (!StringUtils.isBlank(resourceLib)) {
            attrs.add(getTagAttribute("library", resourceLib));
        }
        if (!StringUtils.isBlank(target)) {
            attrs.add(getTagAttribute("target", target));
        }
        TagAttributesImpl attributes = new TagAttributesImpl(attrs.toArray(new TagAttribute[] {}));
        ComponentConfig cconfig = TagConfigFactory.createComponentConfig(config, tagId, attributes, nextHandler,
                componentType, rendererType);
        return cconfig;
    }

    protected void includeResourceBundle(FaceletContext ctx, UIComponent parent, String name, ResourceType type,
            String target, FaceletHandler nextHandler) throws IOException {
        String componentType = UIOutput.COMPONENT_TYPE;
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        attrs.add(getTagAttribute("name", name));
        attrs.add(getTagAttribute("type", type.name()));
        if (!StringUtils.isBlank(target)) {
            attrs.add(getTagAttribute("target", target));
        }
        TagAttributesImpl attributes = new TagAttributesImpl(attrs.toArray(new TagAttribute[] {}));
        ComponentConfig cconfig = TagConfigFactory.createComponentConfig(config, tagId, attributes, nextHandler,
                componentType, ResourceBundleRenderer.RENDERER_TYPE);
        new ComponentHandler(cconfig).apply(ctx, parent);
    }

    protected void includeXHTML(FaceletContext ctx, UIComponent parent, List<Resource> rs, FaceletHandler leaf)
            throws IOException {
        if (rs != null && !rs.isEmpty()) {
            for (Resource r : rs) {
                String uri = r.getURI();
                if (StringUtils.isBlank(uri)) {
                    log.error(String.format("Invalid resource '%s': no uri defined at %s", r.getName(),
                            tag.getLocation()));
                    continue;
                }
                TagAttributeImpl srcAttr = getTagAttribute("src", uri);
                TagAttributesImpl attributes = new TagAttributesImpl(new TagAttribute[] { srcAttr });
                TagConfig xconfig = TagConfigFactory.createTagConfig(config, tagId, attributes, leaf);
                new IncludeHandler(xconfig).apply(ctx, parent);
            }
        }
    }

}

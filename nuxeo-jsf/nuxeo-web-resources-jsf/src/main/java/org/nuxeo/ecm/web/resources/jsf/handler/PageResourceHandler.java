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
import org.nuxeo.ecm.web.resources.jsf.PageResourceRenderer;
import org.nuxeo.ecm.web.resources.jsf.ResourceBundleRenderer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.PageDescriptor;

import com.sun.faces.facelets.tag.TagAttributeImpl;
import com.sun.faces.facelets.tag.TagAttributesImpl;
import com.sun.faces.facelets.tag.jsf.html.ScriptResourceHandler;
import com.sun.faces.facelets.tag.jsf.html.StylesheetResourceHandler;
import com.sun.faces.facelets.tag.ui.IncludeHandler;

/**
 * Tag handler for page resource bundles, resolving early resources that need to be included at build time (e.g JSF and
 * XHTML resources for now).
 *
 * @since 7.10
 */
public class PageResourceHandler extends MetaTagHandler {

    private static final Log log = LogFactory.getLog(PageResourceHandler.class);

    protected final TagConfig config;

    protected final TagAttribute name;

    protected final TagAttribute type;

    protected final TagAttribute flavor;

    protected final TagAttribute target;

    protected final TagAttribute[] vars;

    protected final ResourceType[] handledTypesArray = { ResourceType.css, ResourceType.js, ResourceType.jsfcss,
            ResourceType.jsfjs, ResourceType.html, ResourceType.xhtml, ResourceType.xhtmlfirst };

    public PageResourceHandler(TagConfig config) {
        super(config);
        this.config = config;
        name = getAttribute("name");
        type = getAttribute("type");
        flavor = getAttribute("flavor");
        target = getAttribute("target");
        vars = tag.getAttributes().getAll();
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        return null;
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        if (name == null) {
            return;
        }
        String pageName = name.getValue(ctx);
        ThemeStylingService tss = Framework.getService(ThemeStylingService.class);
        PageDescriptor page = tss.getPage(pageName);
        if (page == null) {
            // NO-OP
            return;
        }
        String typeValue = null;
        if (type != null) {
            typeValue = type.getValue(ctx);
        }
        ResourceType rtype = resolveType(typeValue);
        if (rtype == null) {
            log.error("Unsupported type '" + typeValue + "' on tag nxr:resourceBundle at " + tag.getLocation());
            return;
        }

        String flavorValue = null;
        if (flavor != null) {
            flavorValue = flavor.getValue(ctx);
        }

        String targetValue = null;
        if (target != null) {
            targetValue = target.getValue(ctx);
        }

        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        LeafFaceletHandler leaf = new LeafFaceletHandler();
        if (rtype == ResourceType.any) {
            String cssTarget = targetValue;
            String jsTarget = targetValue;
            String htmlTarget = targetValue;
            if (vars != null) {
                for (TagAttribute var : vars) {
                    if ("target_css".equalsIgnoreCase(var.getLocalName())) {
                        String val = resolveAttribute(ctx, var);
                        if (val != null) {
                            cssTarget = val;
                        }
                    } else if ("target_js".equalsIgnoreCase(var.getLocalName())) {
                        String val = resolveAttribute(ctx, var);
                        if (val != null) {
                            jsTarget = val;
                        }
                    } else if ("target_html".equalsIgnoreCase(var.getLocalName())) {
                        String val = resolveAttribute(ctx, var);
                        if (val != null) {
                            htmlTarget = val;
                        }
                    }
                }
            }
            // first include handlers that match JSF resources
            applyPage(ctx, parent, wrm, page, ResourceType.jsfcss, flavorValue, cssTarget, leaf);
            applyPage(ctx, parent, wrm, page, ResourceType.jsfjs, flavorValue, jsTarget, leaf);
            // then include xhtmlfirst templates
            applyPage(ctx, parent, wrm, page, ResourceType.xhtmlfirst, flavorValue, null, leaf);
            // then let other resources (css, js, html) be processed by the component at render time
            applyPage(ctx, parent, wrm, page, ResourceType.css, flavorValue, cssTarget, nextHandler);
            applyPage(ctx, parent, wrm, page, ResourceType.js, flavorValue, jsTarget, nextHandler);
            applyPage(ctx, parent, wrm, page, ResourceType.html, flavorValue, htmlTarget, nextHandler);
            // then include xhtml templates
            applyPage(ctx, parent, wrm, page, ResourceType.xhtml, flavorValue, null, leaf);
        } else {
            applyPage(ctx, parent, wrm, page, rtype, flavorValue, targetValue, leaf);
        }
    }

    protected void applyPage(FaceletContext ctx, UIComponent parent, WebResourceManager wrm, PageDescriptor page,
            ResourceType type, String flavor, String targetValue, FaceletHandler nextHandler) throws IOException {
        switch (type) {
        case jsfjs:
            for (Resource r : retrieveResources(wrm, page, type)) {
                String rtarget = r.getTarget();
                ComponentConfig config = getJSFResourceComponentConfig(r, "javax.faces.resource.Script",
                        rtarget == null ? targetValue : rtarget, nextHandler);
                new ScriptResourceHandler(config).apply(ctx, parent);
            }
            break;
        case jsfcss:
            for (Resource r : retrieveResources(wrm, page, type)) {
                String rtarget = r.getTarget();
                ComponentConfig config = getJSFResourceComponentConfig(r, "javax.faces.resource.Stylesheet",
                        rtarget == null ? targetValue : rtarget, nextHandler);
                new StylesheetResourceHandler(config).apply(ctx, parent);
            }
            break;
        case xhtmlfirst:
            includeXHTML(ctx, parent, retrieveResources(wrm, page, type), nextHandler);
            break;
        case xhtml:
            includeXHTML(ctx, parent, retrieveResources(wrm, page, type), nextHandler);
            break;
        case js:
            includePageResource(ctx, parent, page.getName(), type, flavor, targetValue, nextHandler);
            break;
        case css:
            includePageResource(ctx, parent, page.getName(), type, flavor, targetValue, nextHandler);
            break;
        case html:
            for (String bundle : page.getResourceBundles()) {
                includeResourceBundle(ctx, parent, bundle, type, flavor, targetValue, nextHandler);
            }
            break;
        default:
            break;
        }

    }

    // helper methods

    protected List<Resource> retrieveResources(WebResourceManager wrm, String bundle, ResourceType type) {
        return wrm.getResources(new ResourceContextImpl(), bundle, type.name());
    }

    protected List<Resource> retrieveResources(WebResourceManager wrm, PageDescriptor page, ResourceType type) {
        List<Resource> res = new ArrayList<Resource>();
        List<String> bundles = page.getResourceBundles();
        for (String bundle : bundles) {
            res.addAll(retrieveResources(wrm, bundle, type));
        }
        return res;
    }

    protected String resolveAttribute(FaceletContext ctx, TagAttribute var) {
        String val = var.getValue(ctx);
        if (!StringUtils.isBlank(val)) {
            return val;
        }
        return null;
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

    protected void includeXHTML(FaceletContext ctx, UIComponent parent, List<Resource> rs, FaceletHandler leaf)
            throws IOException {
        if (rs != null && !rs.isEmpty()) {
            for (Resource r : rs) {
                String uri = r.getURI();
                if (StringUtils.isBlank(uri)) {
                    log.error("Invalid resource '" + r.getName() + "': no uri defined at " + tag.getLocation());
                    continue;
                }
                TagAttributeImpl srcAttr = getTagAttribute("src", uri);
                TagAttributesImpl attributes = new TagAttributesImpl(new TagAttribute[] { srcAttr });
                TagConfig xconfig = TagConfigFactory.createTagConfig(config, tagId, attributes, leaf);
                new IncludeHandler(xconfig).apply(ctx, parent);
            }
        }
    }

    protected void includeResourceBundle(FaceletContext ctx, UIComponent parent, String name, ResourceType type,
            String flavor, String target, FaceletHandler nextHandler) throws IOException {
        String componentType = UIOutput.COMPONENT_TYPE;
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        attrs.add(getTagAttribute("name", name));
        attrs.add(getTagAttribute("type", type.name()));
        if (!StringUtils.isBlank(target)) {
            attrs.add(getTagAttribute("target", target));
        }
        if (!StringUtils.isBlank(flavor)) {
            attrs.add(getTagAttribute("flavor", flavor));
        }
        TagAttributesImpl attributes = new TagAttributesImpl(attrs.toArray(new TagAttribute[] {}));
        ComponentConfig cconfig = TagConfigFactory.createComponentConfig(config, tagId, attributes, nextHandler,
                componentType, ResourceBundleRenderer.RENDERER_TYPE);
        new ComponentHandler(cconfig).apply(ctx, parent);
    }

    protected void includePageResource(FaceletContext ctx, UIComponent parent, String name, ResourceType type,
            String flavor, String target, FaceletHandler nextHandler) throws IOException {
        String componentType = UIOutput.COMPONENT_TYPE;
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        attrs.add(getTagAttribute("name", name));
        attrs.add(getTagAttribute("type", type.name()));
        if (!StringUtils.isBlank(target)) {
            attrs.add(getTagAttribute("target", target));
        }
        if (!StringUtils.isBlank(flavor)) {
            attrs.add(getTagAttribute("flavor", flavor));
        }
        TagAttributesImpl attributes = new TagAttributesImpl(attrs.toArray(new TagAttribute[] {}));
        ComponentConfig cconfig = TagConfigFactory.createComponentConfig(config, tagId, attributes, nextHandler,
                componentType, PageResourceRenderer.RENDERER_TYPE);
        new ComponentHandler(cconfig).apply(ctx, parent);
    }

}

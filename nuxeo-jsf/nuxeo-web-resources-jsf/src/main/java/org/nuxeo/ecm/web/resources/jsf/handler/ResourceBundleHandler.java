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
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.facelets.tag.jsf.html.ScriptResourceHandler;
import com.sun.faces.facelets.tag.jsf.html.StylesheetResourceHandler;

/**
 * Tag handler for resource bundles, resolving early resources that need to be included at build time (e.g JSF and XHTML
 * resources for now).
 *
 * @since 7.4
 */
public class ResourceBundleHandler extends PageResourceHandler {

    private static final Log log = LogFactory.getLog(ResourceBundleHandler.class);

    protected final TagAttribute items;

    public ResourceBundleHandler(TagConfig config) {
        super(config);
        items = getAttribute("items");
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
            log.error("Unsupported type '" + typeValue + "' on tag nxr:resourceBundle at " + tag.getLocation());
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
            for (String bundle : bundles) {
                // first include handlers that match JSF resources
                applyBundle(ctx, parent, wrm, bundle, ResourceType.jsfcss, flavorValue, cssTarget, leaf);
                applyBundle(ctx, parent, wrm, bundle, ResourceType.jsfjs, flavorValue, jsTarget, leaf);
                // then include xhtmlfirst templates
                applyBundle(ctx, parent, wrm, bundle, ResourceType.xhtmlfirst, flavorValue, null, leaf);
                // then let other resources (css, js, html) be processed by the component at render time
                applyBundle(ctx, parent, wrm, bundle, ResourceType.css, flavorValue, cssTarget, nextHandler);
                applyBundle(ctx, parent, wrm, bundle, ResourceType.js, flavorValue, jsTarget, nextHandler);
                applyBundle(ctx, parent, wrm, bundle, ResourceType.html, flavorValue, htmlTarget, nextHandler);
                // then include xhtml templates
                applyBundle(ctx, parent, wrm, bundle, ResourceType.xhtml, flavorValue, null, leaf);
            }
        } else {
            for (String bundle : bundles) {
                applyBundle(ctx, parent, wrm, bundle, rtype, flavorValue, targetValue, leaf);
            }
        }
    }

    protected void applyBundle(FaceletContext ctx, UIComponent parent, WebResourceManager wrm, String bundle,
            ResourceType type, String flavor, String targetValue, FaceletHandler nextHandler) throws IOException {
        switch (type) {
        case jsfjs:
            for (Resource r : retrieveResources(wrm, bundle, type)) {
                String rtarget = r.getTarget();
                ComponentConfig config = getJSFResourceComponentConfig(r, "javax.faces.resource.Script",
                        rtarget == null ? targetValue : rtarget, nextHandler);
                new ScriptResourceHandler(config).apply(ctx, parent);
            }
            break;
        case jsfcss:
            for (Resource r : retrieveResources(wrm, bundle, type)) {
                String rtarget = r.getTarget();
                ComponentConfig config = getJSFResourceComponentConfig(r, "javax.faces.resource.Stylesheet",
                        rtarget == null ? targetValue : rtarget, nextHandler);
                new StylesheetResourceHandler(config).apply(ctx, parent);
            }
            break;
        case xhtmlfirst:
            includeXHTML(ctx, parent, retrieveResources(wrm, bundle, type), nextHandler);
            break;
        case xhtml:
            includeXHTML(ctx, parent, retrieveResources(wrm, bundle, type), nextHandler);
            break;
        case js:
            includeResourceBundle(ctx, parent, bundle, type, flavor, targetValue, nextHandler);
            break;
        case css:
            includeResourceBundle(ctx, parent, bundle, type, flavor, targetValue, nextHandler);
            break;
        case html:
            includeResourceBundle(ctx, parent, bundle, type, flavor, targetValue, nextHandler);
            break;
        default:
            break;
        }
    }

}

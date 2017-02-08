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

        String includeTimestampValue = null;
        if (includeTimestamp != null) {
            includeTimestampValue = includeTimestamp.getValue(ctx);
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
                applyBundle(ctx, parent, wrm, bundle, ResourceType.jsfcss, flavorValue, cssTarget,
                        includeTimestampValue, leaf);
                applyBundle(ctx, parent, wrm, bundle, ResourceType.jsfjs, flavorValue, jsTarget, includeTimestampValue,
                        leaf);
                // then include xhtmlfirst templates
                applyBundle(ctx, parent, wrm, bundle, ResourceType.xhtmlfirst, flavorValue, null, includeTimestampValue,
                        leaf);
                // then let other resources (css, js, html) be processed by the component at render time
                applyBundle(ctx, parent, wrm, bundle, ResourceType.css, flavorValue, cssTarget, includeTimestampValue,
                        nextHandler);
                applyBundle(ctx, parent, wrm, bundle, ResourceType.js, flavorValue, jsTarget, includeTimestampValue,
                        nextHandler);
                applyBundle(ctx, parent, wrm, bundle, ResourceType.html, flavorValue, htmlTarget, includeTimestampValue,
                        nextHandler);
                // then include xhtml templates
                applyBundle(ctx, parent, wrm, bundle, ResourceType.xhtml, flavorValue, null, includeTimestampValue,
                        leaf);
            }
        } else {
            for (String bundle : bundles) {
                applyBundle(ctx, parent, wrm, bundle, rtype, flavorValue, targetValue, includeTimestampValue, leaf);
            }
        }
    }

    protected void applyBundle(FaceletContext ctx, UIComponent parent, WebResourceManager wrm, String bundle,
            ResourceType type, String flavor, String targetValue, String includeTimestamp, FaceletHandler nextHandler)
                    throws IOException {
        switch (type) {
        case jsfjs:
            for (Resource r : retrieveResources(wrm, bundle, type)) {
                String rtarget = r.getTarget();
                ComponentConfig config = getJSFResourceComponentConfig(r, "javax.faces.resource.Script",
                        rtarget == null ? targetValue : rtarget, includeTimestamp, nextHandler);
                new ScriptResourceHandler(config).apply(ctx, parent);
            }
            break;
        case jsfcss:
            for (Resource r : retrieveResources(wrm, bundle, type)) {
                String rtarget = r.getTarget();
                ComponentConfig config = getJSFResourceComponentConfig(r, "javax.faces.resource.Stylesheet",
                        rtarget == null ? targetValue : rtarget, includeTimestamp, nextHandler);
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
            includeResourceBundle(ctx, parent, bundle, type, flavor, targetValue, includeTimestamp, nextHandler);
            break;
        case css:
            includeResourceBundle(ctx, parent, bundle, type, flavor, targetValue, includeTimestamp, nextHandler);
            break;
        case html:
            includeResourceBundle(ctx, parent, bundle, type, flavor, targetValue, includeTimestamp, nextHandler);
            break;
        default:
            break;
        }
    }

}

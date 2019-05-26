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
package org.nuxeo.ecm.webapp.resources;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.jsf.PageResourceRenderer;
import org.nuxeo.ecm.web.resources.jsf.ResourceBundleRenderer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Moves CSS files to the start of the head tag and reorders js resources.
 *
 * @since 7.10
 */
public class NuxeoWebResourceDispatcher implements ComponentSystemEventListener {

    private static final Log log = LogFactory.getLog(NuxeoWebResourceDispatcher.class);

    protected static String TARGET_HEAD = "head";

    protected static String SLOT_HEAD_START = "headstart";

    private static String SLOT_BODY_START = "bodystart";

    private static String SLOT_BODY_END = "bodyend";

    private static String DEFER_JS_PROP = "nuxeo.jsf.deferJavaScriptLoading";

    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        UIViewRoot root = ctx.getViewRoot();
        boolean ajaxRequest = ctx.getPartialViewContext().isAjaxRequest();
        if (ajaxRequest) {
            // do not interfere with ajax scripts re-rendering logics
            List<UIComponent> resources = root.getComponentResources(ctx, TARGET_HEAD);
            String message = "Head resource %s on ajax request";
            for (UIComponent r : resources) {
                logResourceInfo(r, message);
            }
            return;
        }
        List<UIComponent> cssResources = new ArrayList<>();
        List<UIComponent> otherResources = new ArrayList<>();
        List<UIComponent> resources = root.getComponentResources(ctx, TARGET_HEAD);
        for (UIComponent r : resources) {
            if (isCssResource(ctx, r)) {
                cssResources.add(r);
            } else {
                otherResources.add(r);
            }
        }

        moveResources(ctx, root, cssResources, TARGET_HEAD, SLOT_HEAD_START,
                "Pushing head resource %s at the beggining of head tag");
        if (isDeferJavaScriptLoading()) {
            moveResources(ctx, root, otherResources, TARGET_HEAD, SLOT_BODY_START,
                    "Pushing head resource %s at the beggining of body tag");
        }
    }

    protected void moveResources(FacesContext ctx, UIViewRoot root, List<UIComponent> resources, String removeFrom,
            String addTo, String message) {
        // push target resources
        List<UIComponent> existing = new ArrayList<>(root.getComponentResources(ctx, addTo));
        for (UIComponent r : resources) {
            ComponentUtils.setRelocated(r);
            root.removeComponentResource(ctx, r, removeFrom);
            root.addComponentResource(ctx, r, addTo);
            logResourceInfo(r, message);
        }
        // add them back again for head resources to be still before them
        for (UIComponent r : existing) {
            root.addComponentResource(ctx, r, addTo);
        }
    }

    protected void logResourceInfo(UIComponent resource, String message) {
        if (log.isDebugEnabled()) {
            String name = getLogName(resource);
            if (StringUtils.isBlank(name)) {
                log.debug(String.format(message, resource));
            } else {
                log.debug(String.format(message, name));
            }
        }
    }

    protected String getLogName(UIComponent resource) {
        String name = (String) resource.getAttributes().get("name");
        if (StringUtils.isBlank(name)) {
            return (String) resource.getAttributes().get("src");
        }
        return name;
    }

    protected boolean isCssResource(FacesContext ctx, UIComponent r) {
        String rtype = r.getRendererType();
        if ("javax.faces.resource.Stylesheet".equals(rtype)) {
            return true;
        }
        if (ResourceBundleRenderer.RENDERER_TYPE.equals(rtype) || PageResourceRenderer.RENDERER_TYPE.equals(rtype)) {
            String type = (String) r.getAttributes().get("type");
            if (ResourceType.css.equals(type) || ResourceType.jsfcss.equals(type)) {
                return true;
            }
            return false;
        }
        String name = (String) r.getAttributes().get("name");
        if (name == null) {
            return false;
        }
        name = name.toLowerCase();
        if (name.contains(".css") || name.contains(".ecss")) {
            return true;
        }
        return false;
    }

    public boolean isDeferJavaScriptLoading() {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        return cs.isBooleanTrue(DEFER_JS_PROP);
    }

    public String getHeadStartTarget() {
        return SLOT_HEAD_START;
    }

    public String getBodyStartTarget() {
        return SLOT_BODY_START;
    }

    public String getBodyEndTarget() {
        return SLOT_BODY_END;
    }

    public String getHeadJavaScriptTarget() {
        return isDeferJavaScriptLoading() ? SLOT_BODY_END : SLOT_BODY_START;
    }

    public String getBodyJavaScriptTarget() {
        return isDeferJavaScriptLoading() ? SLOT_BODY_END : null;
    }

}

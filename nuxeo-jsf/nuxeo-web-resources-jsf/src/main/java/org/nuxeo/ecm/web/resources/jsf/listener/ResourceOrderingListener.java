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
package org.nuxeo.ecm.web.resources.jsf.listener;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.jsf.ResourceBundleRenderer;

/**
 * Moves CSS files to the start of the head tag.
 *
 * @since 7.10
 */
public class ResourceOrderingListener implements SystemEventListener {

    private static final Log log = LogFactory.getLog(ResourceOrderingListener.class);

    protected static String TARGET_HEAD = "head";

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        UIViewRoot root = (UIViewRoot) event.getSource();
        FacesContext ctx = FacesContext.getCurrentInstance();
        List<UIComponent> cssResources = new ArrayList<UIComponent>();
        List<UIComponent> otherResources = new ArrayList<UIComponent>();
        List<UIComponent> resources = root.getComponentResources(ctx, TARGET_HEAD);
        for (UIComponent r : resources) {
            if (isCssResource(ctx, r)) {
                cssResources.add(r);
            } else {
                otherResources.add(r);
            }
        }
        // remove all previous resources
        for (UIComponent r : resources) {
            root.removeComponentResource(ctx, r, TARGET_HEAD);
        }
        // add them back in desired order
        for (UIComponent r : cssResources) {
            root.addComponentResource(ctx, r, TARGET_HEAD);
        }
        for (UIComponent r : otherResources) {
            root.addComponentResource(ctx, r, TARGET_HEAD);
        }
    }

    protected String getName(UIComponent r) {
        return (String) r.getAttributes().get("name");
    }

    protected boolean isCssResource(FacesContext ctx, UIComponent r) {
        String rtype = r.getRendererType();
        if ("javax.faces.resource.Stylesheet".equals(rtype)) {
            return true;
        }
        if (ResourceBundleRenderer.RENDERER_TYPE.equals(rtype)) {
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

    @Override
    public boolean isListenerForSource(Object source) {
        return (source instanceof UIViewRoot);
    }

}

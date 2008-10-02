/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.jsf.taglib;

import javax.faces.component.UIComponent;
import javax.faces.webapp.UIComponentELTag;

public class PanelTag extends UIComponentELTag {

    private String identifier;

    private String url;

    private String loading;

    private String stylesheet;

    private String javascript;

    private String subviews;

    private String controlledBy;

    private String filter;

    private String visibleInPerspectives;

    @Override
    public String getComponentType() {
        return "nxthemes.panel";
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);

        // the panel's identifier
        component.getAttributes().put("identifier", identifier);

        // the panel's URL
        component.getAttributes().put("url", url);

        // the element to show while loading
        if (null != loading) {
            component.getAttributes().put("loading", loading);
        }

        // stylesheet
        if (null != stylesheet) {
            component.getAttributes().put("stylesheet", stylesheet);
        }

        // script
        if (null != javascript) {
            component.getAttributes().put("javascript", javascript);
        }

        // the sub-views
        if (null != subviews) {
            component.getAttributes().put("subviews", subviews);
        }

        // the perspective controller(s)
        component.getAttributes().put("controlledBy", controlledBy);

        // the panel's perspectives
        component.getAttributes().put("visibleInPerspectives",
                visibleInPerspectives);

        // Filter
        if (null != filter) {
            component.getAttributes().put("filter", filter);
        }
    }

    @Override
    public void release() {
        super.release();
        identifier = null;
        url = null;
        loading = null;
        stylesheet = null;
        javascript = null;
        subviews = null;
        controlledBy = null;
        visibleInPerspectives = null;
        filter = null;
    }

    public String getControlledBy() {
        return controlledBy;
    }

    public void setControlledBy(String controlledBy) {
        this.controlledBy = controlledBy;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVisibleInPerspectives() {
        return visibleInPerspectives;
    }

    public void setVisibleInPerspectives(String visibleInPerspectives) {
        this.visibleInPerspectives = visibleInPerspectives;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public String getJavascript() {
        return javascript;
    }

    public void setJavascript(String javascript) {
        this.javascript = javascript;
    }

    public String getSubviews() {
        return subviews;
    }

    public void setSubviews(String subviews) {
        this.subviews = subviews;
    }

    public String getLoading() {
        return loading;
    }

    public void setLoading(String loading) {
        this.loading = loading;
    }

}

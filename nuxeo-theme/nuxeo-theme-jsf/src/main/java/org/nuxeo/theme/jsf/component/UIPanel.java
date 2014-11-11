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

package org.nuxeo.theme.jsf.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.theme.jsf.Utils;

public class UIPanel extends UIOutput {

    private String identifier;

    private String url;

    private String loading;

    private String stylesheet;

    private String javascript;

    private String subviews;

    private String visibleInPerspectives;

    private String controlledBy;

    private String filter;

    @Override
    public void encodeBegin(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();

        final Map attributes = getAttributes();
        identifier = (String) attributes.get("identifier");
        url = (String) attributes.get("url");
        loading = (String) attributes.get("loading");
        stylesheet = (String) attributes.get("stylesheet");
        javascript = (String) attributes.get("javascript");
        subviews = (String) attributes.get("subviews");
        visibleInPerspectives = (String) attributes.get("visibleInPerspectives");
        controlledBy = (String) attributes.get("controlledBy");
        filter = (String) attributes.get("filter");

        // model
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("id", identifier);
        Map<String, Object> data = new HashMap<String, Object>();

        String[] query = url.split("\\?");
        if (query.length > 1) {
            Map<String, Object> form = new HashMap<String, Object>();
            for (String param : query[1].split("&")) {
                String[] kv = param.split("=");
                form.put(kv[0], kv[1]);
            }
            data.put("form", form);
            url = query[0];
        }
        data.put("url", url);

        if (null != loading) {
            data.put("loading", loading);
        }
        if (null != stylesheet) {
            data.put("css", stylesheet);
        }
        if (null != javascript) {
            data.put("script", javascript);
        }
        model.put("data", data);

        // model
        writer.startElement("ins", this);
        writer.writeAttribute("class", "model", null);
        writer.write(Utils.toJson(model));
        writer.endElement("ins");

        // view
        Map<String, Object> view = new HashMap<String, Object>();
        view.put("id", identifier);
        Map<String, Object> widget = new HashMap<String, Object>();
        widget.put("type", "panel");
        view.put("widget", widget);
        view.put("model", identifier);
        if (null != visibleInPerspectives) {
            view.put("perspectives", visibleInPerspectives.split(","));
        }
        if (null != subviews) {
            view.put("subviews", subviews.split(","));
        }
        if (null != controlledBy) {
            view.put("controllers", controlledBy.split(","));
        }
        if (null != filter) {
            view.put("filter", filter);
        }
        writer.startElement("ins", this);
        writer.writeAttribute("class", "view", null);
        writer.write(Utils.toJson(view));
    }

    @Override
    public void encodeEnd(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();
        writer.endElement("ins");
    }

    public String getControlledBy() {
        return controlledBy;
    }

    public void setControlledBy(final String controlledBy) {
        this.controlledBy = controlledBy;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public String getJavascript() {
        return javascript;
    }

    public void setJavascript(final String javascript) {
        this.javascript = javascript;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    public void setStylesheet(final String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public String getSubviews() {
        return subviews;
    }

    public void setSubviews(final String subviews) {
        this.subviews = subviews;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getVisibleInPerspectives() {
        return visibleInPerspectives;
    }

    public void setVisibleInPerspectives(final String visibleInPerspectives) {
        this.visibleInPerspectives = visibleInPerspectives;
    }

    public String getLoading() {
        return loading;
    }

    public void setLoading(String loading) {
        this.loading = loading;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

}

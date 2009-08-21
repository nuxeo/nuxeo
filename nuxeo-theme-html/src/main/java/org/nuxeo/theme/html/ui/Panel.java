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

package org.nuxeo.theme.html.ui;

import java.util.HashMap;
import java.util.Map;

public class Panel {

    public static String render(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();

        String identifier = params.get("identifier");
        String url = params.get("url");
        String loading = params.get("loading");
        String stylesheet = params.get("stylesheet");
        String javascript = params.get("javascript");
        String subviews = params.get("subviews");
        String visibleInPerspectives = params.get("visibleInPerspectives");
        String controlledBy = params.get("controlledBy");
        String filter = params.get("filter");

        // model
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("id", identifier);
        Map<String, Object> data = new HashMap<String, Object>();

        Map<String, Object> form = new HashMap<String, Object>();
        if (url != null) {
            String[] query = url.split("\\?");
            if (query.length > 1) {
                for (String param : query[1].split("&")) {
                    String[] kv = param.split("=");
                    form.put(kv[0], kv[1]);
                }
                url = query[0];
            }
        }
        // add context information
        form.put("org.nuxeo.theme.application.path", params.get("org.nuxeo.theme.application.path"));
        form.put("org.nuxeo.theme.application.name", params.get("org.nuxeo.theme.application.name"));

        data.put("form", form);
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
        sb.append(String.format("<ins class=\"model\">%s</ins>",
                org.nuxeo.theme.html.Utils.toJson(model)));

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

        sb.append(String.format("<ins class=\"view\">%s</ins>",
                org.nuxeo.theme.html.Utils.toJson(view)));
        return sb.toString();
    }

}

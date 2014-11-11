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

import org.nuxeo.theme.html.Utils;

public class Button {

    public static String render(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();

        String identifier = params.get("identifier");
        String controlledBy = params.get("controlledBy");
        String switchTo = params.get("switchTo");
        String link = params.get("link");
        String menu = params.get("menu");
        String label = params.get("label");
        String classNames = params.get("classNames");

        // view
        Map<String, Object> view = new HashMap<String, Object>();
        view.put("id", identifier);
        Map<String, Object> widget = new HashMap<String, Object>();
        widget.put("type", "button");
        view.put("widget", widget);
        if (null != switchTo) {
            String[] p = switchTo.split("/");
            if (p.length > 1) {
                view.put("perspectiveController", p[0]);
                view.put("toPerspective", p[1]);
            }
        }
        if (null != controlledBy) {
            view.put("controllers", controlledBy.split(","));
        }
        if (null != link) {
            view.put("link", link);
        }
        if (null != menu) {
            view.put("menu", menu);
        }
        if (null != classNames) {
            view.put("classNames", classNames);
        }
        view.put("label", label);

        sb.append(String.format("<ins class=\"view\">%s</ins>",
                Utils.toJson(view)));
        return sb.toString();
    }

}

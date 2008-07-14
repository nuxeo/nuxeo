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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.theme.html.Utils;

public class UITabs extends UIOutput {

    private String identifier;

    private String styleClass;

    private String controlledBy;

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();

        final Map<String, Object> attributes = getAttributes();
        identifier = (String) attributes.get("identifier");
        styleClass = (String) attributes.get("styleClass");
        controlledBy = (String) attributes.get("controlledBy");

        // view
        final Map<String, Object> view = new HashMap<String, Object>();
        view.put("id", identifier);
        final Map<String, Object> widget = new HashMap<String, Object>();
        widget.put("type", "tabs");
        if (styleClass != null) {
            widget.put("styleClass", styleClass);
        }
        if (null != controlledBy) {
            view.put("controllers", controlledBy.split(","));
        }

        final List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Object child : getChildren()) {
            if (child instanceof UITab) {
                UITab tab = (UITab) child;
                Map<String, Object> tabAttributes = tab.getAttributes();
                Map<String, Object> itemMap = new HashMap<String, Object>();
                itemMap.put("label", tabAttributes.get("label"));

                String link = (String) tabAttributes.get("link");
                if (link != null) {
                    itemMap.put("link", link);
                }

                String switchTo = (String) tabAttributes.get("switchTo");
                if (null != switchTo) {
                    itemMap.put("switchTo", switchTo);
                }

                items.add(itemMap);
            }
        }
        widget.put("items", items);
        view.put("widget", widget);

        writer.startElement("ins", this);
        writer.writeAttribute("class", "view", null);
        writer.write(Utils.toJson(view));
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

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(final String styleClass) {
        this.styleClass = styleClass;
    }

}

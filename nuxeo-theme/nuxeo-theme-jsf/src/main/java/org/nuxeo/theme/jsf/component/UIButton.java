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

public class UIButton extends UIOutput {

    private String identifier;

    private String controlledBy;

    private String switchTo;

    private String link;

    private String label;

    private String classNames;

    @Override
    public void encodeBegin(final FacesContext context) throws IOException {
        final ResponseWriter writer = context.getResponseWriter();

        final Map attributes = getAttributes();
        identifier = (String) attributes.get("identifier");
        controlledBy = (String) attributes.get("controlledBy");
        switchTo = (String) attributes.get("switchTo");
        link = (String) attributes.get("link");
        label = (String) attributes.get("label");
        classNames = (String) attributes.get("classNames");

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
        if (null != classNames) {
            view.put("classNames", classNames);
        }
        view.put("label", label);
        writer.startElement("ins", this);
        writer.writeAttribute("class", "view", null);
        writer.write(Utils.toJson(view));
    }

    @Override
    public void encodeEnd(final FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
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

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getLink() {
        return link;
    }

    public void setLink(final String link) {
        this.link = link;
    }

    public String getSwitchTo() {
        return switchTo;
    }

    public void setSwitchTo(final String switchTo) {
        this.switchTo = switchTo;
    }

    public String getClassNames() {
        return classNames;
    }

    public void setClassNames(final String classNames) {
        this.classNames = classNames;
    }
}

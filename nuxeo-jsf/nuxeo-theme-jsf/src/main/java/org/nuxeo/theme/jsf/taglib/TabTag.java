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

public class TabTag extends UIComponentELTag {
    private String label;

    private String switchTo;

    private String link;

    @Override
    public String getComponentType() {
        return "nxthemes.tab";
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);

        // the panel's identifier
        component.getAttributes().put("label", label);

        // the perspective to switch to
        if (switchTo != null) {
            component.getAttributes().put("switchTo", switchTo);
        }

        // the link
        if (link != null) {
            component.getAttributes().put("link", link);
        }
    }

    @Override
    public void release() {
        super.release();
        label = null;
        switchTo = null;
        link = null;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSwitchTo() {
        return switchTo;
    }

    public void setSwitchTo(String switchTo) {
        this.switchTo = switchTo;
    }

}

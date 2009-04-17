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

public class TabsTag extends UIComponentELTag {
    private String identifier;

    private String styleClass;

    private String controlledBy;

    @Override
    public String getComponentType() {
        return "nxthemes.tabs";
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);

        component.getAttributes().put("identifier", identifier);

        if (styleClass != null) {
            component.getAttributes().put("styleClass", styleClass);
        }

        // the perspective controller(s)
        if (controlledBy != null) {
            component.getAttributes().put("controlledBy", controlledBy);
        }
    }

    @Override
    public void release() {
        super.release();
        identifier = null;
        styleClass = null;
        controlledBy = null;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public String getControlledBy() {
        return controlledBy;
    }

    public void setControlledBy(String controlledBy) {
        this.controlledBy = controlledBy;
    }

}

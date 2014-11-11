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

public class ResourcesTag extends UIComponentELTag {

    private String ignoreLocal;
    
    @Override
    public String getComponentType() {
        return "nxthemes.resources";
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);
    }

    public String getIgnoreLocal() {
        return ignoreLocal;
    }

    public void setIgnoreLocal(String ignoreLocal) {
        this.ignoreLocal = ignoreLocal;
    }

    @Override
    public void release() {
        super.release();
    }

}

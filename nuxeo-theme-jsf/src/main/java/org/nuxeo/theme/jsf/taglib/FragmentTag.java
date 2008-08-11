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

public class FragmentTag extends UIComponentELTag {

    private String uid;

    private String engine;

    private String mode;

    @Override
    public String getComponentType() {
        return "nxthemes.fragment";
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);
        component.getAttributes().put("uid", uid);
        component.getAttributes().put("engine", engine);
        component.getAttributes().put("mode", mode);
    }

    @Override
    public void release() {
        super.release();
        uid = null;
        engine = null;
        mode = null;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}

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

package org.nuxeo.theme.webwidgets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("decoration")
public final class DecorationType {

    @XNode("@name")
    private String name;

    @XNodeList(value = "resource", type = String[].class, componentType = String.class)
    public String[] resources;

    @XNodeMap(value = "panel-decoration", key = "@mode", type = HashMap.class, componentType = PanelDecorationType.class)
    public Map<String, PanelDecorationType> panelDecorations;

    @XNodeMap(value = "widget-decoration", key = "@mode", type = HashMap.class, componentType = WidgetDecorationType.class)
    public Map<String, WidgetDecorationType> widgetDecorations;

    public String[] getResources() {
        return resources;
    }

    public String getName() {
        return name;
    }

    public PanelDecorationType getPanelDecoration(String mode) {
        return panelDecorations.get(mode);
    }

    public WidgetDecorationType getWidgetDecoration(String mode) {
        return widgetDecorations.get(mode);
    }

    public Set<String> getWindowDecorationModes() {
        return widgetDecorations.keySet();
    }

}

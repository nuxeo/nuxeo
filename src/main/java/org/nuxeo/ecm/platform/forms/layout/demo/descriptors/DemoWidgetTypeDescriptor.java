/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.demo.descriptors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoLayout;

/**
 * @author Anahide Tchertchian
 */
@XObject("widgetType")
public class DemoWidgetTypeDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("label")
    protected String label;

    @XNode("viewId")
    protected String viewId;

    @XNode("category")
    protected String category;

    @XNodeList(value = "layouts/layout", type = ArrayList.class, componentType = DemoLayoutDescriptor.class)
    protected List<DemoLayoutDescriptor> demoLayouts;

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getViewId() {
        return viewId;
    }

    public String getCategory() {
        return category;
    }

    public List<DemoLayout> getDemoLayouts() {
        List<DemoLayout> res = new ArrayList<DemoLayout>();
        res.addAll(demoLayouts);
        return res;
    }

}

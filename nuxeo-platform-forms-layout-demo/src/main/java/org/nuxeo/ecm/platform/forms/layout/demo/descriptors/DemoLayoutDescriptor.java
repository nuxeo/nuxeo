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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoLayout;
import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;

/**
 * @author Anahide Tchertchian
 */
@XObject("layout")
public class DemoLayoutDescriptor implements DemoLayout {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@src")
    protected String sourcePath;

    @XNode("@isListing")
    protected boolean listing = false;

    @XNode("@hideViewMode")
    protected boolean hideViewMode = false;

    public String getName() {
        return name;
    }

    public String getSourcePath() {
        return LayoutDemoManager.APPLICATION_PATH + sourcePath;
    }

    public boolean isListing() {
        return listing;
    }

    @Override
    public boolean isHideViewMode() {
        return hideViewMode;
    }

}

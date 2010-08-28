/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.htmleditor.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * TinyMCE plugin descriptor.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@XObject(value = "htmlEditorPlugin")
public class HtmlEditorPluginDescriptor {

    @XNode("@pluginName")
    private String pluginName;

    @XNode("@pluginButtonName")
    private String pluginButtonName;

    @XNode("@toolbarName")
    private String toolbarName;

    @XNode("@remove")
    private boolean remove = false;

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(final String pluginName) {
        this.pluginName = pluginName;
    }

    public String getPluginButtonName() {
        return pluginButtonName;
    }

    public void setPluginButtonName(final String pluginButtonName) {
        this.pluginButtonName = pluginButtonName;
    }

    public String getToolbarName() {
        return toolbarName;
    }

    public void setToolbarName(final String toolbarName) {
        this.toolbarName = toolbarName;
    }

    public Boolean getRemove() {
        return remove;
    }

    public void setRemove(final boolean remove) {
        this.remove = remove;
    }

}

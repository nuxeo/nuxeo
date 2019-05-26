/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     troger
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

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(final boolean remove) {
        this.remove = remove;
    }

}

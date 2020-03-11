/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.model.impl;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Can be used to register links to your module entry points in the main web engine page
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("shortcut")
public class ModuleShortcut {

    /**
     * The relative href to your entry point (relative to webengine root)
     */
    @XNode("@href")
    public String href;

    /**
     * A title. If not specified module name will be used.
     */
    @XNode("title")
    public String title;

    /**
     * An optional icon
     */
    @XNode("icon")
    public String icon;

    public ModuleShortcut() {
    }

    public ModuleShortcut(String href, String title) {
        this.href = href;
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getHref() {
        return href;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ModuleShortcut) {
            return ((ModuleShortcut) obj).href.equals(href);
        }
        return false;
    }

    @Override
    public String toString() {
        return href;
    }

}

/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.model.impl;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Can be used to register links to your module entry points in the main web
 * engine page
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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

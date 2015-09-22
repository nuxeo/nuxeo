/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.io.download;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for the permissions associated to a blob download.
 *
 * @since 7.4
 */
@XObject("permission")
public class DownloadPermissionDescriptor implements Comparable<DownloadPermissionDescriptor> {

    public static final String DEFAULT_SCRIPT_LANGUAGE = "JavaScript";

    @XNode("@name")
    private String name;

    @XNode("script")
    private String script;

    @XNode("script@language")
    private String scriptLanguage;

    public String getName() {
        return name;
    }

    public String getScript() {
        return script;
    }

    public String getScriptLanguage() {
        return scriptLanguage == null ? DEFAULT_SCRIPT_LANGUAGE : scriptLanguage;
    }

    // empty constructor
    public DownloadPermissionDescriptor() {
    }

    // copy constructor
    public DownloadPermissionDescriptor(DownloadPermissionDescriptor other) {
        name = other.name;
        script = other.script;
        scriptLanguage = other.scriptLanguage;
    }

    public void merge(DownloadPermissionDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.script != null) {
            script = other.script;
        }
        if (other.scriptLanguage != null) {
            scriptLanguage = other.scriptLanguage;
        }
    }

    @Override
    public int compareTo(DownloadPermissionDescriptor other) {
        return name.compareTo(other.name);
    }

}

/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

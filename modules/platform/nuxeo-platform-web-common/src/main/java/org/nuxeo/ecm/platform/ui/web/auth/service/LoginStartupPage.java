/*
 * (C) Copyright 2016-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.ui.web.auth.service;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 8.4
 */
@XObject("startupPage")
public class LoginStartupPage implements Comparable<LoginStartupPage>, Descriptor {

    /** @since 2025.18 */
    @XNode("@id")
    protected String id;

    @XNode("@priority")
    protected int priority;

    @XNode("path")
    protected String path;

    @Override
    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public LoginStartupPage merge(Descriptor o) {
        var other = (LoginStartupPage) o;
        var merged = new LoginStartupPage();
        merged.id = getIfNull(other.id, id);
        merged.path = defaultIfEmpty(other.path, path);
        // Keep the highest priority
        merged.priority = other.compareTo(this) > 0 ? other.priority : priority;
        return merged;
    }

    @Override
    public int compareTo(LoginStartupPage o) {
        if (o == null || priority > o.getPriority()) {
            return 1;
        }
        if (priority < o.getPriority()) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LoginStartupPage && path != null) {
            return path.equals(((LoginStartupPage) obj).getPath())
                    && priority == ((LoginStartupPage) obj).getPriority();
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return path + "," + priority;
    }
}

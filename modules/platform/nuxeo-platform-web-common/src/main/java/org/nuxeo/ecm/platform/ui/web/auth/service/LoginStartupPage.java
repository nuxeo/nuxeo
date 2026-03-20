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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 8.4
 */
@XObject("startupPage")
public class LoginStartupPage implements Comparable<LoginStartupPage> {

    @XNode("@priority")
    protected int priority;

    @XNode("path")
    protected String path;

    public String getPath() {
        return path;
    }

    public int getPriority() {
        return priority;
    }

    public LoginStartupPage merge(LoginStartupPage newStartupPage) {
        var merged = new LoginStartupPage();
        merged.path = StringUtils.defaultIfEmpty(newStartupPage.path, path);
        // Keep the highest priority
        merged.priority = newStartupPage.compareTo(this) > 0 ? newStartupPage.priority : priority;
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

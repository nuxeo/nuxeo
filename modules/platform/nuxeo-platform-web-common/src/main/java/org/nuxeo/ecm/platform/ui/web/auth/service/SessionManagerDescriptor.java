/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.ui.web.auth.service;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;
import org.nuxeo.runtime.model.Descriptor;

@XObject("sessionManager")
public class SessionManagerDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@class")
    protected Class<NuxeoAuthenticationSessionManager> className;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    /**
     * @deprecated since 2025.0, use {@link #isEnabled()} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public boolean getEnabled() {
        return isEnabled();
    }

    public boolean isEnabled() {
        return toBooleanDefaultIfNull(enabled, true);
    }

    public Class<NuxeoAuthenticationSessionManager> getClassName() {
        return className;
    }

    @Override
    public SessionManagerDescriptor merge(Descriptor o) {
        var other = (SessionManagerDescriptor) o;
        var merged = new SessionManagerDescriptor();
        merged.name = name; // we merge based on name, so no need for merging it
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.className = getIfNull(other.className, className);
        return merged;
    }
}

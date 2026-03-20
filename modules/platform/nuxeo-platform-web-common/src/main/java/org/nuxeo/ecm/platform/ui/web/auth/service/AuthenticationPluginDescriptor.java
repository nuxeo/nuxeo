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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.runtime.model.Descriptor;

@XObject("authenticationPlugin")
public class AuthenticationPluginDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@class")
    protected Class<NuxeoAuthenticationPlugin> className;

    protected Boolean needStartingURLSaving;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<>();

    protected Boolean stateful;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    /**
     * @deprecated since 2025.0, use {@link #isEnabled()} ()} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public boolean getEnabled() {
        return isEnabled();
    }

    public boolean isEnabled() {
        return toBooleanDefaultIfNull(enabled, true);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Class<NuxeoAuthenticationPlugin> getClassName() {
        return className;
    }

    public void setClassName(Class<NuxeoAuthenticationPlugin> className) {
        this.className = className;
    }

    /**
     * @deprecated since 2025.0, use {@link #isNeedStartingURLSaving()} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public boolean getNeedStartingURLSaving() {
        return isNeedStartingURLSaving();
    }

    public boolean isNeedStartingURLSaving() {
        return toBooleanDefaultIfNull(needStartingURLSaving, false);
    }

    @XNode("needStartingURLSaving")
    public void setNeedStartingURLSaving(boolean needStartingURLSaving) {
        this.needStartingURLSaving = Boolean.valueOf(needStartingURLSaving);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * @deprecated since 2025.0, use {@link #isStateful()} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public boolean getStateful() {
        return isStateful();
    }

    public boolean isStateful() {
        return toBooleanDefaultIfNull(stateful, isNeedStartingURLSaving());
    }

    @XNode("stateful")
    public void setStateful(boolean stateful) {
        this.stateful = Boolean.valueOf(stateful);
    }

    @Override
    public AuthenticationPluginDescriptor merge(Descriptor o) {
        var other = (AuthenticationPluginDescriptor) o;
        var merged = new AuthenticationPluginDescriptor();
        merged.name = name; // we merge based on name, so no need for merging it
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.className = getIfNull(other.className, className);
        merged.needStartingURLSaving = getIfNull(other.needStartingURLSaving, needStartingURLSaving);
        merged.parameters = new HashMap<>(parameters);
        merged.parameters.putAll(other.parameters);
        merged.stateful = getIfNull(other.stateful, stateful);
        return merged;
    }
}

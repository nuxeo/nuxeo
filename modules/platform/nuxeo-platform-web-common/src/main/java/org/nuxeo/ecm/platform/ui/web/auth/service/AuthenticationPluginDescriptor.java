/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashMap;
import java.util.Map;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

@XObject("authenticationPlugin")
public class AuthenticationPluginDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    boolean enabled = true;

    @XNode("@class")
    Class<NuxeoAuthenticationPlugin> className;

    private Boolean needStartingURLSaving;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> parameters = new HashMap<>();

    private Boolean stateful;

    public Class<NuxeoAuthenticationPlugin> getClassName() {
        return className;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public boolean getNeedStartingURLSaving() {
        if (needStartingURLSaving != null) {
            return needStartingURLSaving;
        }
        return false;
    }

    public boolean getStateful() {
        if (stateful != null) {
            return stateful;
        }
        return Boolean.valueOf(getNeedStartingURLSaving());
    }

    public void setClassName(Class<NuxeoAuthenticationPlugin> className) {
        this.className = className;
    }

    @XNode("needStartingURLSaving")
    public void setNeedStartingURLSaving(boolean needStartingURLSaving) {
        this.needStartingURLSaving = Boolean.valueOf(needStartingURLSaving);
    }

    @XNode("stateful")
    public void setStateful(boolean stateful) {
        this.stateful = Boolean.valueOf(stateful);
    }

}

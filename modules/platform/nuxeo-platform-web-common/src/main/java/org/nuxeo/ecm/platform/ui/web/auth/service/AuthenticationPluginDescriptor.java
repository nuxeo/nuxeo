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
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

@XObject("authenticationPlugin")
@XRegistry(enable = false)
public class AuthenticationPluginDescriptor {

    @XNode("@name")
    @XRegistryId
    private String name;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    boolean enabled = true;

    @XNode("@class")
    private Class<NuxeoAuthenticationPlugin> className;

    @XNode("needStartingURLSaving")
    private boolean needStartingURLSaving;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> parameters = new HashMap<>();

    @XNode("stateful")
    private boolean stateful;

    public Class<NuxeoAuthenticationPlugin> getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public boolean getNeedStartingURLSaving() {
        return needStartingURLSaving;
    }

    public boolean getStateful() {
        return stateful;
    }

}

/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "authenticationPlugin")
public class AuthenticationPluginDescriptor implements Serializable {

    private static final long serialVersionUID = 237654398643289764L;

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    Boolean enabled = true;

    @XNode("@class")
    Class className;

    @XNode("loginModulePlugin")
    String loginModulePlugin;

    @XNode("needStartingURLSaving")
    Boolean needStartingURLSaving = false;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> parameters = new HashMap<String, String>();

    @XNode("statefull")
     Boolean statefull = true;

    public Class getClassName() {
        return className;
    }

    public void setClassName(Class className) {
        this.className = className;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getLoginModulePlugin() {
        return loginModulePlugin;
    }

    public void setLoginModulePlugin(String loginModulePlugin) {
        this.loginModulePlugin = loginModulePlugin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Boolean getNeedStartingURLSaving() {
        return needStartingURLSaving;
    }

    public void setNeedStartingURLSaving(Boolean needStartingURLSaving) {
        this.needStartingURLSaving = needStartingURLSaving;
    }

    public Boolean getStatefull() {
        return statefull;
    }

    public void setStatefull(Boolean statefull) {
        this.statefull = statefull;
    }
}

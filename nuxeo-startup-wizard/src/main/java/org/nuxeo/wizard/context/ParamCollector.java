/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 * $Id$
 */

package org.nuxeo.wizard.context;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Manages all the parameters entered by User to configure his Nuxeo server
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class ParamCollector {

    public static final String Key = "collector";

    protected Map<String, String> configurationParams = new HashMap<String, String>();

    protected Map<String, String> connectParams = new HashMap<String, String>();

    public void addConfigurationParam(String name, String value) {
            configurationParams.put(name, value);
    }

    public void addConnectParam(String name, String value) {
        configurationParams.put(name, value);
    }

    public Map<String, String> getConfigurationParams() {
        return configurationParams;
    }

    public String getConfigurationParam(String name) {
        String param = configurationParams.get(name);
        if (param==null) {
            param = "";
        }
        return param;
    }

    public String getConfigurationParamValue(String name) {
        return configurationParams.get(name);
    }

    public Map<String, String> getConnectParams() {
        return connectParams;
    }

    public void collectConfigurationParams(HttpServletRequest req) {
        Enumeration<String> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith("org.nuxeo.") || name.startsWith("nuxeo.") || name.startsWith("mail.")) {
                String value = req.getParameter(name);
                if (!value.isEmpty() || (value.isEmpty() && configurationParams.containsKey(name))) {
                    addConfigurationParam(name, value);
                }
            }
        }
    }
}

/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat, jcarsique
 *
 * $Id$
 */

package org.nuxeo.wizard.context;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * Manages all the parameters entered by User to configure his Nuxeo server
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class ParamCollector {

    public static final String Key = "collector";

    private ConfigurationGenerator configurationGenerator;

    public ConfigurationGenerator getConfigurationGenerator() {
        return configurationGenerator;
    }

    protected Map<String, String> configurationParams = new HashMap<String, String>();

    protected Map<String, String> connectParams = new HashMap<String, String>();

    public ParamCollector() {
        configurationGenerator = new ConfigurationGenerator();
        configurationGenerator.init();
        addConfigurationParam("nuxeo.db.template",
                configurationGenerator.extractDatabaseTemplateName());
    }

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
        if (param == null) {
            param = configurationGenerator.getUserConfig().getProperty(name, "");
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
        @SuppressWarnings("unchecked")
        Enumeration<String> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith("org.nuxeo.") || name.startsWith("nuxeo.")
                    || name.startsWith("mail.")) {
                String value = req.getParameter(name);
                if (!value.isEmpty()
                        || (value.isEmpty() && configurationParams.containsKey(name))) {
                    addConfigurationParam(name, value);
                }
            }
        }
    }
}

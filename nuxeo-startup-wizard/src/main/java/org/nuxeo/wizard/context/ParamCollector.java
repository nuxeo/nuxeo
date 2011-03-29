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
 */

package org.nuxeo.wizard.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * Manages all the parameters entered by User to configure his Nuxeo server
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.1
 */
public class ParamCollector {
    private static final Log log = LogFactory.getLog(ParamCollector.class);

    public static final String Key = "collector";

    private ConfigurationGenerator configurationGenerator;

    public ConfigurationGenerator getConfigurationGenerator() {
        return configurationGenerator;
    }

    protected Map<String, String> configurationParams = new HashMap<String, String>();

    protected Map<String, String> connectParams = new HashMap<String, String>();

    private String distributionName = "dm";

    private String logo = null;

    public String getDistributionName() {
        return distributionName;
    }

    public ParamCollector() {
        configurationGenerator = new ConfigurationGenerator();
        configurationGenerator.init();
        try {
            Properties distribution = new Properties();
            distribution.load(new FileInputStream(new File(
                    configurationGenerator.getConfigDir(),
                    "distribution.properties")));
            distributionName = distribution.getProperty(
                    "org.nuxeo.distribution.name", "dm").toLowerCase();
        } catch (FileNotFoundException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
    }

    public void addConfigurationParam(String name, String value) {
        if (value == null) {
            configurationParams.remove(name);
        } else {
            configurationParams.put(name, value);
        }
    }

    public void addConnectParam(String name, String value) {
        configurationParams.put(name, value);
    }

    public Map<String, String> getConfigurationParams() {
        return configurationParams;
    }

    public Map<String, String> getChangedParameters() {
        return configurationGenerator.getChangedParameters(configurationParams);
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

    /**
     * @see ConfigurationGenerator#changeDBTemplate(String)
     * @param templateName database template to use
     */
    public void changeDBTemplate(String templateName) {
        List<String> keys = new ArrayList<String>();
        configurationGenerator.changeDBTemplate(templateName);
        for (String key : configurationParams.keySet()) {
            if (key.startsWith("nuxeo.db")) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            configurationParams.remove(key);
        }
    }

    public String getLogo() {
        if (logo == null) {
            if ("dm".equalsIgnoreCase(distributionName)) {
                logo = "/images/logo_dm_72dpi_white.png";
            } else if ("cap".equalsIgnoreCase(distributionName)) {
                logo = "/images/logo_ep_72dpi_white.png";
            } else if ("dam".equalsIgnoreCase(distributionName)) {
                logo = "/images/logo_dam_72dpi_white.png";
            } else if ("cmf".equalsIgnoreCase(distributionName)) {
                logo = "/images/logo_cmf_72dpi_white.png";
            } else {
                logo = "/images/logo_ep_72dpi_white.png";
            }
        }
        return logo;
    }
}

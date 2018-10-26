/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat, jcarsique
 *
 */

package org.nuxeo.wizard.context;

import static org.nuxeo.launcher.config.ConfigurationGenerator.NUXEO_DEV_SYSTEM_PROP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * Manages all the parameters entered by User to configure his Nuxeo server
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class ParamCollector {

    public static final String Key = "collector";

    private static final String SKIP_SECTIONS_KEY = "nuxeo.wizard.skippedsections";

    private List<String> sectionsToSkip;

    private ConfigurationGenerator configurationGenerator;

    public ConfigurationGenerator getConfigurationGenerator() {
        return configurationGenerator;
    }

    protected Map<String, String> configurationParams = new HashMap<>();

    protected Map<String, String> connectParams = new HashMap<>();

    public ParamCollector() {
        configurationGenerator = new ConfigurationGenerator();
        configurationGenerator.init();
        String skipSections = configurationGenerator.getUserConfig().getProperty(SKIP_SECTIONS_KEY, "");
        sectionsToSkip = Arrays.asList(skipSections.split(","));
    }

    public boolean isSectionSkipped(String section) {
        return sectionsToSkip.contains(section);
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
        Map<String, String> changedParameters = configurationGenerator.getChangedParameters(configurationParams);
        // Don't consider "org.nuxeo.dev" as changed if unset in the config and unchecked in the form
        if (configurationGenerator.getUserConfig().get(NUXEO_DEV_SYSTEM_PROP) == null
                && !Boolean.parseBoolean(changedParameters.get(NUXEO_DEV_SYSTEM_PROP))) {
            changedParameters.remove(NUXEO_DEV_SYSTEM_PROP);
        }
        return changedParameters;
    }

    public String getConfigurationParam(String name) {
        return getConfigurationParam(name, "");
    }

    public String getConfigurationParam(String name, String defaultValue) {
        String param = configurationParams.get(name);
        if (param == null) {
            param = configurationGenerator.getUserConfig().getProperty(name, defaultValue);
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
                    addConfigurationParam(name, StringEscapeUtils.escapeHtml4(value));
                }
            }
        }
    }

    /**
     * @see ConfigurationGenerator#changeDBTemplate(String)
     * @param templateName database template to use
     */
    public void changeDBTemplate(String templateName) {
        configurationGenerator.changeDBTemplate(templateName);
    }

    /**
     * @since 8.1
     */
    public void removeDbKeys() {
        List<String> keys = new ArrayList<>();
        for (String key : configurationParams.keySet()) {
            if (key.startsWith("nuxeo.db") || key.startsWith("nuxeo.mongodb")) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            configurationParams.remove(key);
        }
    }

}

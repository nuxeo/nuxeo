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
 *     tdelprat
 *
 */

package org.nuxeo.wizard.context;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.common.Environment;
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * Simple Context management
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class Context {

    public static final String CONTEXT_ATTRIBUTE = "context";

    protected static ParamCollector collector;

    protected static String baseUrl;

    protected static Boolean browserInternetAccess;

    protected Map<String, String> errors = new HashMap<>();

    protected Map<String, String> infos = new HashMap<>();

    protected static Map<String, String> connectMap;

    protected static String distributionKey = null;

    protected HttpServletRequest req;

    protected Context(HttpServletRequest req) {
        this.req = req;
    }

    public static Context instance(HttpServletRequest req) {
        Context ctx = (Context) req.getAttribute(CONTEXT_ATTRIBUTE);
        if (ctx == null) {
            ctx = new Context(req);
            req.setAttribute(CONTEXT_ATTRIBUTE, ctx);
        }
        return ctx;
    }

    public static void reset() {
        collector = null;
        connectMap = null;
    }

    public ParamCollector getCollector() {
        if (collector == null) {
            collector = new ParamCollector();
        }
        return collector;
    }

    public String getDistributionKey() {
        if (distributionKey == null) {
            ConfigurationGenerator configurationGenerator = new ConfigurationGenerator();
            configurationGenerator.init();
            try {
                Properties distribution = new Properties();
                distribution.load(new FileInputStream(new File(configurationGenerator.getConfigDir(),
                        "distribution.properties")));
                String name = distribution.getProperty(Environment.DISTRIBUTION_NAME, "unknown").toLowerCase();
                String server = distribution.getProperty(Environment.DISTRIBUTION_SERVER, "unknown").toLowerCase();
                String version = distribution.getProperty(Environment.DISTRIBUTION_VERSION, "unknown").toLowerCase();
                String pkg = distribution.getProperty(Environment.DISTRIBUTION_PACKAGE, "unknown").toLowerCase();
                distributionKey = name + "-" + server + "-" + version + "-" + pkg;
            } catch (Exception e) {
                distributionKey = "unknown";
            }
        }
        return distributionKey;
    }

    public void trackError(String fieldId, String message) {
        errors.put(fieldId, message);
    }

    public boolean hasErrors() {
        return errors.size() > 0;
    }

    public void trackInfo(String fieldId, String message) {
        infos.put(fieldId, message);
    }

    public boolean hasInfos() {
        return infos.size() > 0;
    }

    public Map<String, String> getErrorsMap() {
        return errors;
    }

    public Map<String, String> getInfosMap() {
        return infos;
    }

    public String getFieldsInErrorAsJson() {
        StringBuffer sb = new StringBuffer("[");
        for (String key : errors.keySet()) {
            sb.append("'");
            sb.append(key);
            sb.append("',");
        }
        sb.append("END]");
        return sb.toString().replace(",END", "");
    }

    public void storeConnectMap(Map<String, String> map) {
        connectMap = map;
    }

    public boolean isConnectRegistrationDone() {
        return connectMap != null && "true".equals(connectMap.get("registrationOK"));
    }

    public static Map<String, String> getConnectMap() {
        return connectMap;
    }

    public void setBaseUrl(String base) {
        baseUrl = base;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isBrowserInternetAccessChecked() {
        if (browserInternetAccess == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean hasBrowserInternetAccess() {
        if (browserInternetAccess == null) {
            return false;
        }
        return browserInternetAccess;
    }

    public void setBrowserInternetAccess(boolean browserInternetAccess) {
        Context.browserInternetAccess = browserInternetAccess;
    }

}

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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.common;

import java.io.File;
import java.util.Properties;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Environment {
    // the home directory
    public static final String HOME_DIR = "org.nuxeo.app.home";
    // the web root
    public static final String WEB_DIR = "org.nuxeo.app.web";
    // the config dir
    public static final String CONFIG_DIR = "org.nuxeo.app.config";
    // the data dir
    public static final String DATA_DIR = "org.nuxeo.app.data";
    // the log dir
    public static final String LOG_DIR = "org.nuxeo.app.log";


    // the application layout (optional)
    // directory containing nuxeo runtime osgi bundles
    public static final String BUNDLES_DIR = "nuxeo.osgi.app.bundles";

    public static final String BUNDLES = "nuxeo.osgi.bundles";

    private static Environment DEFAULT;

    public static void setDefault(Environment env) {
        Environment.DEFAULT = env;
    }

    public static Environment  getDefault() {
        return Environment.DEFAULT;
    }

    protected File home;
    protected File data;
    protected File log;
    protected File config;
    protected File web;
    protected File temp;
    protected Properties properties;
    protected String[] args;

    public Environment(File home) {
        this (home, null);
    }

    public Environment(File home, Properties properties) {
        this.home = home;
        this.properties = new Properties();
        if (properties != null) {
            loadProperties(properties);
        }
        this.properties.put(Environment.HOME_DIR, this.home.getAbsolutePath());
    }

    /**
     * @return the home.
     */
    public File getHome() {
        return home;
    }


    public File getTemp() {
        if (temp == null) {
            temp = new File(home, "tmp");
        }
        return temp;
    }

    /**
     * @param temp the temp to set.
     */
    public void setTemp(File temp) {
        this.temp = temp;
    }

    /**
     * @return the config.
     */
    public File getConfig() {
        if (config == null) {
            config = new File(home, "config");
        }
        return config;
    }

    /**
     * @param config the config to set.
     */
    public void setConfig(File config) {
        this.config = config;
    }

    /**
     * @return the log.
     */
    public File getLog() {
        if (log == null) {
            log = new File(home, "log");
        }
        return log;
    }

    /**
     * @param log the log to set.
     */
    public void setLog(File log) {
        this.log = log;
    }

    /**
     * @return the data.
     */
    public File getData() {
        if (data == null) {
            data = new File(home, "data");
        }
        return data;
    }

    /**
     * @param data the data to set.
     */
    public void setData(File data) {
        this.data = data;
    }

    /**
     * @return the web.
     */
    public File getWeb() {
        if (web == null) {
            web = new File(home, "web");
        }
        return web;
    }

    /**
     * @param web the web to set.
     */
    public void setWeb(File web) {
        this.web = web;
    }

    public String[] getCommandLineArguments() {
        return args;
    }

    public void setCommandLineArguments(String[] args) {
        this.args = args;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        String val =  properties.getProperty(key);
        return val == null ? defaultValue : val;
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * @return the properties.
     */
    public Properties getProperties() {
        return properties;
    }

    public void loadProperties(Properties properties) {
        this.properties.putAll(properties);
        this.properties.put(HOME_DIR, home.getAbsolutePath());
    }

}

/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.launcher.config.backingservices;

import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_DRIVER;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_HOST;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_JDBC_URL;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_NAME;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_PORT;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_PWD;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_DB_USER;
import static org.nuxeo.launcher.config.ConfigurationGenerator.PARAM_TEMPLATE_DBNAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.launcher.commons.DatabaseDriverException;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * @since 9.2
 */
public class DBCheck implements BackingChecker {

    private static final Log log = LogFactory.getLog(DBCheck.class);

    public static final List<String> DB_EXCLUDE_CHECK_LIST = Arrays.asList("default", "none");

    @Override
    public boolean accepts(ConfigurationGenerator cg) {
        return !DB_EXCLUDE_CHECK_LIST.contains(
                cg.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBTYPE));

    }

    @Override
    public void check(ConfigurationGenerator cg) throws ConfigurationException {
        try {
            checkDatabaseConnection(cg);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        } catch (DatabaseDriverException e) {
            log.debug(e, e);
            log.error(e.getMessage());
            throw new ConfigurationException("Could not find database driver: " + e.getMessage());
        } catch (SQLException e) {
            log.debug(e, e);
            log.error(e.getMessage());
            throw new ConfigurationException("Failed to connect on database: " + e.getMessage());
        }
    }

    /**
     * Check driver availability and database connection
     *
     * @throws DatabaseDriverException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws SQLException
     */
    public void checkDatabaseConnection(ConfigurationGenerator cg)
            throws FileNotFoundException, IOException, DatabaseDriverException, SQLException {
        CryptoProperties config = cg.getUserConfig();
        String databaseTemplate = config.getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME);
        String dbName = config.getProperty(ConfigurationGenerator.PARAM_DB_NAME);
        String dbUser = config.getProperty(ConfigurationGenerator.PARAM_DB_USER);
        String dbPassword = config.getProperty(ConfigurationGenerator.PARAM_DB_PWD);
        String dbHost = config.getProperty(ConfigurationGenerator.PARAM_DB_HOST);
        String dbPort = config.getProperty(ConfigurationGenerator.PARAM_DB_PORT);

        File databaseTemplateDir = new File(cg.getNuxeoHome(), ConfigurationGenerator.TEMPLATES + File.separator + databaseTemplate);
        Properties templateProperties = ConfigurationGenerator.loadTrimmedProperties(new File(databaseTemplateDir, ConfigurationGenerator.NUXEO_DEFAULT_CONF));
        String classname, connectionUrl;
        // check if value is set in nuxeo.conf
        if (config.containsKey(PARAM_DB_DRIVER)) {
            classname = (String) config.get(PARAM_DB_DRIVER);
        } else {
            classname = templateProperties.getProperty(PARAM_DB_DRIVER);
        }
        if (config.containsKey(PARAM_DB_JDBC_URL)) {
            connectionUrl = (String) config.get(PARAM_DB_JDBC_URL);
        } else {
            connectionUrl = templateProperties.getProperty(PARAM_DB_JDBC_URL);
        }
        // Load driver class from template or default lib directory
        Driver driver = lookupDriver(cg, databaseTemplate, databaseTemplateDir, classname);
        // Test db connection
        DriverManager.registerDriver(driver);
        Properties ttProps = new Properties(config);
        ttProps.put(PARAM_DB_HOST, dbHost);
        ttProps.put(PARAM_DB_PORT, dbPort);
        ttProps.put(PARAM_DB_NAME, dbName);
        ttProps.put(PARAM_DB_USER, dbUser);
        ttProps.put(PARAM_DB_PWD, dbPassword);
        TextTemplate tt = new TextTemplate(ttProps);
        String url = tt.processText(connectionUrl);
        Properties conProps = new Properties();
        conProps.put("user", dbUser);
        conProps.put("password", dbPassword);
        log.debug("Testing URL " + url + " with " + conProps);
        Connection con = driver.connect(url, conProps);
        con.close();
    }


    /**
     * Build an {@link URLClassLoader} for the given databaseTemplate looking in the templates directory and in the
     * server lib directory, then looks for a driver
     * @param cg
     *
     * @param classname Driver class name, defined by {@link #PARAM_DB_DRIVER}
     * @return Driver driver if found, else an Exception must have been raised.
     * @throws IOException
     * @throws FileNotFoundException
     * @throws DatabaseDriverException If there was an error when trying to instantiate the driver.
     * @since 5.6
     */
    private Driver lookupDriver(ConfigurationGenerator cg, String databaseTemplate, File databaseTemplateDir, String classname)
            throws FileNotFoundException, IOException, DatabaseDriverException {
        File[] files = (File[]) ArrayUtils.addAll( //
                new File(databaseTemplateDir, "lib").listFiles(), //
                cg.getServerConfigurator().getServerLibDir().listFiles());
        List<URL> urlsList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith("jar")) {
                    try {
                        urlsList.add(new URL("jar:file:" + file.getPath() + "!/"));
                        log.debug("Added " + file.getPath());
                    } catch (MalformedURLException e) {
                        log.error(e);
                    }
                }
            }
        }
        URLClassLoader ucl = new URLClassLoader(urlsList.toArray(new URL[0]));
        try {
            return (Driver) Class.forName(classname, true, ucl).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new DatabaseDriverException(e);
        }
    }

}

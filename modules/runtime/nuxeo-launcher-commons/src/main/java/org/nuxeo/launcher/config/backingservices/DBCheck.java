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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.codec.CryptoProperties;
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

        cg.checkDatabaseConnection(databaseTemplate, dbName, dbUser, dbPassword, dbHost, dbPort);
    }

}

/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.scheduler;

import java.sql.Connection;
import java.sql.SQLException;

import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.quartz.utils.ConnectionProvider;

/**
 * Quartz Connection Provider delegating to the Nuxeo datasource framework.
 *
 * @since 7.1
 */
public class NuxeoQuartzConnectionProvider implements ConnectionProvider {

    protected String dataSourceName;

    protected Connection connection;

    /**
     * Called by reflection from StdSchedulerFactory.setBeanProps.
     *
     * @param jndiURL the JNDI URL from the Quartz configuration
     */
    public void setJndiURL(String jndiURL) {
        int i = jndiURL.lastIndexOf('/');
        if (i != -1) {
            dataSourceName = jndiURL.substring(i + 1);
        } else {
            dataSourceName = jndiURL;
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        connection = ConnectionHelper.getConnection(dataSourceName);
        return connection;
    }

    @Override
    public void shutdown() throws SQLException {
        connection.close();
    }

    /**
     * @since 7.10
     */
    @Override
    public void initialize() throws SQLException {
        // do nothing
    }

}

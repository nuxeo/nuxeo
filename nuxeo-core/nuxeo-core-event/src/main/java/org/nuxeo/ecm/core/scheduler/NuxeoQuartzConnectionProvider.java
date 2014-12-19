/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

}

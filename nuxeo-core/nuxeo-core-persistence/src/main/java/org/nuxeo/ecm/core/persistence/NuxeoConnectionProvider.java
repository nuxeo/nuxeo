/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProvider;
import org.nuxeo.runtime.datasource.DataSourceHelper;

/**
 * ConnectionProvider for Hibernate that looks up the datasource
 * from the nuxeo's container.
 *
 * @since 5.7
 */
public class NuxeoConnectionProvider implements ConnectionProvider {

    protected DataSource ds;

    @Override
    public void configure(Properties props) {
        String name = props.getProperty(Environment.DATASOURCE);
        try {
            ds = DataSourceHelper.getDataSource(name);
        } catch (NamingException cause) {
            throw new HibernateException("Cannot lookup datasource by name " + name, cause);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public void close() throws HibernateException {
        ds = null;
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // don't try to close the connection after each statement
        // (not used if connection release mode is auto)
        return false;
    }

}

/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

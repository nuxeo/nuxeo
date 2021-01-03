/*
 * (C) Copyright 2012-2021 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;
import org.nuxeo.runtime.datasource.DataSourceHelper;

/**
 * ConnectionProvider for Hibernate that looks up the datasource
 * from the nuxeo's container.
 *
 * @since 5.7
 */
public class NuxeoConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

    private static final long serialVersionUID = 1L;

    protected DataSource ds; // NOSONAR

    @Override
    public void configure(Map props) {
        Object value = props.get(AvailableSettings.DATASOURCE);
        if (value instanceof DataSource) {
            ds = (DataSource) value;
        } else {
            String name = (String) value;
            try {
                ds = DataSourceHelper.getDataSource(name);
            } catch (NamingException cause) {
                throw new HibernateException("Cannot lookup datasource by name " + name, cause);
            }
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
    public void stop() {
        ds = null;
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // don't try to close the connection after each statement
        // (not used if connection release mode is auto)
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return ConnectionProvider.class.equals(unwrapType) || NuxeoConnectionProvider.class.isAssignableFrom(unwrapType)
                || DataSource.class.isAssignableFrom(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (ConnectionProvider.class.equals(unwrapType) || NuxeoConnectionProvider.class.isAssignableFrom(unwrapType)) {
            return (T) this;
        } else if (DataSource.class.isAssignableFrom(unwrapType)) {
            return (T) ds;
        } else {
            throw new UnknownUnwrapTypeException(unwrapType);
        }
    }

}

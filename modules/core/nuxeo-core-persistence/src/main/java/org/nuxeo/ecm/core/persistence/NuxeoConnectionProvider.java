/*
 * (C) Copyright 2023 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.core.persistence;

import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.nuxeo.runtime.datasource.DataSourceHelper;

/**
 * ConnectionProvider for Hibernate that looks up the datasource from the Nuxeo container.
 *
 * @since 5.7
 */
public class NuxeoConnectionProvider extends DatasourceConnectionProviderImpl {

    private static final long serialVersionUID = 1L;

    @Override
    public void configure(@SuppressWarnings("rawtypes") Map props) {
        String name = (String) props.get(AvailableSettings.DATASOURCE);
        try {
            DataSource ds = DataSourceHelper.getDataSource(name);
            setDataSource(ds);
            super.configure(props);
        } catch (NamingException cause) {
            throw new HibernateException("Cannot lookup datasource by name " + name, cause);
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // don't try to close the connection after each statement
        // (not used if connection release mode is auto)
        return false;
    }

}

/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.runtime.datasource;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * Helper class to look up {@link DataSource}s without having to deal with vendor-specific JNDI prefixes.
 *
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
public class DataSourceHelper {

    private DataSourceHelper() {
    }

    /**
     * Look up a datasource JNDI name given a partial name.
     * <p>
     * For a datasource {@code "jdbc/foo"}, then it's sufficient to pass {@code "foo"} to this method.
     *
     * @return the datasource JNDI name
     */
    public static String getDataSourceJNDIName(String name) {
        return NuxeoContainer.nameOf(relativize(name));
    }

    protected static String relativize(String name) {
        int idx = name.lastIndexOf("/");
        if (idx > 0) {
            name = name.substring(idx + 1);
        }
        return "jdbc/".concat(name);
    }

    /**
     * Look up a datasource given a partial name.
     * <p>
     * For a datasource {@code "jdbc/foo"}, then it's sufficient to pass {@code "foo"} to this method.
     *
     * @param partialName the partial name
     * @return the datasource
     */
    public static DataSource getDataSource(String partialName) throws NamingException {
        return getDataSource(partialName, DataSource.class);
    }

    public static XADataSource getXADataSource(String partialName) throws NamingException {
        return getDataSource(partialName, XADataSource.class);
    }

    public static <T> T getDataSource(String name, Class<T> clazz) throws NamingException {
        PooledDataSourceRegistry pools = Framework.getService(PooledDataSourceRegistry.class);
        if (pools == null) {
            throw new NamingException("runtime datasource no installed");
        }
        T ds = pools.getPool(relativize(name), clazz);
        if (ds == null) {
            return NuxeoContainer.lookup(name, clazz);
        }
        return ds;
    }

}

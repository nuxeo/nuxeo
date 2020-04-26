/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

public class PooledDataSourceRegistry {

    public interface PooledDataSource extends DataSource {
        void dispose();

        Connection getConnection(boolean noSharing) throws SQLException;
    }

    protected final Map<String, PooledDataSource> pools = new ConcurrentHashMap<>();

    protected final PooledDataSourceFactory poolFactory = new PooledDataSourceFactory();

    public <T> T getPool(String name, Class<T> type) {
        return type.cast(pools.get(name));
    }

    public synchronized void registerPooledDataSource(String dsName, Map<String, String> properties) {
        pools.computeIfAbsent(dsName, k -> poolFactory.createPooledDataSource(properties));
    }

    protected void clearPool(String name) {
        PooledDataSource ds = pools.remove(name);
        if (ds != null) {
            ds.dispose();
        }
    }

    public void createAlias(String name, PooledDataSource ds) {
        pools.put(name, ds);
    }

    public void removeAlias(String name) {
        pools.remove(name);
    }

}

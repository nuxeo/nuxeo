/*
 * (C) Copyright 2009-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.datasource;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo component allowing the JNDI registration of datasources by extension point contributions.
 * <p>
 * For now only the internal Nuxeo JNDI server is supported.
 */
public class DataSourceComponent extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(DataSourceComponent.class);

    public static final String DATASOURCES_XP = "datasources";

    public static final String ENV_CTX_NAME = "java:comp/env/";

    protected PooledDataSourceRegistry poolRegistry;

    @Override
    public int getApplicationStartedOrder() {
        return -1000;
    }

    @Override
    public void start(ComponentContext context) {
        poolRegistry = new PooledDataSourceRegistry();
        DataSourceRegistry reg = getExtensionPointRegistry(DATASOURCES_XP);
        reg.getDataSources().forEach(this::bindDataSource);
        reg.getDataSourceLinks().forEach(this::bindDataSourceLink);
    }

    @Override
    public void stop(ComponentContext context) {
        poolRegistry = null;
    }

    protected void bindDataSource(DataSourceDescriptor descr) {
        log.info("Registering datasource: {}", descr::getName);
        poolRegistry.registerPooledDataSource(descr.getName(), descr.getAllProperties());
    }

    protected void bindDataSourceLink(DataSourceLinkDescriptor descr) {
        log.info("Registering DataSourceLink: {}", descr.name);
        DataSource ds;
        try {
            ds = DataSourceHelper.getDataSource(descr.global, DataSource.class);
        } catch (NamingException e) {
            throw new RuntimeServiceException("Cannot find DataSourceLink '" + descr.name + "' in JNDI", e);
        }
        poolRegistry.createAlias(DataSourceHelper.relativize(descr.name), ds);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(PooledDataSourceRegistry.class)) {
            return adapter.cast(poolRegistry);
        }
        return super.getAdapter(adapter);
    }

}

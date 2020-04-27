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

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo component allowing the JNDI registration of datasources by extension
 * point contributions.
 * <p>
 * For now only the internal Nuxeo JNDI server is supported.
 */
public class DataSourceComponent extends DefaultComponent {

    private final Log log = LogFactory.getLog(DataSourceComponent.class);

    static DataSourceComponent instance;

    public static final String DATASOURCES_XP = "datasources";

    public static final String ENV_CTX_NAME = "java:comp/env/";

    protected Map<String, DataSourceDescriptor> datasources = new HashMap<>();

    protected Map<String, DataSourceLinkDescriptor> links = new HashMap<>();

    protected final PooledDataSourceRegistry poolRegistry = new PooledDataSourceRegistry();

    protected boolean started;

    @Override
    public void activate(ComponentContext context) {
        instance = this;
        datasources = new HashMap<>();
        links = new HashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        links = null;
        datasources = null;
        instance = null;
        //TODO should poolRegistry and sorterRegistry be removed?
    }

    @Override
    public void registerContribution(Object contrib, String extensionPoint, ComponentInstance component) {
        if (contrib instanceof DataSourceDescriptor) {
            addDataSource((DataSourceDescriptor) contrib);
        } else if (contrib instanceof DataSourceLinkDescriptor) {
            addDataSourceLink((DataSourceLinkDescriptor) contrib);
        } else {
            log.error("Wrong datasource extension type " + contrib.getClass().getName());
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String extensionPoint, ComponentInstance component) {
        if (contrib instanceof DataSourceDescriptor) {
            removeDataSource((DataSourceDescriptor) contrib);
        } else if (contrib instanceof DataSourceLinkDescriptor) {
            removeDataSourceLink((DataSourceLinkDescriptor) contrib);
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return -1000;
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public void start(ComponentContext context) {
        if (started) {
            return;
        }
        started = true;
        // bind datasources
        for (DataSourceDescriptor datasourceDesc : datasources.values()) {
            bindDataSource(datasourceDesc);
        }
        // bind links
        for (DataSourceLinkDescriptor linkDesc : links.values()) {
            bindDataSourceLink(linkDesc);
        }
    }

    @Override
    public void stop(ComponentContext context) {
        try {
            for (DataSourceLinkDescriptor desc : links.values()) {
                unbindDataSourceLink(desc);
            }
            for (DataSourceDescriptor desc : datasources.values()) {
                unbindDataSource(desc);
            }
        } finally {
            started = false;
        }
    }

    protected void addDataSource(DataSourceDescriptor contrib) {
        datasources.put(contrib.getName(), contrib);
        bindDataSource(contrib);
    }

    protected void removeDataSource(DataSourceDescriptor contrib) {
        unbindDataSource(contrib);
        datasources.remove(contrib.getName());
    }

    protected void bindDataSource(DataSourceDescriptor descr) {
        log.info("Registering datasource: " + descr.getName());
        poolRegistry.registerPooledDataSource(descr.getName(), descr.getAllProperties());
    }

    protected void unbindDataSource(DataSourceDescriptor descr) {
        log.info("Unregistering datasource: " + descr.getName());
        poolRegistry.unregisterPooledDataSource(descr.getName());
    }

    protected void addDataSourceLink(DataSourceLinkDescriptor contrib) {
        links.put(contrib.name, contrib);
        bindDataSourceLink(contrib);
    }

    protected void removeDataSourceLink(DataSourceLinkDescriptor contrib) {
        unbindDataSourceLink(contrib);
        links.remove(contrib.name);
    }

    protected void bindDataSourceLink(DataSourceLinkDescriptor descr) {
        log.info("Registering DataSourceLink: " + descr.name);
        DataSource ds;
        try {
            ds = DataSourceHelper.getDataSource(descr.global, DataSource.class);
        } catch (NamingException e) {
            throw new RuntimeServiceException("Cannot find DataSourceLink '" + descr.name + "' in JNDI", e);
        }
        poolRegistry.createAlias(DataSourceHelper.relativize(descr.name), ds);
    }

    protected void unbindDataSourceLink(DataSourceLinkDescriptor descr) {
        log.info("Unregistering DataSourceLink: " + descr.name);
        poolRegistry.removeAlias(descr.name);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(PooledDataSourceRegistry.class)) {
            return adapter.cast(poolRegistry);
        }
        return super.getAdapter(adapter);
    }

}

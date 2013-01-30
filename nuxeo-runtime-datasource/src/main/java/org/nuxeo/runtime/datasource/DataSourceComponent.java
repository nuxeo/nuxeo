/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.runtime.datasource;

import java.util.HashMap;
import java.util.Map;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.NotContextException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.DataSourceHelper;
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

    public static final String DATASOURCES_XP = "datasources";

    public static final String ENV_CTX_NAME = "java:comp/env/";

    protected final Map<String, DataSourceDescriptor> datasources = new HashMap<String, DataSourceDescriptor>();

    protected final Map<String, DataSourceLinkDescriptor> links = new HashMap<String, DataSourceLinkDescriptor>();

    protected final DataSourceRegistry registry = new DataSourceRegistry();

    protected InitialContext namingContext;

    @Override
    public void registerContribution(Object contrib, String extensionPoint,
            ComponentInstance component) throws Exception {
        if (DATASOURCES_XP.equals(extensionPoint)) {
        	if (contrib instanceof DataSourceDescriptor) {
        		addDataSource((DataSourceDescriptor) contrib);
        	} else if (contrib instanceof DataSourceLinkDescriptor) {
        		addDataSourceLink((DataSourceLinkDescriptor) contrib);
        	} else {
        		log.error("Wrong datasource extension type " + contrib.getClass().getName());
        	}
        } else {
            log.error("Ignoring unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String extensionPoint,
            ComponentInstance component) throws Exception {
        if (DATASOURCES_XP.equals(extensionPoint)) {
        	if (contrib instanceof DataSourceDescriptor) {
        		removeDataSource((DataSourceDescriptor) contrib);
        	} else if (contrib instanceof DataSourceLinkDescriptor) {
        		removeDataSourceLink((DataSourceLinkDescriptor) contrib);
        	}
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return -1000;
    }

    public boolean isStarted() {
        return namingContext != null;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        if (namingContext != null) {
            return;
        }
        namingContext = new InitialContext();
        // allocate datasource sub-contexts
        Name comp = new CompositeName(DataSourceHelper.getDataSourceJNDIPrefix());
        Context ctx = namingContext;
        for (int i = 0; i < comp.size(); i++) {
            try {
                ctx = (Context) ctx.lookup(comp.get(i));
            } catch (NamingException e) {
                ctx = ctx.createSubcontext(comp.get(i));
            }
        }
        // bind datasources
        for (DataSourceDescriptor datasourceDesc : datasources.values()) {
            bindDataSource(datasourceDesc);
        }
        // bind links
        for (DataSourceLinkDescriptor linkDesc:links.values()) {
        	bindDataSourceLink(linkDesc);
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        for (DataSourceLinkDescriptor desc : links.values()) {
            log.warn(desc.name + " datasource link still referenced");
            unbindDataSourceLink(desc);
        }
        links.clear();
        for (DataSourceDescriptor desc : datasources.values()) {
            log.warn(desc.poolName + " datasource still referenced");
            unbindDataSource(desc);
        }
        datasources.clear();
        namingContext = null;
    }

    protected void addDataSource(DataSourceDescriptor contrib) throws NamingException {
        datasources.put(contrib.poolName, contrib);
        bindDataSource(contrib);
    }

    protected void removeDataSource(DataSourceDescriptor contrib) throws NamingException {
        unbindDataSource(contrib);
        datasources.remove(contrib.poolName);
    }

    protected void bindDataSource(DataSourceDescriptor descr) {
        if (namingContext == null) {
            return;
        }
        log.info("Registering datasource: " + descr.poolName);
        try {
            descr.bindSelf(namingContext);
        } catch (NamingException e) {
            log.error("Cannot bind datasource '" + descr.poolName + "' in JNDI", e);
        }
    }

    protected void unbindDataSource(DataSourceDescriptor descr) {
        if (namingContext == null) {
            return;
        }
        log.info("Unregistering datasource: " + descr.poolName);
        try {
            registry.clearDatasource(new CompositeName(descr.poolName));
            descr.unbindSelf(namingContext);
        } catch (NamingException e) {
            log.error("Cannot unbind datasource '" + descr.poolName + "' in JNDI",
                    e);
        }
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
        if (namingContext == null) {
            return;
        }
        log.info("Registering DataSourceLink: " + descr.name);
        try {
            descr.bindSelf(namingContext);
        } catch (NamingException e) {
            log.error("Cannot bind DataSourceLink '" + descr.name + "' in JNDI", e);
        }
    }

    protected void unbindDataSourceLink(DataSourceLinkDescriptor descr) {
        if (namingContext == null) {
            return;
        }
        log.info("Unregistering DataSourceLink: " + descr.name);
        try {
            Context ctx = new InitialContext();
            ctx.unbind(ENV_CTX_NAME + descr.name);
        } catch (NotContextException e) {
            log.warn(e);
        } catch (NoInitialContextException e) {
            ;
        } catch (NamingException e) {
            log.error("Cannot unbind DataSourceLink '" + descr.name + "' in JNDI",
                    e);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(DataSourceRegistry.class)) {
            return adapter.cast(registry);
        }
        return super.getAdapter(adapter);
    }

}

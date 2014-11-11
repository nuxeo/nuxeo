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

import java.lang.reflect.Field;
import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.jndi.InitialContextAccessor;
import org.nuxeo.common.jndi.NamingContextFactory;
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

    private static final Log log = LogFactory.getLog(DataSourceComponent.class);

    public static final String DATASOURCES_XP = "datasources";

    public static final String ENV_CTX_NAME = "java:comp/env";

    public static final String CONTEXT_ACCESS_CONTROLLER_CLASS_NAME = "org.apache.naming.ContextAccessController";

    public static final String READ_ONLY_CONTEXTS_FIELD_NAME = "readOnlyContexts";

    protected boolean isNamingOwner;

    @Override
    public void activate(ComponentContext context) throws Exception {
        clearReadOnlyNamingContexts();

        Context ctx = InitialContextAccessor.getInitialContext();
        if (ctx != null) {
            if (InitialContextAccessor.isWritable(ctx)) {
                return;
            }
            NamingContextFactory.setDelegateContext(ctx);
            NamingContextFactory.setDelegateEnvironment(ctx.getEnvironment());
        }
        NamingContextFactory.setAsInitial();
        isNamingOwner = true;
    }

    /**
     * Clear the Map holding the read only contexts so that we can contribute
     * our own DataSources because Tomcat sets its Naming contexts as read only
     * after initializing them.
     */
    protected void clearReadOnlyNamingContexts() {
        try {
            Class clazz = Class.forName(CONTEXT_ACCESS_CONTROLLER_CLASS_NAME);
            if (clazz == null) {
                return;
            }

            for (Field field : clazz.getDeclaredFields()) {
                if (READ_ONLY_CONTEXTS_FIELD_NAME.equals(field.getName())) {
                    field.setAccessible(true);
                    Hashtable readOnlyContexts = (Hashtable) field.get(clazz);
                    // readOnlyContexts.remove("/Catalina/localhost/nuxeo");
                    readOnlyContexts.clear();
                    return;
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (isNamingOwner) {
            NamingContextFactory.revertSetAsInitial();
            isNamingOwner = false;
        }
    }

    @Override
    public void registerContribution(Object contrib, String extensionPoint,
            ComponentInstance component) throws Exception {
        if (DATASOURCES_XP.equals(extensionPoint)) {
            if (contrib instanceof DataSourceDescriptor) {
                addDataSource((DataSourceDescriptor) contrib);
            } else {
                log.error("Invalid datasource contribution: "
                        + contrib.getClass().getName());
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
            } else {
                log.error("Invalid datasource contribution: "
                        + contrib.getClass().getName());
            }
        } else {
            log.error("Ignoring unknown extension point: " + extensionPoint);
        }
    }

    protected void addDataSource(DataSourceDescriptor descr) {
        log.info("Registering datasource: " + descr.name);
        try {
            Name name = new CompositeName(ENV_CTX_NAME + "/" + descr.name);
            Context ctx = new InitialContext();
            // bind intermediate names as subcontexts (jdbc/foo)
            for (int i = 0; i < name.size() - 1; i++) {
                try {
                    ctx = (Context) ctx.lookup(name.get(i));
                } catch (NamingException e) {
                    ctx = ctx.createSubcontext(name.get(i));
                }
            }
            ctx.bind(name.get(name.size() - 1), descr.getReference());
        } catch (NamingException e) {
            log.error("Cannot bind datasource '" + descr.name + "' in JNDI", e);
        }
    }

    protected void removeDataSource(DataSourceDescriptor descr) {
        log.info("Unregistering datasource: " + descr.name);
        try {
            Context ctx = new InitialContext();
            ctx.unbind(ENV_CTX_NAME + "/" + descr.name);
        } catch (NamingException e) {
            log.error("Cannot unbind datasource '" + descr.name + "' in JNDI",
                    e);
        }
    }

}

/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to look up {@link DataSource}s without having to deal with
 * vendor-specific JNDI prefixes.
 *
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
public class DataSourceHelper {

    private DataSourceHelper() {
    }

    protected static final class TestingInitialContext extends InitialContext {
        protected TestingInitialContext() throws NamingException {
            super(false); // lazy mode is breaking jboss
        }
        // subclass in order to access the protected method
        @Override
        public Context getDefaultInitCtx() throws NamingException {
            return super.getDefaultInitCtx();
        }
    }

    private static final Log log = LogFactory.getLog(DataSourceHelper.class);

    public static final String PREFIX_PROPERTY = "org.nuxeo.runtime.datasource.prefix";

    public static final String DEFAULT_PREFIX = "java:comp/env/jdbc";

    protected static String prefix;

    public static void autodetectPrefix() {
        Context ctx = getDefaultInitCtx();
        String name = ctx == null ? null : ctx.getClass().getName();
        if ("org.nuxeo.common.jndi.NamingContext".equals(name)) { // Nuxeo-Embedded
            prefix = DEFAULT_PREFIX;
        } else if ("org.jnp.interfaces.NamingContext".equals(name)) { // JBoss
            prefix = "java:";
        } else if ("org.apache.naming.SelectorContext".equals(name)) { // Tomcat
            prefix = DEFAULT_PREFIX;
        } else if ("org.mortbay.naming.local.localContextRoot".equals(name)) { // Jetty
            prefix = "jdbc";
        } else if ("com.sun.enterprise.naming.impl.SerialContext".equals(name)) { // GlassFish
            prefix = DEFAULT_PREFIX;
        } else {
            // unknown, use Java EE standard
            log.error("Unknown JNDI Context class: " + name);
            prefix = DEFAULT_PREFIX;
        }
        log.info("Using JDBC JNDI prefix: " + prefix);
    }

    public static Context getDefaultInitCtx() {
        try {
            return new TestingInitialContext().getDefaultInitCtx();
        } catch (NamingException e) {
            return null;
        }
    }

    /**
     * Get the JNDI prefix used for DataSource lookups.
     */
    public static String getDataSourceJNDIPrefix() {
        if (prefix == null) {
            if (Framework.isInitialized()) {
                String configuredPrefix = Framework.getProperty(PREFIX_PROPERTY);
                if (configuredPrefix != null) {
                    prefix = configuredPrefix;
                } else {
                    autodetectPrefix();
                }
            } else {
                prefix = DEFAULT_PREFIX;
            }
        }
        return prefix;
    }

    /**
     * Look up a datasource JNDI name given a partial name.
     * <p>
     * For a datasource {@code "jdbc/foo"}, then it's sufficient to pass {@code
     * "foo"} to this method.
     *
     * @param partialName the partial name
     * @return the datasource JNDI name
     */
    public static String getDataSourceJNDIName(String partialName) {
        String targetPrefix = getDataSourceJNDIPrefix();
        // keep suffix only (jdbc/foo -> foo)
        int idx = partialName.lastIndexOf("/");
        if (idx > 0) {
            partialName = partialName.substring(idx + 1);
        }
        // add prefix
        return targetPrefix + "/" + partialName;
    }

    /**
     * Look up a datasource given a partial name.
     * <p>
     * For a datasource {@code "jdbc/foo"}, then it's sufficient to pass {@code
     * "foo"} to this method.
     *
     * @param partialName the partial name
     * @return the datasource
     * @throws NamingException
     */
    public static DataSource getDataSource(String partialName)
            throws NamingException {
        String jndiName = getDataSourceJNDIName(partialName);
        InitialContext context = new InitialContext();
        Object resolved = context.lookup(jndiName);
        if (resolved instanceof Reference) {
            try {
                resolved = NamingManager.getObjectInstance(resolved,  new CompositeName(jndiName), context, null);
            } catch (Exception e) {
                throw new Error("Cannot get access to " + jndiName, e);
            }
        }
        return (DataSource)resolved;
    }

}

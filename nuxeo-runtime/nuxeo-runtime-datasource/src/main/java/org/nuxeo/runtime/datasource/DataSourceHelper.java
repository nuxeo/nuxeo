/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.runtime.datasource;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.InitialContextAccessor;

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

    private static final Log log = LogFactory.getLog(DataSourceHelper.class);

    public static final String PREFIX_PROPERTY = "org.nuxeo.runtime.datasource.prefix";

    public static final String DEFAULT_PREFIX = "java:comp/env/jdbc";

    protected static String prefix;

    public static void autodetectPrefix() {
        Context ctx = InitialContextAccessor.getInitialContext();
        String name = ctx == null ? null : ctx.getClass().getName();
        if ("org.jnp.interfaces.NamingContext".equals(name)) { // JBoss
            prefix = "java:";
        } else if ("org.mortbay.naming.local.localContextRoot".equals(name)) { // Jetty
            prefix = "jdbc";
        } else {
            // Standard JEE containers (Nuxeo-Embedded, Tomcat, GlassFish, ...
            prefix = DEFAULT_PREFIX;
        }
        log.info("Using JDBC JNDI prefix: " + prefix);
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
     * For a datasource {@code "jdbc/foo"}, then it's sufficient to pass
     * {@code "foo"} to this method.
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
     * For a datasource {@code "jdbc/foo"}, then it's sufficient to pass
     * {@code "foo"} to this method.
     *
     * @param partialName the partial name
     * @return the datasource
     * @throws NamingException
     */
    public static DataSource getDataSource(String partialName)
            throws NamingException {
        return getDataSource(partialName, DataSource.class);
    }

    public static XADataSource getXADataSource(String partialName)
            throws NamingException {
        return getDataSource(partialName, XADataSource.class);
    }

    public static <T> T getDataSource(String partialName, Class<T> clazz)
            throws NamingException {
        String jndiName = getDataSourceJNDIName(partialName);
        InitialContext context = new InitialContext();
        Object resolved = context.lookup(jndiName);
        if (resolved instanceof Reference) {
            try {
                resolved = NamingManager.getObjectInstance(resolved,
                        new CompositeName(jndiName), context, null);
            } catch (Exception e) {
                throw new RuntimeException("Cannot get access to " + jndiName,
                        e);
            }
        }
        return clazz.cast(resolved);
    }

    public static Map<String,DataSource> getDatasources() throws NamingException {
        String prefix = getDataSourceJNDIPrefix();
        Context naming = new InitialContext();
        Context jdbc = (Context)naming.lookup(prefix);
        Enumeration<NameClassPair> namesPair = jdbc.list("");
        Map<String,DataSource> datasourcesByName = new HashMap<String,DataSource>();
        while (namesPair.hasMoreElements()) {
            NameClassPair pair = namesPair.nextElement();
            String name = pair.getName();
            if (pair.isRelative()) {
                name = prefix + "/" + name;
            }
            Object ds = naming.lookup(name);
            if (ds instanceof DataSource){
                datasourcesByName.put(name, (DataSource)ds);
            }
        }
        return datasourcesByName;
    }

    /**
     * @param repositoryName
     * @return
     *
     * @since TODO
     */
    public static String getDataSourceRepositoryJNDIName(
            String repositoryName) {
        return getDataSourceJNDIName(ConnectionHelper.getPseudoDataSourceNameForRepository(repositoryName));
    }
}

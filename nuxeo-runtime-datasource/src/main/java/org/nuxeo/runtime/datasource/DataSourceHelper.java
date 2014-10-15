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

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.nuxeo.runtime.jtajca.NuxeoContainer;

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

    /**
     * Get the JNDI prefix used for DataSource lookups.
     */
    public static String getDataSourceJNDIPrefix() {
        return NuxeoContainer.nameOf("jdbc");
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
    public static String getDataSourceJNDIName(String name) {
        return NuxeoContainer.nameOf("jdbc/".concat(relativize(name)));
    }

    protected static String relativize(String name) {
        int idx = name.lastIndexOf("/");
        if (idx > 0) {
            return name.substring(idx + 1);
        }
        return name;
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

    public static <T> T getDataSource(String name, Class<T> clazz)
            throws NamingException {
        return NuxeoContainer.lookupDataSource(relativize(name), clazz);
    }

    public static Map<String,DataSource> getDatasources() throws NamingException {
        String prefix = getDataSourceJNDIPrefix();
        Context naming =  NuxeoContainer.getRootContext();
        if (naming == null) {
            throw new NamingException("No root context");
        }
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

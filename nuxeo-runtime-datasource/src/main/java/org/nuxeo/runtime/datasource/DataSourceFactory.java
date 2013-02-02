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

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.dbcp.managed.BasicManagedDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JNDI factory for a DataSource that delegates to an Apache DBCP pool.
 * <p>
 * An instance of this class is registered in JNDI for each datasource
 * configured by the {@link DataSourceComponent}.
 */
public class DataSourceFactory implements ObjectFactory {

    private static final Log log = LogFactory.getLog(DataSourceFactory.class);

    private static final String URL_UPPER = "URL";

    private static final String URL_LOWER = "url";

    private static final String[] TOMCAT_PROPERTIES = { "scope", "auth",
            "factory" };

    public static final Set<String> SYSTEM_PROPERTIES;
    static {
        Set<String> props = new HashSet<String>(
                Arrays.asList(BasicManagedDataSourceFactory.ALL_PROPERTIES));
        props.addAll(Arrays.asList(TOMCAT_PROPERTIES));
        SYSTEM_PROPERTIES = Collections.unmodifiableSet(props);
    }

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
            Hashtable<?, ?> env) throws Exception {
        Reference ref = (Reference) obj;
        if (!DataSource.class.getName().equals(ref.getClassName())) {
            return null;
        }

        boolean xa = ref.get(BasicManagedDataSourceFactory.PROP_XADATASOURCE) != null;
        log.info(String.format("Creating pooled %s datasource: %s", xa ? "XA"
                : "non-XA", name));

        // extract implementation-specific properties from Reference
        // skip those used to define standard datasource parameters
        Map<String, String> properties = new HashMap<String, String>();
        Enumeration<RefAddr> refAddrs = ref.getAll();
        while (refAddrs.hasMoreElements()) {
            RefAddr ra = refAddrs.nextElement();
            String key = ra.getType();
            String value = ra.getContent().toString();
            if (!SYSTEM_PROPERTIES.contains(key)) {
                properties.put(key, value);
            }
        }

        DataSource ds;
        if (!xa) {
            // fetch url from properties
            for (Entry<String, String> en : properties.entrySet()) {
                // often misspelled, thus the ignore case
                if (URL_LOWER.equalsIgnoreCase(en.getKey())) {
                    ref.add(new StringRefAddr(URL_LOWER, en.getValue()));
                }
            }
            ObjectFactory factory = new BasicDataSourceFactory();
            ds = (DataSource) factory.getObjectInstance(ref, name, nameCtx, env);
            BasicDataSource bds = (BasicDataSource) ds;

            // set properties
            for (Entry<String, String> en : properties.entrySet()) {
                String key = en.getKey();
                if (URL_LOWER.equalsIgnoreCase(key)) {
                    continue;
                }
                bds.addConnectionProperty(key, en.getValue());
            }
        } else {
            ObjectFactory factory = new BasicManagedDataSourceFactory();
            ds = (DataSource) factory.getObjectInstance(obj, name, nameCtx, env);
            if (ds == null) {
                return null;
            }
            BasicManagedDataSource bmds = (BasicManagedDataSource) ds;

            // set transaction manager
            bmds.setTransactionManager(new LazyTransactionManager());

            // set properties
            XADataSource xaDataSource = bmds.getXaDataSourceInstance();
            if (xaDataSource == null) {
                return null;
            }
            for (Entry<String, String> en : properties.entrySet()) {
                String key = en.getKey();
                // proper JavaBean convention for initial cap
                if (Character.isLowerCase(key.charAt(1))) {
                    key = Character.toLowerCase(key.charAt(0))
                            + key.substring(1);
                }
                String value = en.getValue();
                boolean ok = false;
                try {
                    BeanUtils.setProperty(xaDataSource, key, value);
                    ok = true;
                } catch (Exception e) {
                    if (URL_LOWER.equals(key)) {
                        // commonly misspelled
                        try {
                            BeanUtils.setProperty(xaDataSource, URL_UPPER,
                                    value);
                            ok = true;
                        } catch (Exception ee) {
                            // log error below
                        }
                    }
                }
                if (!ok) {
                    log.error(String.format("Cannot set %s = %s on %s", key,
                            value, xaDataSource.getClass().getName()));
                }
            }
        }
        return ds;
    }
}

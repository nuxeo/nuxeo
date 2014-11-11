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
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.naming.GenericNamingResourcesFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * The descriptor for a Nuxeo-defined datasource.
 * <p>
 * The attributes of a {@code <datasource>} element are:
 * <ul>
 * <li><b>name</b>: the JNDI name (for instance {@code jdbc/foo})</li>
 * <li><b>driverClassName</b>: the JDBC driver class name (only for a non-XA
 * datasource)</li>
 * <li><b>xaDataSource</b>: the XA datasource class name (only for a XA
 * datasource)</li>
 * </ul>
 * <p>
 * To configure the characteristics of the pool:
 * <ul>
 * <li><b>maxActive</b>: the maximum number of active connections</li>
 * <li><b>minIdle</b>: the minimum number of idle connections</li>
 * <li><b>maxIdle</b>: the maximum number of idle connections</li>
 * <li><b>maxWait</b>: the maximum number of milliseconds to wait for a
 * connection to be available, or -1 (the default) to wait indefinitely</li>
 * <li>... see {@link org.apache.commons.dbcp.BasicDataSource BasicDataSource}
 * setters for more</li>
 * </ul>
 * <p>
 * To configure the datasource connections, individual {@code <property>}
 * sub-elements are used.
 * <p>
 * For a non-XA datasource, you must specify at least a <b>url</b>:
 *
 * <pre>
 *   &lt;property name=&quot;url&quot;&gt;jdbc:derby:foo/bar&lt;/property&gt;
 *   &lt;property name=&quot;username&quot;&gt;nuxeo&lt;/property&gt;
 *   &lt;property name=&quot;password&quot;&gt;nuxeo&lt;/property&gt;
 * </pre>
 *
 * For a XA datasource, see the documentation for your JDBC driver.
 */
@XObject("datasource")
public class DataSourceDescriptor {

    /*
     * It is not possible to expand the variables in the setters because in
     * tests, values are not available in context. A clean up needs to be done
     * to have the values during startup.
     */

    @XNode("@name")
    protected String name;

    public String getName() {
        return Framework.expandVars(name);
    }

    @XNode("@xaDataSource")
    protected String xaDataSource;


    public String getXaDataSource() {
        return Framework.expandVars(xaDataSource);
    }

    @XNode("@dataSource")
    protected String dataSource;


    public String getDataSource() {
        return Framework.expandVars(dataSource);
    }

    @XNode("@driverClassName")
    protected String driverClasssName;

    public String getDriverClasssName() {
        return Framework.expandVars(driverClasssName);
    }

    @XNode("")
    public Element element;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties;

    protected Reference poolReference;

    protected Reference xaReference;

    public static class PoolFactory
            implements ObjectFactory {

        @Override
        public Object getObjectInstance(
                Object obj, Name name,
                Context nameCtx,
                Hashtable<?, ?> env)
                throws Exception {
            return Framework.getLocalService(
                    PooledDataSourceRegistry.class).getOrCreatePool(
                    obj, name, nameCtx,
                    env);
        }

    }

    public void bindSelf(
            Context naming)
            throws NamingException {
        if (xaDataSource != null) {
            String xaName = DataSourceHelper.getDataSourceJNDIName(getName()
                    + "-xa");
            poolReference = new Reference(
                    XADataSource.class.getName(),
                    PoolFactory.class.getName(),
                    null);
            poolReference.add(new StringRefAddr(
                    "dataSourceJNDI",
                    xaName));
            xaReference = new Reference(
                    Framework.expandVars(xaDataSource),
                    GenericNamingResourcesFactory.class.getName(),
                    null);
            for (Entry<String, String> e : properties.entrySet()) {
                String key = e.getKey();
                String value = Framework.expandVars(e.getValue());
                StringRefAddr addr = new StringRefAddr(
                        key, value);
                xaReference.add(addr);
            }
            naming.bind(
                    DataSourceHelper.getDataSourceJNDIName(xaName),
                    xaReference);
        } else if (dataSource != null) {
            poolReference = new Reference(
                    DataSource.class.getName(),
                    PoolFactory.class.getName(),
                    null);
            final String name = Framework.expandVars(dataSource);
            poolReference.add(new StringRefAddr("dataSourceJNDI",
                    DataSourceHelper.getDataSourceJNDIName(name)));
        } else if (driverClasssName != null) {
            poolReference = new Reference(
                    DataSource.class.getName(),
                    PoolFactory.class.getName(),
                    null);
        } else {
            throw new RuntimeException(
                    "Datasource "
                            + getName()
                            + " should have xaDataSource or driverClassName attribute");
        }

        for (Entry<String, String> e : properties.entrySet()) {
            String key = e.getKey();
            String value = Framework.expandVars(e.getValue());
            StringRefAddr addr = new StringRefAddr(
                    key, value);
            poolReference.add(addr);
        }

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String attrName = attr.getNodeName();
            String value = Framework.expandVars(attr.getNodeValue());
            StringRefAddr addr = new StringRefAddr(
                    attrName, value);
            poolReference.add(addr);
        }

        LogFactory.getLog(DataSourceDescriptor.class).info("binding " + getName());
        naming.bind(
                DataSourceHelper.getDataSourceJNDIName(getName()),
                poolReference);
    }

    public void unbindSelf(
            Context naming)
            throws NamingException {
        try {
            final PooledDataSourceRegistry registry = Framework.getLocalService(PooledDataSourceRegistry.class);
            if (registry != null) {
                registry.clearPool(getName());
            }
        } catch (Exception cause) {
            NamingException error = new NamingException(
                    "Cannot clear pooled datasource "
                            + getName());
            error.initCause(cause);
            throw error;
        } finally {
            try {
                if (xaReference != null) {
                    naming.unbind(DataSourceHelper.getDataSourceJNDIName(getName()
                            + "-xa"));
                }
            } finally {
                naming.unbind(DataSourceHelper.getDataSourceJNDIName(getName()));
            }
        }
    }

}

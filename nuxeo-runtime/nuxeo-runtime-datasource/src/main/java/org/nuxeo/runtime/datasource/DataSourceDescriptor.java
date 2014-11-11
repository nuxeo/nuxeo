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
import java.util.Map.Entry;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;

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

    @XNode("@name")
    public String name;

    @XNode("")
    public Element element;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties;

    /** How we store XA props in the Reference. */
    public static final String PROP_PREFIX = "property.";

    protected Reference reference;

    public Reference getReference() {
        if (reference == null) {
            reference = new Reference(DataSource.class.getName(),
                    DataSourceFactory.class.getName(), null);
            NamedNodeMap attrs = element.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                String name = attr.getNodeName();
                if ("name".equals(name)) {
                    continue;
                }
                String value = Framework.expandVars(attr.getNodeValue());
                reference.add(new StringRefAddr(name, value));
            }
            for (Entry<String, String> e : properties.entrySet()) {
                String name = e.getKey();
                String value = Framework.expandVars(e.getValue());
                reference.add(new StringRefAddr(PROP_PREFIX + name, value));
            }
        }
        return reference;
    }

}

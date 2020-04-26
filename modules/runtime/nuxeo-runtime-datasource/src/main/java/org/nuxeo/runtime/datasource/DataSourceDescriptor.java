/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *   &lt;property name=&quot;url&quot;&gt;jdbc:h2:foo/bar&lt;/property&gt;
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

    // collect explicit properties and all descriptor attributes
    public Map<String, String> getAllProperties() {
        Map<String, String> props = new HashMap<>();
        properties.forEach((k, v) -> props.put(k, Framework.expandVars(v)));
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            props.put(attr.getNodeName(), Framework.expandVars(attr.getNodeValue()));
        }
        return props;
    }

}

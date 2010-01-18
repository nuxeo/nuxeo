/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.remoting;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.AppConfigurationEntry;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.login.SecurityDomain;
import org.nuxeo.runtime.config.ConfigurationException;
import org.nuxeo.runtime.config.ServerConfiguration;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Server {

    ServerConfiguration  getConfiguration(InvokerLocator locator, Version version) throws ConfigurationException;

    String getName();

    String getDescription();

    ComponentName[] getComponents();

    boolean hasComponent(ComponentName name);

    Collection<ComponentInstance> getActiveComponents();

    Collection<RegistrationInfo> getRegistrations();

    String getServerAddress();

    ComponentInstance getComponent(String name);

    ComponentInstance getComponent(ComponentName name);

    /**
     * Gets the product info as a string of the form:
     * <code>ProductName ProductVersion</code>.
     *
     * @return the product info
     */
    String getProductInfo();

    /**
     * Returns the service binding configuration as a string array containing the
     * following informations:
     *
     * <pre>
     * [group, serviceInterface, name, locator], [group, serviceInterface, name, locator], ...
     * </pre>
     *
     * So each service binding takes 4 consecutive entries in the array.
     *
     * @return the service bindings
     */
    String[] getServiceBindings();

    /**
     * Returns the service locators as an array of service locator properties.
     * <p>
     * There are 4 special properties that can be used:
     * <li><code>@class</code> - the service locator class name (a String)
     * <li><code>@host</code> - the server host (a String)
     * <li><code>@port</code> - the server port (an Integer)
     * <li><code>@groups</code> - the server service groups (a String array)
     *
     * @return an array containing the service locators descriptor properties
     */
    Properties[] getServiceHosts() throws Exception;

    /**
     * Gets the login configuration of this server. The login configuration is a
     * map of SecurityDomain. A security domains is defined by a name and an
     * array of {@link AppConfigurationEntry} objects
     * <p>
     * Since these objects are not serializable they will be represented as an
     * <code>Object[]</code> array of 3 elements:
     * <ol>
     * <li> the login module name stored as a <code>String</code>
     * <li> the login module control flag stored as a <code>String</code>
     * <li> the login module options stored as a <code>Map</code>
     * </ol>
     *
     * So the entry value in the returned map is an <code>Object[3][]</code>
     * array of 3 length object arrays describing the
     * {@link AppConfigurationEntry}.
     *
     * @return a map describing the security domains available on the server
     *
     * @see AppConfigurationEntry
     * @see SecurityDomain
     *
     * @throws Exception
     */
    Map<String, Object[][]> getSecurityDomains() throws Exception;

    /**
     * Gets the runtime properties as a Java properties file content.
     */
    Properties getProperties();

    // ---- remote resource loading ---------

    byte[] getClass(ComponentName component, String name);

    byte[] getResource(ComponentName component, String name);

    byte[] getLocalResource(ComponentName component, String name);

}

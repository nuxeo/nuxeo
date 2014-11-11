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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.config;

import java.util.Map;
import java.util.Properties;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.SecurityDomain;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractServerConfiguration implements ServerConfiguration {

    private static final long serialVersionUID = 1970555604877434479L;

    protected final String name;

    protected final Version version;

    protected final InvokerLocator locator;

    protected Properties properties;

    protected Properties jndiProperties;

    protected SecurityDomain[] securityDomains;

    protected Map<String, String> serviceBindings;

    protected String streamingLocator;

    protected String[] peers;

    //TODO
    //protected ServiceLocator serviceLocator;

    protected AbstractServerConfiguration(InvokerLocator locator, String name, Version version) {
        this.locator = locator;
        this.name = name;
        this.version = version;
    }

    @Override
    public InvokerLocator getLocator() {
        return locator;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public String getProductInfo() {
        String name = Framework.getRuntime().getProperty("org.nuxeo.ecm.product.name");
        String version = Framework.getRuntime().getProperty("org.nuxeo.ecm.product.version");
        if (name == null) {
            name = "Nuxeo Runtime Server";
            version = Framework.getRuntime().getVersion().toString();
        } else if (version == null) {
            version = "0.0.0";
        }
        return name + ' ' + version;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Map<String, String> getServiceBindings() {
        return serviceBindings;
    }

    public void setServiceBindings(Map<String, String> bindings) {
        serviceBindings = bindings;
    }

    public SecurityDomain[] getSecurityDomains() {
        return securityDomains;
    }

    public void setSecurityDomains(SecurityDomain[] domains) {
        securityDomains = domains;
    }

    public String getStreamingLocator() {
        return streamingLocator;
    }

    public void setStreamingLocator(String locator) {
        streamingLocator = locator;
    }

    @Override
    public String[] getPeers() {
        return peers;
    }

    public void setPeers(String[] peers) {
        this.peers = peers;
    }

    @Override
    public Properties getJndiProperties() {
        if (jndiProperties == null) {
            jndiProperties = AutoConfigurationService.readJndiProperties(properties);
        }
        return jndiProperties;
    }

    public void setJndiProperties(Properties jndiProperties) {
        this.jndiProperties = jndiProperties;
    }

// TODO
//    public ServiceLocator getServiceLocator() {
//        return this.serviceLocator;
//    }
//
//    public void setServiceLocator(ServiceLocator serviceLocator) {
//        this.serviceLocator = serviceLocator;
//    }

    @Override
    public abstract void install() throws Exception;

}

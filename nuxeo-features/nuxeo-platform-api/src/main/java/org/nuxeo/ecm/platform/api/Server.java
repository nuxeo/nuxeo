/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceManagement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated use new service API {@link ServiceManagement}
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class Server {

    private static final String jndiPrefix = "nuxeo/";
    private static final String jndiSuffix = "/remote";

    private final Platform platform;

    private final String name;
    private final String host;
    private final String port;

    private final Map<String, ServiceDescriptor> services;
    private final Map<String, RepositoryDescriptor> repositories;

    private final JndiContextFactory jndiContextFactory;
    private InitialContext jndiContext;
    private RepositoryConnector repositoryConnector;
    private ServiceConnector serviceConnector;


    public Server(Platform platform, ServerDescriptor descriptor) throws Exception {
        name = descriptor.name;
        this.platform = platform;
        services = new Hashtable<String, ServiceDescriptor>();
        repositories = new Hashtable<String, RepositoryDescriptor>();
        host = Framework.expandVars(descriptor.host);
        port = descriptor.port == null ? "1099" : descriptor.port;
        if (descriptor.jndiContextFactory == null) {
            jndiContextFactory =  DefaultJndiContextFactory.getInstance();
        } else {
            jndiContextFactory = (JndiContextFactory) descriptor.jndiContextFactory.newInstance();
        }
        jndiContext = jndiContextFactory.createJndiContext(host, port);
        if (descriptor.serviceConnector == null) {
            serviceConnector = DefaultServiceConnector.getInstance();
        } else {
            serviceConnector = (ServiceConnector) descriptor.serviceConnector.newInstance();
        }
        if (descriptor.repositoryConnector == null) {
            repositoryConnector = DefaultRepositoryConnector.getInstance();
        } else {
            repositoryConnector = (RepositoryConnector) descriptor.repositoryConnector.newInstance();
        }
        if (descriptor.services != null) {
            registerServices(descriptor.services.values());
        }
        if (descriptor.repositories != null) {
            registerRepositories(descriptor.repositories.values());
        }
    }

    public String getName() {
        return name;
    }

    public ServiceDescriptor getServiceDescriptor(String className) {
        return services.get(className);
    }

    public RepositoryDescriptor getRepositoryDescriptor(String className) {
        return repositories.get(className);
    }

    public final InitialContext getJndiContext() {
        return jndiContext;
    }

    public Object lookup(String jndiName) throws NamingException {
        return jndiContext.lookup(jndiName);
    }

    public Object getService(ServiceDescriptor sd) throws NamingException {
        return serviceConnector.connect(sd);
    }

    public CoreSession openRepository(RepositoryDescriptor rd, Map<String, Serializable> ctx)
            throws NamingException, ClientException {
        CoreSession session = repositoryConnector.connect(rd);
        if (session != null) {
            String sid = session.connect(rd.name, ctx);
            // register session on local JVM so it can be used later by doc models
            CoreInstance.getInstance().registerSession(sid, session);
        }
        return session;
    }

    void registerServices(Collection<ServiceDescriptor> services) {
        for (ServiceDescriptor sd : services) {
            sd.server = this;
            if (sd.jndiName == null) {
                continue;
            }
            if (sd.jndiName.startsWith("%")) {
                sd.jndiName = jndiPrefix + sd.jndiName.substring(1)
                        + jndiSuffix;
            }
            this.services.put(sd.serviceClass, sd);
            if (!sd.isPrivate) {
                platform.registerService(sd);
            }
        }
    }

    void registerRepositories(Collection<RepositoryDescriptor> repositories) {
        for (RepositoryDescriptor rd : repositories) {
            rd.server = this;
            this.repositories.put(rd.name, rd);
            platform.registerRepository(rd);
        }
    }

}

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * A client view on the application.
 * <p>
 * It enables the client to access platform services.
 * <p>
 * A platform is composed one or more servers running in different JVMs.
 * The default server is the one on the same JVM as the client and must have
 * the name "default".
 * <p>
 * Services are described by a host, port and other optional attributes and
 * are exposing services and repositories to the local platform. These services
 * are exposed via EJB3 remoting.
 * If a service is not found on defined servers the platform will delegate
 * the lookup on the local NXRuntime service registry.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @deprecated Use {@link Framework} API instead. Will be removed in 5.2.
 */
@Deprecated
@SuppressWarnings({"ALL"})
public final class Platform implements Serializable {

    private static final Log log = LogFactory.getLog(Platform.class);

    private static final long serialVersionUID = 6176553194123324439L;

    private final String name;

    private Map<String, ServiceDescriptor> services;
    private Map<String, RepositoryDescriptor> repositories;
    private Map<String, Server> servers;

    private final Map<String, List<ServerDescriptor>> pendingBindings;


    public Platform(String name) {
        this.name = name;
        servers = new HashMap<String, Server>();
        services = new HashMap<String, ServiceDescriptor>();
        repositories = new HashMap<String, RepositoryDescriptor>();
        pendingBindings = new HashMap<String, List<ServerDescriptor>>();
    }

    public synchronized  void addServer(ServerDescriptor sd) throws Exception {
        Server server = servers.get(sd.name);
        if (server != null) {
            // this is a server bindings contribution
            if (sd.services != null) {
                server.registerServices(sd.services.values());
            }
            if (sd.repositories != null) {
                server.registerRepositories(sd.repositories.values());
            }
        } else if (sd.host != null) {
            // this is the server definition
            server = new Server(this, sd);
            servers.put(sd.name, server);
            // register any bindings that are waiting for this server
            registerPendingBindings(server);
        } else {
            // this is a server bindings contribution - but the server is not yet registered.
            // register contribs into a pending queue
            addToPendingQueue(sd);
        }
    }

    public synchronized  void removeServer(String name) {
        Server server = servers.remove(name);
        if (server == null) {
            return;
        }
        Iterator<ServiceDescriptor> sit = services.values().iterator();
        while (sit.hasNext()) {
            ServiceDescriptor sd = sit.next();
            if (sd.server == server) {
                sit.remove();
            }
        }
        Iterator<RepositoryDescriptor> rit = repositories.values().iterator();
        while (rit.hasNext()) {
            RepositoryDescriptor sd = rit.next();
            if (sd.server == server) {
                rit.remove();
            }
        }
        //server.dispose();
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T getService(Class<T> remoteItf) throws Exception {
        ServiceDescriptor sd = services.get(remoteItf.getName());
        if (sd != null) {
            return (T) sd.server.getService(sd);
        }
        // try to find a local service through NXRuntime
        return Framework.getRuntime().getService(remoteItf);
    }

    public CoreSession openRepository(String repositoryName) throws Exception {
        return openRepository(repositoryName, null);
    }

    public synchronized CoreSession openRepository(String repositoryName,
            Map<String, Serializable> ctx) throws Exception {
        RepositoryManager repositoryMgr = Framework.getService(RepositoryManager.class);
        Repository repo = repositoryMgr.getRepository(repositoryName);
        if (repo != null) {
            if (ctx == null) {
                ctx = new HashMap<String, Serializable>();
            }
            return repo.open(ctx);
        }
        return null;
    }

    public synchronized ServerDescriptor[] getServers() {
        return servers.values().toArray(new ServerDescriptor[servers.size()]);
    }

    // added for compatibility
    public Repository getDefaultRepository() {
        try {
            RepositoryManager repositoryMgr = Framework.getService(RepositoryManager.class);
            return repositoryMgr.getDefaultRepository();
        } catch (Exception e) {
            log.error(e, e);
            return null;
        }
    }

    public synchronized  void dispose() {
        services.clear();
        services = null;
        repositories.clear();
        repositories = null;
        servers.clear();
        servers = null;
    }

    void registerService(ServiceDescriptor sd) {
        services.put(sd.serviceClass, sd);
    }

    void unregisterService(String className) {
        services.remove(className);
    }

    void registerRepository(RepositoryDescriptor rd) {
        repositories.put(rd.name, rd);
    }

    void unregisterRepository(String name) {
        repositories.remove(name);
    }

    private void registerPendingBindings(Server server) {
        List<ServerDescriptor> bindings = pendingBindings.remove(server.getName());
        if (bindings == null) {
            return; // no pending bindings for this server
        }
        for (ServerDescriptor sd : bindings) {
            if (sd.services != null) {
                server.registerServices(sd.services.values());
            }
            if (sd.repositories != null) {
                server.registerRepositories(sd.repositories.values());
            }
        }
    }

    private void addToPendingQueue(ServerDescriptor sd) {
        List<ServerDescriptor> bindings = pendingBindings.get(sd.name);
        if (bindings == null) {
            bindings = new ArrayList<ServerDescriptor>();
            pendingBindings.put(sd.name, bindings);
        }
        bindings.add(sd);
    }

}

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

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.remoting.transporter.TransporterClient;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("server")
public class ServerDescriptor {

    private final InvokerLocator locator;
    private Server server;
    private Map<ComponentName, RemoteComponent> components;
    private Set<String> extensions; // extensions that were already contributed

    public ServerDescriptor(InvokerLocator locator) {
        this.locator = locator;
        components = new Hashtable<ComponentName, RemoteComponent>();
        extensions = new HashSet<String>();
    }

    public ServerDescriptor(String uri) throws MalformedURLException {
        this(new InvokerLocator(uri));
    }


    public Server getServer() throws Exception {
        if (server == null) {
            synchronized (this) {
                if (server == null) {
                    server = (Server) TransporterClient.createTransporterClient(locator, Server.class);
                }
            }
        }
        return server;
    }

    /**
     * @return the identity.
     */
    public String getURI() {
        return locator.getLocatorURI();
    }

    public InvokerLocator getLocator() {
        return locator;
    }

    public Collection<ComponentName> getComponents() {
        return components.keySet();
    }

    public RemoteComponent addComponent(ComponentName name) {
        RemoteComponent rco = new RemoteComponent(this, name);
        components.put(name, rco);
        return rco;
    }

    public RemoteComponent getComponent(ComponentName name) {
        return components.get(name);
    }

    public RemoteComponent removeComponent(ComponentName name) {
        return components.remove(name);
    }

    public boolean hasComponent(ComponentName name) {
        return components.containsKey(name);
    }

    public boolean addExtension(String extensionId) {
        if (!extensions.contains(extensionId)) {
            synchronized (this) {
                if (!extensions.contains(extensionId)) {
                    extensions.add(extensionId);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasExtension(String extensionId) {
        synchronized (this) {
            return extensions.contains(extensionId);
        }
    }

    public synchronized boolean removeExtension(String extensionId) {
        synchronized (this) {
            return extensions.remove(extensionId);
        }
    }

    public void destroy() {
        if (server != null) {
            TransporterClient.destroyTransporterClient(server);
            server = null;
        }
        components.clear();
        components = null;
        extensions.clear();
        extensions = null;
    }

}

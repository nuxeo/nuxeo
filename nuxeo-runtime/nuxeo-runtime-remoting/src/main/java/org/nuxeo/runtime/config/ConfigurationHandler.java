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

import java.util.ArrayList;
import java.util.List;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.remoting.Server;
import org.nuxeo.runtime.remoting.UnsupportedServerVersionException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ConfigurationHandler {

    private static final List<ConfigurationHandler> registry = new ArrayList<ConfigurationHandler>();

    public static void registerHandler(ConfigurationHandler handler) {
        registry.add(handler);
    }

    public static ConfigurationHandler getHandler(Version version)
            throws UnsupportedServerVersionException {
        if (version == null) {
            version = Version.MIN;
        }
        for (int i = 0, len = registry.size(); i < len; i++) {
            ConfigurationHandler cl = registry.get(i);
            if (cl.accept(version)) {
                return cl;
            }
        }
        throw new UnsupportedServerVersionException(version);
    }

    public static ServerConfiguration loadConfig(InvokerLocator locator, Server server, String version)
            throws ConfigurationException {
        return loadConfig(locator, server, Version.parseString(version));
    }

    public static ServerConfiguration loadConfig(InvokerLocator locator, Server server, Version version)
            throws ConfigurationException {
        return getHandler(version).loadConfig(locator, server);
    }

    public static ServerConfiguration buildConfig(Version version) throws ConfigurationException {
        return getHandler(version).buildConfig();
    }

    public boolean accept(Version v) {
        return v.isEqualTo(getVersion());
    }

    public abstract Version getVersion();

    /**
     * Loads the configuration of the given remote server.
     *
     * @param locator the server locator
     * @param server the server proxy object
     * @return the configuration
     */
    public abstract ServerConfiguration loadConfig(InvokerLocator locator, Server server)
            throws ConfigurationException;

    /**
     * Gets the configuration for the current running framework.
     *
     * @return the configuration
     */
    public abstract ServerConfiguration buildConfig() throws ConfigurationException;

}

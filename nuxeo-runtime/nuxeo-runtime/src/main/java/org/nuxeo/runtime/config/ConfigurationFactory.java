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
import org.nuxeo.runtime.remoting.UnsupportedServerVersionException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ConfigurationFactory {

    private static final List<ConfigurationFactory> registry = new ArrayList<ConfigurationFactory>();

    public static void registerFactory(ConfigurationFactory handler) {
        registry.add(handler);
    }

    public static ConfigurationFactory getFactory(Version version)
            throws UnsupportedServerVersionException {
        if (version == null) {
            version = Version.MIN;
        }
        for (int i=0,len=registry.size(); i<len; i++) {
            ConfigurationFactory cf = registry.get(i);
            if (cf.accept(version)) {
                return cf;
            }
        }
        throw new UnsupportedServerVersionException(version);
    }

    public boolean accept(Version v) {
        return v.isEqualTo(getVersion());
    }

    public abstract Version getVersion();

    /**
     * Loads the configuration of the given remote server.
     *
     * @param locator the server locator
     * @param version
     * @return the configuration
     */
    public abstract ServerConfiguration createConfiguration(InvokerLocator locator, Version version)
            throws ConfigurationException;

}

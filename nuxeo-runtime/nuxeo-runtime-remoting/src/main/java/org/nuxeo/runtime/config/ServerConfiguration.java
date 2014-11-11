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

import java.io.Serializable;
import java.util.Properties;

import org.jboss.remoting.InvokerLocator;
import org.nuxeo.runtime.Version;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ServerConfiguration extends Serializable {

    String getName();

    Version getVersion();

    String getProductInfo();

    String[] getPeers();

    InvokerLocator getLocator();

    /**
     * Gets the server properties.
     *
     * @return the server properties
     */
    Properties getProperties();

    /**
     * Get the JNDI properties required by clients to connect to this server.
     */
    Properties getJndiProperties();

    /**
     * Installs the given configuration on the running framework.
     *
     * @throws Exception if any exception occurs
     */
    void install() throws Exception;

}

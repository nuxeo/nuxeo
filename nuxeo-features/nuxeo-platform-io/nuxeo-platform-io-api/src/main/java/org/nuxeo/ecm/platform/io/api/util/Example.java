/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.io.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.nuxeo.ecm.core.api.PathRef;

/**
 * Example on how to use copy between repositories
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Example {

    public static IOConfiguration createLocalConfiguration() {
        LocalConfiguration cfg = new LocalConfiguration();
        // define what you want to do:
        cfg.addDocument(new PathRef("/default-domain/workspaces/ws1"));
        cfg.setProperty(IOConfiguration.DOC_READER_FACTORY, "org.nuxeo.MyReaderFactory");
        cfg.setProperty("put here factory properties if any too", "...");
        // ...
        return cfg;
    }

    public static IOConfiguration createRemoteConfiguration() {
        // create the jndi environment to lookup the remote IOManager
        // This example show how to configure for JBoss with default transport
        // If HTTP transport is used then you should change this (look in nuxeo.properties file)
        Properties env = new Properties();
        // where remote_host is the host IP where remote IOManager is deployed
        env.put("java.naming.provider.url", "jnp://remote_host:1099");
        env.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        // The JBOSS JNDI binding for IOManagerBean is nuxeo/IOManagerBean/remote
        RemoteConfiguration cfg = new RemoteConfiguration("nuxeo/IOManagerBean/remote", env);
        // define what you want to do:
        cfg.addDocument(new PathRef("/default-domain/workspaces/ws1"));
        cfg.setProperty(IOConfiguration.DOC_READER_FACTORY, "org.nuxeo.MyReaderFactory");
        cfg.setProperty("put here factory properties if any too", "...");
        // ...
        return cfg;
    }

    public static void copyFromLocalToRemote() throws Exception {
        IOConfiguration src = createLocalConfiguration();
        IOConfiguration dest = createRemoteConfiguration();
        Collection<String> ioAdapters = new ArrayList<String>();
        // create io adapters if any is needed
        IOHelper.copy(src, dest, ioAdapters);
    }

    public static void copyFromLocalToLocal() throws Exception {
        IOConfiguration src = createLocalConfiguration();
        IOConfiguration dest = createLocalConfiguration();
        Collection<String> ioAdapters = new ArrayList<String>();
        // create io adapters if any is needed
        IOHelper.copy(src, dest, ioAdapters);
    }

    public static void copyRemoteToRemote() throws Exception {
        IOConfiguration src = createRemoteConfiguration();
        IOConfiguration dest = createRemoteConfiguration();
        Collection<String> ioAdapters = new ArrayList<String>();
        // create io adapters if any is needed
        IOHelper.copy(src, dest, ioAdapters);
    }

    public static void copyRemoteToLocal() throws Exception {
        IOConfiguration src = createRemoteConfiguration();
        IOConfiguration dest = createLocalConfiguration();
        Collection<String> ioAdapters = new ArrayList<String>();
        // create io adapters if any is needed
        IOHelper.copy(src, dest, ioAdapters);
    }

    public static void main(String[] args) {
    }

}

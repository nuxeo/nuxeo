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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentName;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RemoteClassLoader extends ClassLoader {

    private static final Log log = LogFactory.getLog(RemoteClassLoader.class);

    private final ComponentName component;

    private final ServerDescriptor sd;

    private final Map<String, Class> loadedClasses = new Hashtable<String, Class>();

    private final Map<String, URI> loadedResources = new Hashtable<String, URI>();

    public RemoteClassLoader(ServerDescriptor sd, ComponentName component,
            ClassLoader parent) {
        super(parent != null ? parent
                : Thread.currentThread().getContextClassLoader());
        this.component = component;
        this.sd = sd;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class klass = loadedClasses.get(name);
        if (klass != null) {
            return klass;
        }
        log.info("Loading class " + name + " from remote");
        try {
            byte[] bytes = sd.getServer().getClass(component, name);
            if (bytes != null) {
                klass = defineClass(name, bytes, 0, bytes.length, null);
                loadedClasses.put(name, klass);
                return klass;
            }
        } catch (Exception e) {
            log.error("findClass failed", e);
            throw new ClassNotFoundException("Failed to find remote class", e);
        }
        return klass;
    }

    @Override
    protected URL findResource(String name) {
        URL resource;
        try {
            resource = loadedResources.get(name).toURL();
        } catch (MalformedURLException e) {
            resource = null;
        }
        if (resource != null) {
            return resource;
        }
        log.info("Loading resource " + name + " from remote");
        try {
            byte[] bytes = sd.getServer().getClass(component, name);
            if (bytes != null) {
                File file = createTempFile(name);
                resource = file.toURI().toURL();
                if (resource != null) {
                    loadedResources.put(name, resource.toURI());
                    return resource;
                }
            }
        } catch (Exception e) {
            log.error("Failed to create temp file for storing remote resource",
                    e);
        }
        return resource;
    }

    protected static File createTempFile(String name) throws IOException {
        File file = File.createTempFile("nxruntime-remote-" + name, ".tmp");
        file.deleteOnExit();
        return file;
    }

}

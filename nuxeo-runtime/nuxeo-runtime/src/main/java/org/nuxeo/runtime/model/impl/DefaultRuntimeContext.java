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

package org.nuxeo.runtime.model.impl;

import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.osgi.framework.Bundle;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultRuntimeContext implements RuntimeContext {

    private static final Log log = LogFactory.getLog(RuntimeContext.class);

    protected RuntimeService runtime;
    protected final ComponentDescriptorReader reader;
    protected final Map<String, ComponentName> deployedFiles;


    public DefaultRuntimeContext() {
        this(Framework.getRuntime());
    }

    public DefaultRuntimeContext(RuntimeService runtime) {
        this.runtime = runtime;
        reader = new ComponentDescriptorReader();
        deployedFiles = new Hashtable<String, ComponentName>();
    }

    public void setRuntime(RuntimeService runtime) {
        this.runtime = runtime;
    }

    public RuntimeService getRuntime() {
        return runtime;
    }

    public Map<String, ComponentName> getDeployedFiles() {
        return deployedFiles;
    }

    public URL getResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    public URL getLocalResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    public RegistrationInfo deploy(URL url) throws Exception {
        if (deployedFiles.containsKey(url.toString())) {
            return null;
        }
        log.debug("Deploying bundle from url " + url);
        RegistrationInfoImpl ri = createRegistrationInfo(url);
        ri.context = this;
        runtime.getComponentManager().register(ri);
        deployedFiles.put(url.toString(), ri.getName());
        return ri;
    }

    public void undeploy(URL url) throws Exception {
        ComponentName name = deployedFiles.remove(url.toString());
        if (name != null) {
            runtime.getComponentManager().unregister(name);
        }
    }

    public boolean isDeployed(URL url) {
        return deployedFiles.containsKey(url.toString());
    }

    public RegistrationInfo deploy(String location) throws Exception {
        URL url = getLocalResource(location);
        if (url != null) {
            return deploy(url);
        } else {
            log.warn("No local resources was found with this name: " + location);
        }
        return null;
    }

    public void undeploy(String location) throws Exception {
        URL url = getLocalResource(location);
        if (url != null) {
            undeploy(url);
        } else {
            log.warn("No local resources was found with this name: " + location);
        }
    }

    public boolean isDeployed(String location) {
        URL url = getLocalResource(location);
        if (url != null) {
            return isDeployed(url);
        } else {
            log.warn("No local resources was found with this name: " + location);
            return false;
        }
    }

    public void destroy() {
        Iterator<ComponentName> it = deployedFiles.values().iterator();
        ComponentManager mgr = runtime.getComponentManager();
        while (it.hasNext()) {
            ComponentName name = it.next();
            it.remove();
            mgr.unregister(name);
        }
    }

    public Bundle getBundle() {
        return null;
    }

    public RegistrationInfoImpl createRegistrationInfo(URL url) throws Exception {
        InputStream in = url.openStream();
        try {
            return createRegistrationInfo(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public RegistrationInfoImpl createRegistrationInfo(InputStream in) throws Exception {
        return reader.read(this, in);
    }

}

/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.StreamRef;
import org.nuxeo.runtime.model.URLStreamRef;
import org.nuxeo.runtime.osgi.OSGiRuntimeActivator;
import org.nuxeo.runtime.osgi.OSGiRuntimeContext;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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

    @Override
    public RuntimeService getRuntime() {
        return runtime;
    }

    public Map<String, ComponentName> getDeployedFiles() {
        return deployedFiles;
    }

    @Override
    public URL getResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    @Override
    public URL getLocalResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(
                className);
    }

    @Override
    public RegistrationInfo deploy(URL url) throws Exception {
        return deploy(new URLStreamRef(url));
    }

    @Override
    public RegistrationInfo deploy(StreamRef ref) throws Exception {
        String name = ref.getId();
        if (deployedFiles.containsKey(name)) {
            return null;
        }
        RegistrationInfoImpl ri = createRegistrationInfo(ref);
        if (ri == null || ri.name == null) {
            // not parsed correctly, e.g., faces-config.xml
            return null;
        }
        log.debug("Deploying component from url " + name);
        ri.context = this;
        ri.xmlFileUrl = ref.asURL();
        if (ri.getBundle() != null) {
            // this is an external component XML.
            // should use the real owner bundle as the context.
            Bundle bundle = OSGiRuntimeActivator.getInstance().getBundle(ri.getBundle());
            if (bundle != null) {
                ri.context = new OSGiRuntimeContext(bundle);
            }
        }
        runtime.getComponentManager().register(ri);
        deployedFiles.put(name, ri.getName());
        return ri;
    }

    @Override
    public void undeploy(URL url) throws Exception {
        ComponentName name = deployedFiles.remove(url.toString());
        if (name != null) {
            runtime.getComponentManager().unregister(name);
        }
    }

    @Override
    public void undeploy(StreamRef ref) throws Exception {
        ComponentName name = deployedFiles.remove(ref.getId());
        if (name != null) {
            runtime.getComponentManager().unregister(name);
        }
    }

    @Override
    public boolean isDeployed(URL url) {
        return deployedFiles.containsKey(url.toString());
    }

    @Override
    public boolean isDeployed(StreamRef ref) {
        return deployedFiles.containsKey(ref.getId());
    }

    @Override
    public RegistrationInfo deploy(String location) throws Exception {
        URL url = getLocalResource(location);
        if (url != null) {
            return deploy(url);
        }
        log.warn("No local resources was found with this name: " + location);
        return null;
    }

    @Override
    public void undeploy(String location) throws Exception {
        URL url = getLocalResource(location);
        if (url != null) {
            undeploy(url);
        } else {
            log.warn("No local resources was found with this name: " + location);
        }
    }

    @Override
    public boolean isDeployed(String location) {
        URL url = getLocalResource(location);
        if (url != null) {
            return isDeployed(url);
        } else {
            log.warn("No local resources was found with this name: " + location);
            return false;
        }
    }

    @Override
    public void destroy() {
        Iterator<ComponentName> it = deployedFiles.values().iterator();
        ComponentManager mgr = runtime.getComponentManager();
        mgr.shutdown();
    }

    @Override
    public Bundle getBundle() {
        return null;
    }

    public RegistrationInfoImpl createRegistrationInfo(StreamRef ref)
            throws Exception {
        String source = FileUtils.read(ref.getStream());
        String expanded = Framework.expandVars(source);
        InputStream in = new ByteArrayInputStream(expanded.getBytes());
        try {
            return createRegistrationInfo(in);
        } finally {
            in.close();
        }
    }

    public RegistrationInfoImpl createRegistrationInfo(InputStream in)
            throws Exception {
        return reader.read(this, in);
    }

}

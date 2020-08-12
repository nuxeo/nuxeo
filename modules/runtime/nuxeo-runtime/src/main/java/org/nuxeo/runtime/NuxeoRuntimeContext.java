/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.StreamRef;

/**
 * Implementation of a {@link RuntimeContext} for {@link NuxeoRuntimeService}.
 * <p/>
 * This context is aimed to hold the {@link Bundle bundle}, its class loader and the deployed components linked to this
 * bundle.
 * <p/>
 * This context is not designed to deploy and undeploy components, it leaves this responsibility to
 * {@link NuxeoRuntimeService} which holds the {@link ComponentManager} responsible of components states.
 *
 * @since 11.1
 */
public class NuxeoRuntimeContext implements RuntimeContext {

    protected static final Logger log = LogManager.getLogger(NuxeoRuntimeContext.class);

    protected final Bundle bundle;

    protected final URLClassLoader classLoader;

    /** The list of component names deployed by this context. */
    protected final List<ComponentName> components;

    public NuxeoRuntimeContext(Bundle bundle, ClassLoader parent) {
        try {
            this.bundle = bundle;
            this.classLoader = new URLClassLoader(bundle.getName(), new URL[] { bundle.getFile().toURI().toURL() },
                    parent);
            this.components = new ArrayList<>();
        } catch (MalformedURLException e) {
            throw new RuntimeServiceException(e);
        }
    }

    @Override
    public RuntimeService getRuntime() {
        throw new UnsupportedOperationException("Context can no more access runtime");
    }

    @Override
    public org.osgi.framework.Bundle getBundle() {
        // TODO bridge it !
        return null;
    }

    public Bundle getBundleDescriptor() {
        return bundle;
    }

    @Override
    public ComponentName[] getComponents() {
        return components.toArray(new ComponentName[0]);
    }

    @Override
    public URL getResource(String name) {
        return classLoader.findResource(name);
    }

    /**
     * @deprecated since 11.1, all bundle resources are now local
     */
    @Override
    @Deprecated(since = "11.1")
    public URL getLocalResource(String name) {
        return getResource(name);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    @Override
    public RegistrationInfo deploy(String location) {
        throw new UnsupportedOperationException("Deploying with context is no more supported");
    }

    @Override
    public RegistrationInfo deploy(URL url) {
        throw new UnsupportedOperationException("Deploying with context is no more supported");
    }

    @Override
    public RegistrationInfo deploy(StreamRef ref) {
        throw new UnsupportedOperationException("Deploying with context is no more supported");
    }

    @Override
    public void undeploy(String location) {
        throw new UnsupportedOperationException("Un-deploying with context is no more supported");
    }

    @Override
    public void undeploy(URL url) {
        throw new UnsupportedOperationException("Un-deploying with context is no more supported");
    }

    @Override
    public void undeploy(StreamRef ref) {
        throw new UnsupportedOperationException("Un-deploying with context is no more supported");
    }

    @Override
    public boolean isDeployed(String location) {
        throw new UnsupportedOperationException(
                "Checking if the component is deployed with context is no more supported");
    }

    @Override
    public boolean isDeployed(URL url) {
        throw new UnsupportedOperationException(
                "Checking if the component is deployed with context is no more supported");
    }

    @Override
    public boolean isDeployed(StreamRef ref) {
        throw new UnsupportedOperationException(
                "Checking if the component is deployed with context is no more supported");
    }

    @Override
    public void destroy() {
        try {
            classLoader.close();
        } catch (IOException e) {
            throw new RuntimeServiceException("Unable to close the class loader of bundle: " + classLoader.getName());
        }
    }
}

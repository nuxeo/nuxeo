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

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.model.RuntimeModelException;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.StreamRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractRuntimeContext implements RuntimeContext {

    private static final Log log = LogFactory.getLog(RuntimeContext.class);

    protected int state = UNREGISTERED;

    protected final String name;

    protected AbstractRuntimeService runtime;

    protected final ComponentDescriptorReader reader = new ComponentDescriptorReader();

    protected final Map<URL, RegistrationInfo[]> deployedFiles = new Hashtable<URL, RegistrationInfo[]>();

    protected final Set<RegistrationInfoImpl> registeredInfos = new HashSet<RegistrationInfoImpl>();

    protected final Set<RegistrationInfoImpl> pendingInfos = new HashSet<RegistrationInfoImpl>();

    protected final Set<RegistrationInfoImpl> resolvedInfos = new HashSet<RegistrationInfoImpl>();

    protected final Set<AbstractRuntimeContext> requiredPendingContexts = new HashSet<AbstractRuntimeContext>();

    protected final Set<AbstractRuntimeContext> requiredContexts = new HashSet<AbstractRuntimeContext>();

    protected final Set<AbstractRuntimeContext> dependsOnMeContexts = new HashSet<AbstractRuntimeContext>();

    protected AbstractRuntimeContext(String name) {
        this.name = name;
    }

    public void setRegistered(AbstractRuntimeService runtime) throws RuntimeModelException {
        if (state != UNREGISTERED) {
            throw new IllegalArgumentException("Not in unregistered state ("
                    + this + ")");
        }
        this.runtime = runtime;
        handleRegistering();
        state = REGISTERED;
        handleRegistered();
    }

    protected void handleRegistering() throws RuntimeModelException {

    }

    protected void handleRegistered() throws RuntimeModelException {
        if (pendingInfos.isEmpty() && requiredPendingContexts.isEmpty()) {
            setResolved();
        }
    }

    public boolean isRegistered() {
        return state == REGISTERED;
    }

    public void setResolved() throws RuntimeModelException {
        if (state != REGISTERED) {
            throw new RuntimeModelException(this + " : not in registered state");
        }
        handleResolving();
        state = RESOLVED;
        handleResolved();
    }

    protected void handleResolving() throws RuntimeModelException {
        ComponentName selfName = new ComponentName(name);
        RegistrationInfoImpl info = runtime.manager.getRegistrationInfo(selfName);
        if (info != null) {
            return;
        }
        info = new RegistrationInfoImpl(selfName);
        info.setContext(this);
        pendingInfos.add(info);
        runtime.manager.register(info);
    }

    protected void handleResolved() {

    }

    public boolean isResolved() {
        return state == RESOLVED;
    }

    public void setActivated() throws Exception {
        if (state != RESOLVED) {
            throw new IllegalStateException("Not in resolved state (" + this
                    + ")");
        }

        state = ACTIVATING;

        handleActivating();

        state = ACTIVATED;

        handleActivated();

    }

    protected void handleActivating() {
        for (RegistrationInfoImpl info : resolvedInfos) {
            info.lazyActivate();
        }
    }

    protected void handleActivated() throws RuntimeModelException {
        RuntimeModelException.CompoundBuilder errors = new RuntimeModelException.CompoundBuilder();
        for (AbstractRuntimeContext other : dependsOnMeContexts) {
            other.requiredPendingContexts.remove(this);
            other.requiredContexts.add(this);
            if (other.isRegistered()) {
                if (other.pendingInfos.isEmpty()
                        && other.requiredPendingContexts.isEmpty()) {
                    other.setResolved();
                }
            } else if (other.isActivated()) {
                for (RegistrationInfoImpl info : other.resolvedInfos) {
                    if (info.context.requiredPendingContexts.isEmpty()) {
                        info.lazyActivate();
                    }
                }
            }
        }
        errors.throwOnError();
    }

    @Override
    public boolean isActivated() {
        return state == ACTIVATED;
    }

    @Override
    public RuntimeService getRuntime() {
        return runtime;
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
    public RegistrationInfo[] deploy(URL url) throws RuntimeModelException  {
        if (deployedFiles.containsKey(url)) {
            return null;
        }
        RegistrationInfoImpl[] ris;
        try {
            ris = reader.read(this, url);
        } catch (IOException e) {
            throw new RuntimeModelException("Cannot read components of " + url, e);
        }
        for (RegistrationInfoImpl ri : ris) {
            ComponentName name = ri.getName();
            if (name == null) {
                // not parsed correctly, e.g., faces-config.xml
                continue;
            }

            log.debug("Deploying component from url " + name);

            registeredInfos.add(ri);
            pendingInfos.add(ri);
            runtime.getComponentManager().register(ri);
        }
        deployedFiles.put(url, ris);
        return ris;
    }

    @Override
    public RegistrationInfo[] deploy(StreamRef ref) throws RuntimeModelException  {
        return deploy(ref.asURL());
    }

    @Override
    public void undeploy(URL url)  {
        RegistrationInfo[] infos = deployedFiles.remove(url);
        for (RegistrationInfo info : infos) {
            runtime.getComponentManager().unregister(info);
        }
    }

    @Override
    public void undeploy(StreamRef ref)  {
        undeploy(ref.asURL());
    }

    @Override
    public boolean isDeployed(URL url) {
        return deployedFiles.containsKey(url);
    }

    @Override
    public boolean isDeployed(StreamRef ref) {
        return deployedFiles.containsKey(ref.asURL());
    }

    @Override
    public RegistrationInfo[] deploy(String location) throws RuntimeModelException {
        URL url = getLocalResource(location);
        if (url == null) {
            log.warn("No local resources was found with this name: " + location);
            return new RegistrationInfoImpl[0];
        }
        return deploy(url);
    }

    @Override
    public void undeploy(String location) throws RuntimeModelException {
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
        for (URL url : new HashSet<URL>(deployedFiles.keySet())) {
            try {
                undeploy(url);
            } catch (Exception e) {
                log.error("Cannot undeploy components from " + url, e);
            }
        }
    }

    public RegistrationInfo[] getRegisteredInfos() {
        return registeredInfos.toArray(new RegistrationInfo[registeredInfos.size()]);
    }

    public RegistrationInfo[] getPendingInfos() {
        return pendingInfos.toArray(new RegistrationInfo[pendingInfos.size()]);
    }

    public RegistrationInfo[] getResolvedInfos() {
        return resolvedInfos.toArray(new RegistrationInfo[resolvedInfos.size()]);
    }


    public AbstractRuntimeContext[] getRequiredContexts() {
        return requiredContexts.toArray(new AbstractRuntimeContext[requiredContexts.size()]);
    }

    public AbstractRuntimeContext[] getRequiredPendingContexts() {
        return requiredPendingContexts.toArray(new AbstractRuntimeContext[requiredPendingContexts.size()]);
   }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractRuntimeContext other = (AbstractRuntimeContext) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    protected void handleComponentResolved(RegistrationInfoImpl info)
            throws RuntimeModelException {
        if (!pendingInfos.remove(info)) {
            return;
        }
        resolvedInfos.add(info);
        boolean requiredActivated = true;
        for (RegistrationInfoImpl other : info.requiredResolved) {
            if (this == other.context) {
                continue;
            }
            other.context.dependsOnMeContexts.add(this);
            if (!other.isActivated()) {
                requiredPendingContexts.add(other.context);
            } else {
                requiredContexts.add(other.context);
            }
            if (!other.isActivated()) {
                requiredActivated = false;
            }
        }
        if (isRegistered()) {
            if (pendingInfos.isEmpty() && requiredPendingContexts.isEmpty()) {
                setResolved();
            }
        } else if (isActivated()) {
            if (requiredActivated) {
                if (!info.isActivated()) {
                    info.activate();
                }
            }
        }
    }

    protected void handleComponentActivated(RegistrationInfoImpl info) {

    }

}

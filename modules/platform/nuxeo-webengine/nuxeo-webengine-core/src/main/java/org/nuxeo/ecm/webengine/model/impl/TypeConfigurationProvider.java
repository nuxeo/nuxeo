/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TypeConfigurationProvider {

    protected final List<TypeDescriptor> types;

    protected final List<AdapterDescriptor> services;

    protected final List<TypeRegistry> registries;

    public TypeConfigurationProvider() {
        types = new ArrayList<>();
        services = new ArrayList<>();
        registries = new Vector<>();
    }

    public void flushCache() {
        // do nothing
    }

    public synchronized boolean isEmpty() {
        return types.isEmpty() && services.isEmpty();
    }

    public synchronized void registerType(TypeDescriptor td) {
        types.add(td);
        fireTypeRegistered(td);
    }

    public synchronized void unregisterType(TypeDescriptor td) {
        if (types.remove(td)) {
            fireTypeUnregistered(td);
        }
    }

    public synchronized void registerAction(AdapterDescriptor ad) {
        services.add(ad);
        fireServiceRegistered(ad);
    }

    public synchronized void unregisterAction(AdapterDescriptor ad) {
        if (services.remove(ad)) {
            fireServiceUnregistered(ad);
        }
    }

    public void addRegistry(TypeRegistry registry) {
        registries.add(registry);
    }

    public void removeRegistry(TypeRegistry listener) {
        registries.remove(listener);
    }

    public synchronized void install(TypeRegistry registry) {
        for (TypeDescriptor td : types) {
            registry.registerType(td);
        }
        for (AdapterDescriptor ad : services) {
            registry.registerAdapter(ad);
        }
        addRegistry(registry);
    }

    protected void fireServiceRegistered(AdapterDescriptor ad) {
        if (registries.isEmpty()) {
            return;
        }
        for (TypeRegistry reg : registries.toArray(new TypeRegistry[registries.size()])) {
            reg.registerAdapter(ad);
        }
    }

    protected void fireServiceUnregistered(AdapterDescriptor ad) {
        if (registries.isEmpty()) {
            return;
        }
        for (TypeRegistry reg : registries.toArray(new TypeRegistry[registries.size()])) {
            reg.unregisterAdapter(ad);
        }
    }

    protected void fireTypeRegistered(TypeDescriptor td) {
        if (registries.isEmpty()) {
            return;
        }
        for (TypeRegistry listener : registries.toArray(new TypeRegistry[registries.size()])) {
            listener.registerType(td);
        }
    }

    protected void fireTypeUnregistered(TypeDescriptor td) {
        if (registries.isEmpty()) {
            return;
        }
        for (TypeRegistry listener : registries.toArray(new TypeRegistry[registries.size()])) {
            listener.unregisterType(td);
        }
    }

}

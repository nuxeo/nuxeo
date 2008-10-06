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

package org.nuxeo.ecm.webengine.model.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeConfigurationProvider {
    
    protected List<TypeDescriptor> types;
    protected List<ServiceDescriptor> services;
    protected List<TypeRegistry> registries;
    

    public TypeConfigurationProvider() {
        types = new ArrayList<TypeDescriptor>();
        services = new ArrayList<ServiceDescriptor>();
        registries = new Vector<TypeRegistry>();
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
    
    public synchronized void registerAction(ServiceDescriptor ad) {
        services.add(ad);
        fireServiceRegistered(ad);
    }

    public synchronized void unregisterAction(ServiceDescriptor ad) {
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
        for (ServiceDescriptor ad : services) {
            registry.registerService(ad);
        }
        addRegistry(registry);
    }
    


    protected void fireServiceRegistered(ServiceDescriptor ad) {
        if (registries.isEmpty()) {
            return;
        }
        for (TypeRegistry reg : registries.toArray(new TypeRegistry[registries.size()])) {
           reg.registerService(ad);
        }
    }

    protected void fireServiceUnregistered(ServiceDescriptor ad) {
        if (registries.isEmpty()) {
            return;
        }
        for (TypeRegistry reg : registries.toArray(new TypeRegistry[registries.size()])) {
           reg.unregisterService(ad);
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

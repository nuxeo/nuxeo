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

package org.nuxeo.ecm.webengine.rest.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeConfigurationProvider {
    
    protected List<TypeDescriptor> types;
    protected List<ActionDescriptor> actions;
    protected List<TypeRegistry> registries;
    

    public TypeConfigurationProvider() {
        types = new ArrayList<TypeDescriptor>();
        actions = new ArrayList<ActionDescriptor>();
        registries = new Vector<TypeRegistry>();
    }
    
    public synchronized boolean isEmpty() {
        return types.isEmpty() && actions.isEmpty();
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
    
    public synchronized void registerAction(ActionDescriptor ad) {
        actions.add(ad);
        fireActionRegistered(ad);
    }
    
    public synchronized void unregisterAction(ActionDescriptor ad) {
        if (actions.remove(ad)) {
            fireActionUnregistered(ad);
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
        for (ActionDescriptor ad : actions) {
            registry.registerAction(ad);
        }
        addRegistry(registry);
    }
    


    protected void fireActionRegistered(ActionDescriptor ad) {
        if (registries.isEmpty()) {
            return;
        }
        for (TypeRegistry listener : registries.toArray(new TypeRegistry[registries.size()])) {
           listener.registerAction(ad);
        }
    }

    protected void fireActionUnregistered(ActionDescriptor ad) {
        if (registries.isEmpty()) {
            return;
        }
        for (TypeRegistry listener : registries.toArray(new TypeRegistry[registries.size()])) {
           listener.unregisterAction(ad);
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

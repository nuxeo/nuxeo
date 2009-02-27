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
 */
package org.nuxeo.ecm.cmis.common;

import java.util.HashMap;
import java.util.Map;


/**
 * TODO implement unregister
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings("unchecked")
public class AdapterManager implements ClassRegistry {

    // adaptableClass => { adapterClass => adapterFactory }
    protected Map<Class<?>, Map<Class<?>, AdapterFactory>> registry; 
    
    
    public AdapterManager() {
        this.registry = new HashMap<Class<?>, Map<Class<?>, AdapterFactory>>();
    }
    
    public synchronized void registerAdapters(Class<?> clazz, AdapterFactory factory) {
        Map<Class<?>, AdapterFactory> adapters = registry.get(clazz);
        if (adapters == null) {
            adapters = new HashMap<Class<?>, AdapterFactory>();
        }
        for (Class<?> adapterType :  factory.getAdapterTypes()) {
            adapters.put(adapterType, factory);    
        }        
        registry.put(clazz, adapters);
    }
    
    public void unregisterAdapters(Class<?> clazz) {
        //TODO
    }

    public void unregisterAdapterFactory(Class<?> factory) {
        //TODO
    }

    public synchronized AdapterFactory getAdapterFactory(Class<?> adaptee, Class<?> adapter) {
        Map<Class<?>, AdapterFactory> adapters = 
            (Map<Class<?>, AdapterFactory>)ClassLookup.lookup(adaptee, this);        
        if (adapters != null) {
            return adapters.get(adapter);
        }
        return null;
    }
    
    public <T> T getAdapter(Object adaptee, Class<T> adapter) {
        AdapterFactory factory = getAdapterFactory(adaptee.getClass(), adapter);
        return factory == null ? null : factory.getAdapter(adaptee, adapter);
    }
    
    public Object get(Class<?> clazz) {
        return registry.get(clazz);
    }
    
    public void put(Class<?> clazz, Object value) {
        registry.put(clazz, (Map<Class<?>, AdapterFactory>)value);
    }
    
}

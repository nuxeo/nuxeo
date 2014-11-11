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
package org.nuxeo.ecm.automation.core.impl;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.automation.TypeAdapter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AdapterKeyedRegistry extends SuperKeyedRegistry<TypeAdapterKey, TypeAdapter> {
    
    protected Set<Class<?>> blacklist;
    
    public AdapterKeyedRegistry() {
        blacklist = new HashSet<Class<?>>();
        blacklist.add(Serializable.class);
        blacklist.add(Cloneable.class);
        blacklist.add(Comparable.class);
    }
    
    @Override
    protected boolean isRoot(TypeAdapterKey key) {
        return key.input == Object.class;
    }
    
    @Override
    protected List<TypeAdapterKey> getSuperKeys(TypeAdapterKey key) {
        List<TypeAdapterKey> result = new ArrayList<TypeAdapterKey>();
        Class<?> cl = key.input.getSuperclass();
        if (cl != null) {
            result.add(new TypeAdapterKey(cl, key.output));
        }
        for (Class<?> itf : key.input.getInterfaces()) {
            if (!blacklist.contains(itf)) {
                result.add(new TypeAdapterKey(itf, key.output));
            }
        }
        return result;
    }
    
    @Override
    protected boolean isCachingEnabled(TypeAdapterKey key) {
        return !Proxy.isProxyClass(key.input);
    }
    
}

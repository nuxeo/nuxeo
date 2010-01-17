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

package org.nuxeo.runtime.binding;

import java.util.HashMap;
import java.util.Map;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SimpleServiceProvider extends AbstractServiceProvider {

    protected final Map<String, Entry> registry;

    /**
     * The default constructor is using a {@link HashMap} as the registry
     */
    public SimpleServiceProvider() {
        this(new HashMap<String, Entry>());
    }

    public SimpleServiceProvider(Map<String, Entry> registry) {
        this.registry = registry;
    }

    public void registerService(Class<?> itf,  Object serviceInstance) {
        Entry entry = new Entry();
        entry.obj = serviceInstance;
        entry.itf = itf;
        registry.put(itf.getName(), entry);
    }

    public void unregisterService(Class<?> itf) {
        Entry entry = registry.remove(itf);
        if (entry != null) {
            if (entry.bindingKey != null) {

            }
        }
    }

    public Map<String, Entry> getRegistry() {
        return registry;
    }

    public void destroy() {
        // TODO Auto-generated method stub
    }

    /**
     * Named service lookup is not yet supported (so bindingKey will be ignored at lookup time)
     */
    public Object getService(Class<?> serviceClass, String bindingKey) {
        String name = serviceClass.getName();
        Object obj = registry.get(name);
        if (obj != null && manager != null) {
            if (obj instanceof Binding) { // use binding as a factory
                manager.registerBinding(bindingKey, (Binding)obj);
            } else {
                manager.registerBinding(bindingKey, new StaticBinding(bindingKey, obj));
            }
            return obj;
        }
        return null;
    }

    public class Entry {
        public Object obj;
        public Class<?> itf;
        public String bindingKey;
    }

}

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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl.osm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeAnnotationRegistry<T> {

    protected final Map<Class<?>, Annotation> registry = new HashMap<Class<?>, Annotation>();


    @SuppressWarnings("unchecked")
    public synchronized T get(Class<?> type) {
        Annotation anno = lookup(type);
        return (T) anno.data;
    }

    public synchronized void put(Class<?> type, T data) {
        Annotation anno = registry.get(type);
        if (anno != null) {
            remove(type); // TODO find a better solution to update reg
        }
        registry.put(type, new Annotation(type, null, data));
    }

    public synchronized void remove(Class<?> type) {
        Annotation anno = registry.remove(type);
        if (anno == null) {
            return;
        }
        Iterator<Annotation> it = registry.values().iterator();
        while (it.hasNext()) {
            if (it.next().provider == type) {
                it.remove();
            }
        }
    }

    protected Annotation lookup(Class<?> type) {
        Annotation anno = registry.get(type);
        if (anno != null) {
            return anno;
        }
        Class<?> superClass = type.getSuperclass();
        if (superClass == null) { // process interfaces
            for (Class<?> itf : type.getInterfaces()) {
                anno = lookup(itf);
                if (anno != null && anno.data != null) {
                    registry.put(type, anno);
                    return anno;
                }
            }
        } else {
            // descent into superclasses
            LinkedList<Class<?>> queue = new LinkedList<Class<?>>();
            queue.add(type);
            anno = lookup(superClass, queue);
            if (anno != null && anno.data != null) {
                registry.put(type, new Annotation(type, anno.type, anno.data));
                return anno;
            }
            // process interfaces of queued classes
            Class<?> t = queue.poll();
            while (t != null) {
                for (Class<?> itf : t.getInterfaces()) {
                    anno = lookup(itf);
                    if (anno != null && anno.data != null) {
                        registry.put(t, new Annotation(t, anno.type, anno.data));
                        return anno;
                    }
                }
                t = queue.poll();
            }
        }
        anno = new Annotation(type, null, null);
        registry.put(type, anno);
        return anno;
    }

    protected Annotation lookup(Class<?> type, Queue<Class<?>> queue) {
        Annotation anno = registry.get(type);
        if (anno != null) {
            return anno;
        }

        queue.add(type);

        Class<?> superClass = type.getSuperclass();
        if (superClass != null) { //descent in super classes
            anno = lookup(superClass, queue);
        }
        if (anno == null) {
            anno = new Annotation(type, null, null);
        }
        registry.put(type, anno);
        return anno;
    }

    public static class Annotation {
        final Class<?> type;
        final Class<?> provider;
        final Object data;

        public Annotation(Class<?> type, Class<?> provider, Object data) {
            this.type = type;
            this.provider = provider;
            this.data = data;
        }
    }

}

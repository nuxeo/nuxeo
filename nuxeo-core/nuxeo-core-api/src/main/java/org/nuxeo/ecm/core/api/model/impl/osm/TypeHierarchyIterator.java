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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeHierarchyIterator implements Iterator<Class<?>> {

    final Queue<Class<?>> queue;
    Class<?> type;

    public TypeHierarchyIterator(Class<?> type) {
        this.type = type;
        queue = new LinkedList<Class<?>>();
    }

    public boolean hasNext() {
        return type != null;
    }

    public Class<?> next() {
        if (type == null) {
            throw new NoSuchElementException();
        }
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            queue.add(superClass);
        }
        for (Class<?> itf : type.getInterfaces()) {
            queue.add(itf);
        }
        Class<?> prev = type;
        type = queue.poll();
        return prev;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove not supported");
    }

}

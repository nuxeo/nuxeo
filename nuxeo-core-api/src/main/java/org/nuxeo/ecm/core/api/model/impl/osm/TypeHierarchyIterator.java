/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    @Override
    public boolean hasNext() {
        return type != null;
    }

    @Override
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

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported");
    }

}

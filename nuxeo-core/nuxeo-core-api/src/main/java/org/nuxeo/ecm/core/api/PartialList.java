/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import java.util.Iterator;
import java.util.List;

/**
 * The bundling of a list and a total size.
 */
public class PartialList<E> implements Iterable<E> {

    public final List<E> list;

    public final long totalSize;

    /**
     * Constructs a partial list.
     *
     * @param list the list
     * @param totalSize the total size
     */
    public PartialList(List<E> list, long totalSize) {
        this.list = list;
        this.totalSize = totalSize;
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

}

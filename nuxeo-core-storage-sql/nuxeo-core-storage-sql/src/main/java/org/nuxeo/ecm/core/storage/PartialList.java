/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage;

import java.io.Serializable;
import java.util.List;

/**
 * The bundling of a list and a total size.
 * <p>
 * The list MUST be {@link Serializable}.
 */
public class PartialList<E> implements Serializable {

    private static final long serialVersionUID = 1L;

    public final List<E> list;

    public final long totalSize;

    /**
     * Constructs a partial list.
     * <p>
     * The list MUST be {@link Serializable}.
     *
     * @param list the list (MUST be {@link Serializable})
     * @param totalSize the total size
     */
    public PartialList(List<E> list, long totalSize) {
        this.list = list;
        this.totalSize = totalSize;
    }

}

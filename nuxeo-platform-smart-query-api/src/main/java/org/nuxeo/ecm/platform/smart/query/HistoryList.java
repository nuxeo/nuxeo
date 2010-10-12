/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.query;

import java.util.LinkedList;

/**
 * Linked list with a capacity to handle undo/redo actions.
 * <p>
 * The method {@link #addLast(Object)} will remove the first object of the list
 * when at full capacity.
 *
 * @since 5.4
 * @author Anahide Tchertchian
 */
public class HistoryList<E> extends LinkedList<E> {

    private static final long serialVersionUID = 1L;

    protected int capacity;

    public HistoryList(int capacity) {
        super();
        this.capacity = capacity;
    }

    @Override
    public void addLast(E o) {
        if (size() >= capacity) {
            removeFirst();
        }
        super.addLast(o);
    }

}

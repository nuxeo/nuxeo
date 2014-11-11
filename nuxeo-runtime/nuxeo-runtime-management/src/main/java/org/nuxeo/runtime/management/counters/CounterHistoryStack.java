/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.runtime.management.counters;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * Fixed length Stack that is used to store values of a counter over time
 *
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class CounterHistoryStack implements Iterable<long[]> {

    protected final LinkedList<long[]> list = new LinkedList<long[]>();

    protected final int maxSize;

    public CounterHistoryStack(int size) {
        maxSize = size;
    }

    public synchronized void push(long[] item) {
        list.push(item);
        if (list.size() > maxSize) {
            list.remove(list.size() - 1);
        }
    }

    @Override
    public Iterator<long[]> iterator() {
        return list.iterator();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (long[] entry : this) {
            sb.append(entry[0]);
            sb.append(" => ");
            sb.append(entry[1]);
            sb.append("\n");
        }
        return sb.toString();
    }

    public long[] get(int idx) {
        return list.get(idx);
    }

    public LinkedList<long[]> getAsList() {
        return list;
    }
}

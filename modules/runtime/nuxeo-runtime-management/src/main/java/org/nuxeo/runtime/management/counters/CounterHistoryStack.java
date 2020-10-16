/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Fixed length Stack that is used to store values of a counter over time
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @deprecated since 11.4: use dropwizard metrics instead
 */
@Deprecated(since = "11.4")
public class CounterHistoryStack implements Iterable<long[]> {

    protected final LinkedList<long[]> list = new LinkedList<>();

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
        StringBuilder sb = new StringBuilder();

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

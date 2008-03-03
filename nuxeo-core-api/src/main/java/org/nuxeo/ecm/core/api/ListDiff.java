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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * A list that is detached from its data source so all modifications on the list
 * are recorded so that the data source will be updated later when the list will
 * be reconnected to it.
 * <p>
 * It purposedly doesn't implement the List interface.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ListDiff implements Serializable {

    public static final int ADD = 1;

    public static final int INSERT = 2;

    public static final int REMOVE = 3;

    public static final int MODIFY = 4;

    public static final int MOVE = 5;

    public static final int CLEAR = 6;

    private static final long serialVersionUID = 2239608903749525011L;

    final List<Entry> diff = new ArrayList<Entry>();

    public ListDiff() {

    }

    public ListDiff(ListDiff listDiff) {
        if (listDiff != null) {
            diff.addAll(Arrays.asList(listDiff.diff()));
        }
    }

    public void add(Object value) {
        diff.add(new Entry(ADD, 0, value));
    }

    public void insert(int index, Object value) {
        diff.add(new Entry(INSERT, index, value));
    }

    public void modify(int index, Object value) {
        diff.add(new Entry(MODIFY, index, value));
    }

    public void move(int fromIndex, int toIndex) {
        // XXX AT: here value is the toIndex, not strange?
        diff.add(new Entry(MOVE, fromIndex, toIndex));
    }

    public void remove(int index) {
        diff.add(new Entry(REMOVE, index, null));
    }

    public void removeAll() {
        diff.add(new Entry(CLEAR, 0, null));
    }

    public void reset() {
        diff.clear();
    }

    public boolean isDirty() {
        return !diff.isEmpty();
    }

    public Entry[] diff() {
        return diff.toArray(new Entry[diff.size()]);
    }

    @Override
    public String toString() {
        return String.format("ListDiff { %s }", diff.toString());
    }

    public static String typeToString(int type) {
        if (type == 1) {
            return "ADD";
        } else if (type == 2) {
            return "INSERT";
        } else if (type == 3) {
            return "REMOVE";
        } else if (type == 4) {
            return "MODIFY";
        } else if (type == 5) {
            return "MOVE";
        } else if (type == 6) {
            return "CLEAR";
        } else {
            return "invalid type: " + Integer.toString(type);
        }
    }

    public static class Entry implements Serializable {

        private static final long serialVersionUID = -3261465349877937657L;

        public int index;

        public int type;

        public Object value;

        public Entry() {
        }

        public Entry(int type, int index, Object value) {
            this.index = index;
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("Entry {%s, %s, %s}",
                    index, ListDiff.typeToString(type), value);
        }

    }

}

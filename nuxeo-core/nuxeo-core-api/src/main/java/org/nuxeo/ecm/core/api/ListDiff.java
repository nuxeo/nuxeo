/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A list that is detached from its data source so all modifications on the list are recorded so that the data source
 * will be updated later when the list will be reconnected to it.
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

    final List<Entry> diff = new ArrayList<>();

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
            return String.format("Entry {%s, %s, %s}", index, ListDiff.typeToString(type), value);
        }

    }

}

/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.ImmutableSet;

/**
 * Abstraction for a Map<String, Serializable> that is Serializable.
 * <p>
 * Internal storage is optimized to avoid a full {@link HashMap} when there is a small number of keys.
 *
 * @since 5.9.5
 */
public class State implements StateAccessor, Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(State.class);

    private static final int HASHMAP_DEFAULT_INITIAL_CAPACITY = 16;

    private static final float HASHMAP_DEFAULT_LOAD_FACTOR = 0.75f;

    // maximum size to use an array after which we switch to a full HashMap
    public static final int ARRAY_MAX = 5;

    private static final int DEBUG_MAX_STRING = 100;

    private static final int DEBUG_MAX_ARRAY = 10;

    public static final State EMPTY = new State(Collections.<String, Serializable> emptyMap());

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /** Initial key order for the {@link #toString} method. */
    private static final Set<String> TO_STRING_KEY_ORDER = new LinkedHashSet<>(Arrays.asList(
            new String[] { "ecm:id", "ecm:primaryType", "ecm:name", "ecm:parentId", "ecm:isVersion", "ecm:isProxy" }));

    /**
     * A diff for a {@link State}.
     * <p>
     * Each value is applied to the existing {@link State}. An element can be:
     * <ul>
     * <li>a {@link StateDiff}, to be applied on a {@link State},
     * <li>a {@link ListDiff}, to be applied on an array/{@link List},
     * <li>an actual value to be set (including {@code null}).
     * </ul>
     *
     * @since 5.9.5
     */
    public static class StateDiff extends State {
        private static final long serialVersionUID = 1L;

        @Override
        public void put(String key, Serializable value) {
            // for a StateDiff, we don't have concurrency problems
            // and we want to store nulls explicitly
            putEvenIfNull(key, value);
        }
    }

    /**
     * Singleton marker.
     */
    private static enum Nop {
        NOP
    }

    /**
     * Denotes no change to an element.
     */
    public static final Nop NOP = Nop.NOP;

    /**
     * A diff for an array or {@link List}.
     * <p>
     * This diff is applied onto an existing array/{@link List} in the following manner:
     * <ul>
     * <li>{@link #diff}, if any, is applied,
     * <li>{@link #rpush}, if any, is applied.
     * </ul>
     *
     * @since 5.9.5
     */
    public static class ListDiff implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Whether this {@link ListDiff} applies to an array ({@code true}) or a {@link List} ({@code false}).
         */
        public boolean isArray;

        /**
         * If diff is not {@code null}, each element of the list is applied to the existing array/{@link List}. An
         * element can be:
         * <ul>
         * <li>a {@link StateDiff}, to be applied on a {@link State},
         * <li>an actual value to be set (including {@code null}),
         * <li>{@link #NOP} if no change is needed.
         * </ul>
         */
        public List<Object> diff;

        /**
         * If rpush is not {@code null}, this is appended to the right of the existing array/{@link List}.
         */
        public List<Object> rpush;

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + (isArray ? "array" : "list")
                    + (diff == null ? "" : ", DIFF " + diff) + (rpush == null ? "" : ", RPUSH " + rpush) + ')';
        }
    }

    // if map != null then use it
    protected Map<String, Serializable> map;

    // else use keys / values
    protected List<String> keys;

    protected List<Serializable> values;

    /**
     * Private constructor with explicit map.
     */
    private State(Map<String, Serializable> map) {
        this.map = map;
    }

    /**
     * Constructor with default capacity.
     */
    public State() {
        this(0, false);
    }

    /**
     * Constructor with default capacity, optionally thread-safe.
     *
     * @param threadSafe if {@code true}, then a {@link ConcurrentHashMap} is used
     */
    public State(boolean threadSafe) {
        this(0, threadSafe);
    }

    /**
     * Constructor for a given default size.
     */
    public State(int size) {
        this(size, false);
    }

    /**
     * Constructor for a given default size, optionally thread-safe.
     *
     * @param threadSafe if {@code true}, then a {@link ConcurrentHashMap} is used
     */
    public State(int size, boolean threadSafe) {
        if (threadSafe) {
            map = new ConcurrentHashMap<String, Serializable>(initialCapacity(size));
        } else {
            if (size > ARRAY_MAX) {
                map = new HashMap<>(initialCapacity(size));
            } else {
                keys = new ArrayList<String>(size);
                values = new ArrayList<Serializable>(size);
            }
        }
    }

    protected static int initialCapacity(int size) {
        return Math.max((int) (size / HASHMAP_DEFAULT_LOAD_FACTOR) + 1, HASHMAP_DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Gets the number of elements.
     */
    public int size() {
        if (map != null) {
            return map.size();
        } else {
            return keys.size();
        }
    }

    /**
     * Checks if the state is empty.
     */
    public boolean isEmpty() {
        if (map != null) {
            return map.isEmpty();
        } else {
            return keys.isEmpty();
        }
    }

    /**
     * Gets a value for a key, or {@code null} if the key is not present.
     */
    public Serializable get(Object key) {
        if (map != null) {
            return map.get(key);
        } else {
            int i = keys.indexOf(key);
            return i >= 0 ? values.get(i) : null;
        }
    }

    /**
     * Sets a key/value.
     */
    public void put(String key, Serializable value) {
        if (value == null) {
            // if we're using a ConcurrentHashMap
            // then null values are forbidden
            // this is ok given our semantics of null vs absent key
            if (map != null) {
                map.remove(key);
            } else {
                int i = keys.indexOf(key);
                if (i >= 0) {
                    // cost is not trivial but we don't use this often, if at all
                    keys.remove(i);
                    values.remove(i);
                }
            }
        } else {
            putEvenIfNull(key, value);
        }
    }

    protected void putEvenIfNull(String key, Serializable value) {
        if (map != null) {
            map.put(key, value);
        } else {
            int i = keys.indexOf(key);
            if (i >= 0) {
                // existing key
                values.set(i, value);
            } else {
                // new key
                if (keys.size() < ARRAY_MAX) {
                    keys.add(key);
                    values.add(value);
                } else {
                    // upgrade to a full HashMap
                    map = new HashMap<>(initialCapacity(keys.size() + 1));
                    for (int j = 0; j < keys.size(); j++) {
                        map.put(keys.get(j), values.get(j));
                    }
                    map.put(key, value);
                    keys = null;
                    values = null;
                }
            }
        }
    }

    /**
     * Removes the mapping for a key.
     *
     * @return the previous value associated with the key, or {@code null} if there was no mapping for the key
     */
    public Serializable remove(Object key) {
        if (map != null) {
            return map.remove(key);
        } else {
            int i = keys.indexOf(key);
            if (i >= 0) {
                keys.remove(i);
                return values.remove(i);
            } else {
                return null;
            }
        }
    }

    /**
     * Gets the key set. IT MUST NOT BE MODIFIED.
     */
    public Set<String> keySet() {
        if (map != null) {
            return map.keySet();
        } else {
            return ImmutableSet.copyOf(keys);
        }
    }

    /**
     * Gets an array of keys.
     */
    public String[] keyArray() {
        if (map != null) {
            return map.keySet().toArray(EMPTY_STRING_ARRAY);
        } else {
            return keys.toArray(EMPTY_STRING_ARRAY);
        }
    }

    /**
     * Checks if there is a mapping for the given key.
     */
    public boolean containsKey(Object key) {
        if (map != null) {
            return map.containsKey(key);
        } else {
            return keys.contains(key);
        }
    }

    /**
     * Gets the entry set. IT MUST NOT BE MODIFIED.
     */
    public Set<Entry<String, Serializable>> entrySet() {
        if (map != null) {
            return map.entrySet();
        } else {
            return new ArraysEntrySet();
        }
    }

    /** EntrySet optimized to just return a simple Iterator on the entries. */
    protected class ArraysEntrySet implements Set<Entry<String, Serializable>> {

        @Override
        public int size() {
            return keys.size();
        }

        @Override
        public boolean isEmpty() {
            return keys.isEmpty();
        }

        @Override
        public Iterator<Entry<String, Serializable>> iterator() {
            return new ArraysEntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(Entry<String, Serializable> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Entry<String, Serializable>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    public class ArraysEntryIterator implements Iterator<Entry<String, Serializable>> {

        private int index;

        @Override
        public boolean hasNext() {
            return index < keys.size();
        }

        @Override
        public Entry<String, Serializable> next() {
            return new ArraysEntry(index++);
        }
    }

    public class ArraysEntry implements Entry<String, Serializable> {

        private final int index;

        public ArraysEntry(int index) {
            this.index = index;
        }

        @Override
        public String getKey() {
            return keys.get(index);
        }

        @Override
        public Serializable getValue() {
            return values.get(index);
        }

        @Override
        public Serializable setValue(Serializable value) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Overridden to display Calendars and arrays better, and truncate long strings and arrays.
     * <p>
     * Also displays some keys first (ecm:id, ecm:name, ecm:primaryType)
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder buf = new StringBuilder();
        buf.append('{');
        boolean empty = true;
        // some keys go first
        for (String key : TO_STRING_KEY_ORDER) {
            if (containsKey(key)) {
                if (!empty) {
                    buf.append(", ");
                }
                empty = false;
                buf.append(key);
                buf.append('=');
                toString(buf, get(key));
            }
        }
        // sort keys
        String[] keys = keyArray();
        Arrays.sort(keys);
        for (String key : keys) {
            if (TO_STRING_KEY_ORDER.contains(key)) {
                // already done
                continue;
            }
            if (!empty) {
                buf.append(", ");
            }
            empty = false;
            buf.append(key);
            buf.append('=');
            toString(buf, get(key));
        }
        buf.append('}');
        return buf.toString();
    }

    @SuppressWarnings("boxing")
    protected static void toString(StringBuilder buf, Object value) {
        if (value instanceof String) {
            String v = (String) value;
            if (v.length() > DEBUG_MAX_STRING) {
                v = v.substring(0, DEBUG_MAX_STRING) + "...(" + v.length() + " chars)...";
            }
            buf.append(v);
        } else if (value instanceof Calendar) {
            Calendar cal = (Calendar) value;
            char sign;
            int offset = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 60000;
            if (offset < 0) {
                offset = -offset;
                sign = '-';
            } else {
                sign = '+';
            }
            buf.append(String.format("Calendar(%04d-%02d-%02dT%02d:%02d:%02d.%03d%c%02d:%02d)", cal.get(Calendar.YEAR), //
                    cal.get(Calendar.MONTH) + 1, //
                    cal.get(Calendar.DAY_OF_MONTH), //
                    cal.get(Calendar.HOUR_OF_DAY), //
                    cal.get(Calendar.MINUTE), //
                    cal.get(Calendar.SECOND), //
                    cal.get(Calendar.MILLISECOND), //
                    sign, offset / 60, offset % 60));
        } else if (value instanceof Object[]) {
            Object[] v = (Object[]) value;
            buf.append('[');
            for (int i = 0; i < v.length; i++) {
                if (i > 0) {
                    buf.append(',');
                    if (i > DEBUG_MAX_ARRAY) {
                        buf.append("...(" + v.length + " items)...");
                        break;
                    }
                }
                toString(buf, v[i]);
            }
            buf.append(']');
        } else {
            buf.append(value);
        }
    }

    @Override
    public Object getSingle(String name) {
        Serializable object = get(name);
        if (object instanceof Object[]) {
            Object[] array = (Object[]) object;
            if (array.length == 0) {
                return null;
            } else if (array.length == 1) {
                // data migration not done in database, return a simple value anyway
                return array[0];
            } else {
                log.warn("Property " + name + ": expected a simple value but read an array: " + Arrays.toString(array));
                return array[0];
            }
        } else {
            return object;
        }
    }

    @Override
    public Object[] getArray(String name) {
        Serializable object = get(name);
        if (object == null) {
            return null;
        } else if (object instanceof Object[]) {
            return (Object[]) object;
        } else {
            // data migration not done in database, return an array anyway
            return new Object[] { object };
        }
    }

    @Override
    public void setSingle(String name, Object value) {
        put(name, (Serializable) value);
    }

    @Override
    public void setArray(String name, Object[] value) {
        put(name, value);
    }

    @Override
    public boolean equals(Object other) {
        return StateHelper.equalsStrict(this, other);
    }

}

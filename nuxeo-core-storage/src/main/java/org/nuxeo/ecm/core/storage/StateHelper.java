/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.storage.State.DiffOp.RPOP;
import static org.nuxeo.ecm.core.storage.State.DiffOp.RPUSH;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.ObjectUtils;
import org.nuxeo.ecm.core.storage.State.Diff;
import org.nuxeo.ecm.core.storage.binary.Binary;

/**
 * Helpers for deep copy and deep diff of {@link State} objects.
 */
public class StateHelper {

    /** Utility class. */
    private StateHelper() {
    }

    /**
     * Checks if we have a base type compatible with {@link State} helper
     * processing.
     */
    public static boolean isScalar(Serializable value) {
        return value instanceof String //
                || value instanceof Boolean //
                || value instanceof Long //
                || value instanceof Double //
                || value instanceof Calendar //
                || value instanceof Binary;
    }

    /**
     * Compares two values.
     */
    public static boolean equals(Serializable a, Serializable b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (a instanceof State && b instanceof State) {
            return equals((State) a, (State) b);
        } else if (a instanceof List && b instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> la = (List<Serializable>) a;
            @SuppressWarnings("unchecked")
            List<Serializable> lb = (List<Serializable>) b;
            return equals(la, lb);
        } else if (a instanceof Object[] && b instanceof Object[]) {
            // array values are supposed to be scalars
            return Arrays.equals((Object[]) a, (Object[]) b);
        } else if (isScalar(a) && isScalar(b)) {
            return a.equals(b);
        } else {
            return false;
        }
    }

    /**
     * Compares two {@link State}s.
     */
    public static boolean equals(State a, State b) {
        if (a.size() != b.size()) {
            return false;
        }
        if (!a.keySet().equals(b.keySet())) {
            return false;
        }
        for (Entry<String, Serializable> en : a.entrySet()) {
            String key = en.getKey();
            Serializable va = en.getValue();
            Serializable vb = b.get(key);
            if (!equals(va, vb)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two {@link List}s.
     */
    public static boolean equals(List<Serializable> a, List<Serializable> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (Iterator<Serializable> ita = a.iterator(), itb = b.iterator(); ita.hasNext();) {
            if (!equals(ita.next(), itb.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare the first length elements of two arrays.
     * <p>
     * The arrays are assumed to have at least length elements.
     */
    public static boolean equals(Object[] a, Object[] b, int length) {
        for (int i = 0; i < length; i++) {
            // basic scalar equals
            if (!ObjectUtils.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare the first size elements of two lists.
     * <p>
     * The lists are assumed to have at least size elements.
     */
    public static boolean equals(List<Serializable> a, List<Serializable> b,
            int size) {
        Iterator<Serializable> ita = a.iterator();
        Iterator<Serializable> itb = b.iterator();
        for (int i = 0; i < size; i++) {
            if (!equals(ita.next(), itb.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Makes a deep copy of a value.
     */
    public static Serializable deepCopy(Serializable value) {
        if (value == null) {
            return value;
        } else if (value instanceof State) {
            return deepCopy((State) value);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) value;
            return (Serializable) deepCopy(list);
        } else if (value instanceof Object[]) {
            // array values are supposed to be scalars
            return ((Object[]) value).clone();
        }
        // else scalar value -- check anyway (debug)
        else if (!isScalar(value)) {
            throw new UnsupportedOperationException("Cannot deep copy: "
                    + value.getClass().getName());
        }
        return value;
    }

    /**
     * Makes a deep copy of a {@link State} map.
     */
    public static State deepCopy(State state) {
        State copy = new State(state.size());
        for (Entry<String, Serializable> en : state.entrySet()) {
            copy.put(en.getKey(), deepCopy(en.getValue()));
        }
        return copy;
    }

    /**
     * Makes a deep copy of a {@link List}.
     */
    public static List<Serializable> deepCopy(List<Serializable> list) {
        List<Serializable> copy = new ArrayList<Serializable>(list.size());
        for (Serializable v : list) {
            copy.add(deepCopy(v));
        }
        return copy;
    }

    private static final Object[] ARRAY_RPOP = new Object[] { RPOP };

    private static final List<Serializable> LIST_RPOP = Collections.<Serializable> singletonList(RPOP);

    public static Serializable diff(Serializable a, Serializable b) {
        if (a instanceof Object[] && b instanceof Object[]) {
            return diff((Object[]) a, (Object[]) b);
        }
        if (a instanceof List && b instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> la = (List<Serializable>) a;
            @SuppressWarnings("unchecked")
            List<Serializable> lb = (List<Serializable>) b;
            return (Serializable) diff(la, lb);
        }
        if (a instanceof State && b instanceof State) {
            return diff((State) a, (State) b);
        }
        return b;
    }

    public static Object[] diff(Object[] a, Object[] b) {
        // check for RPUSH of one or more element
        // TODO configure zero-length "a" case
        if (a.length > 0 && b.length > a.length) {
            // compare initial segments
            if (!equals(a, b, a.length)) {
                return b;
            }
            Object[] diff = new Object[b.length - a.length + 1];
            diff[0] = RPUSH;
            System.arraycopy(b, a.length, diff, 1, b.length - a.length);
            return diff;
        }
        // check for RPOP
        // TODO configure zero-length "b" case
        if (b.length > 0 && a.length == b.length + 1) {
            // compare initial segments
            if (!equals(a, b, b.length)) {
                return b;
            }
            return ARRAY_RPOP;
        }
        return b;
    }

    public static List<Serializable> diff(List<Serializable> a,
            List<Serializable> b) {
        // check for RPUSH of one or more element
        // TODO configure zero-length "a" case
        if (a.size() > 0 && b.size() > a.size()) {
            // compare initial segments
            if (!equals(a, b, a.size())) {
                return b;
            }
            ArrayList<Serializable> diff = new ArrayList<>(b.size() - a.size()
                    + 1);
            diff.add(RPUSH);
            for (int i = a.size(); i < b.size(); i++) {
                diff.add(b.get(i));
            }
            return diff;
        }
        // check for RPOP
        // TODO configure zero-length "b" case
        if (b.size() > 0 && a.size() == b.size() + 1) {
            // compare initial segments
            if (!equals(a, b, b.size())) {
                return b;
            }
            return LIST_RPOP;
        }
        return b;
    }

    /**
     * Makes a diff copy of two {@link State} maps.
     * <p>
     * The returned diff state contains only the key/values that changed.
     * <p>
     * For values set to null or removed, the value is null.
     * <p>
     * For arrays, either a new array is passed, or the RPUSH operator is used
     * if only tail additions have been made, or the RPOP operator is used if
     * only a single tail removal was done.
     * <p>
     * For sub-documents, a recursive diff is returned using a {@link Diff}.
     *
     * @return a {@link map} which, when applied to a, gives b.
     */
    public static Diff diff(State a, State b) {
        Diff diff = new Diff();
        for (Entry<String, Serializable> en : a.entrySet()) {
            String key = en.getKey();
            if (!b.containsKey(key)) {
                // value must be cleared
                diff.put(key, null);
            } else {
                // compare values
                Serializable va = en.getValue();
                Serializable vb = b.get(key);
                if (!equals(va, vb)) {
                    diff.put(key, diff(va, vb));
                }
            }
        }
        for (Entry<String, Serializable> en : b.entrySet()) {
            String key = en.getKey();
            if (!a.containsKey(key)) {
                // value must be added
                diff.put(key, en.getValue());
            }
        }
        return diff;
    }

    /**
     * Applies a diff onto a base state.
     */
    public static void applyDiff(State state, Diff diff) {
        for (Entry<String, Serializable> en : diff.entrySet()) {
            String key = en.getKey();
            Serializable value = en.getValue();
            if (value instanceof Diff) {
                Serializable old = state.get(key);
                if (old == null) {
                    old = new State();
                    state.put(key, old);
                    // enter the next if
                }
                if (old instanceof State) {
                    applyDiff((State) old, (Diff) value);
                    continue;
                }
                throw new UnsupportedOperationException(
                        "Cannot apply Diff non-State: " + old);
            } else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                if (array.length > 0) {
                    if (array[0] == RPUSH) {
                        state.put(key, applyRPush(state.get(key), array));
                        continue;
                    } else if (array[0] == RPOP) {
                        state.put(key, applyRPop(state.get(key)));
                        continue;
                    }
                }
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Serializable> list = (List<Serializable>) value;
                if (list.size() > 0) {
                    if (list.get(0) == RPUSH) {
                        state.put(key, applyRPush(state.get(key), list));
                        continue;
                    } else if (list.get(0) == RPOP) {
                        state.put(key, applyRPop(state.get(key)));
                        continue;
                    }
                }
            }
            state.put(key, value);
        }
    }

    public static Serializable applyRPush(Serializable old,
            List<Serializable> list) {
        if (old == null) {
            old = (Serializable) Collections.emptyList();
        }
        if (old instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> oldList = (List<Serializable>) old;
            ArrayList<Serializable> newList = new ArrayList<>(oldList.size()
                    + list.size() - 1);
            newList.addAll(oldList);
            for (int i = 1; i < list.size(); i++) {
                newList.add(list.get(i));
            }
            return newList;
        }
        throw new UnsupportedOperationException(
                "Cannot apply RPUSH of list on non-list: " + old);
    }

    public static Serializable applyRPush(Serializable old, Object[] array) {
        if (old == null) {
            old = new Object[0];
        }
        if (old instanceof Object[]) {
            Object[] oldArray = (Object[]) old;
            Object[] newArray = new Object[oldArray.length + array.length - 1];
            System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
            System.arraycopy(array, 1, newArray, oldArray.length,
                    array.length - 1);
            return newArray;
        }
        throw new UnsupportedOperationException(
                "Cannot apply RPUSH of array on non-array: " + old);
    }

    public static Serializable applyRPop(Serializable old) {
        if (old instanceof Object[]) {
            Object[] oldArray = (Object[]) old;
            if (oldArray.length == 0) {
                // RPOP on empty array returns empty array
                return oldArray;
            }
            Object[] newArray = new Object[oldArray.length - 1];
            System.arraycopy(oldArray, 0, newArray, 0, newArray.length);
            return newArray;
        }
        if (old instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> oldList = (List<Serializable>) old;
            if (oldList.size() == 0) {
                // RPOP on empty list returns empty list
                return (Serializable) oldList;
            }
            return new ArrayList<>(oldList.subList(0, oldList.size() - 1));
        }
        throw new UnsupportedOperationException("Cannot apply RPOP on: " + old);
    }

}

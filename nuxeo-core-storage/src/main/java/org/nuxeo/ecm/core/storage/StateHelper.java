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

import static org.nuxeo.ecm.core.storage.State.NOP;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.ObjectUtils;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
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
    public static boolean isScalar(Object value) {
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
    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a instanceof State && b instanceof State //
                || a instanceof State && b == null //
                || a == null && b instanceof State) {
            return equals((State) a, (State) b);
        } else if (a instanceof List && b instanceof List //
                || a instanceof List && b == null //
                || a == null && b instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> la = (List<Serializable>) a;
            @SuppressWarnings("unchecked")
            List<Serializable> lb = (List<Serializable>) b;
            return equals(la, lb);
        } else if (a instanceof Object[] && b instanceof Object[] //
                || a instanceof Object[] && b == null //
                || a == null && b instanceof Object[]) {
            return equals((Object[]) a, (Object[]) b);
        } else if (a instanceof ListDiff && b instanceof ListDiff) {
            ListDiff lda = (ListDiff) a;
            ListDiff ldb = (ListDiff) b;
            return lda.isArray == ldb.isArray && equals(lda.diff, ldb.diff)
                    && equals(lda.rpush, ldb.rpush) && lda.rpop == ldb.rpop;
        } else if (isScalar(a) && isScalar(b)) {
            return a.equals(b);
        } else {
            return false;
        }
    }

    /**
     * Compares two {@link State}s.
     * <p>
     * A {@code null} value or an empty array or {@code List} is equivalent to
     * an absent value. A {@code null} {@link State} is equivalent to an empty
     * {@link State} (or a {@link State} containing only absent values).
     */
    public static boolean equals(State a, State b) {
        if (a == null) {
            a = State.EMPTY;
        }
        if (b == null) {
            b = State.EMPTY;
        }
        for (Entry<String, Serializable> en : a.entrySet()) {
            Serializable va = en.getValue();
            if (va == null) {
                // checked by loop on b
                continue;
            }
            String key = en.getKey();
            Serializable vb = b.get(key);
            if (!equals(va, vb)) {
                return false;
            }
        }
        for (Entry<String, Serializable> en : b.entrySet()) {
            String key = en.getKey();
            Serializable va = a.get(key);
            if (va != null) {
                // already checked by loop on a
                continue;
            }
            Serializable vb = en.getValue();
            if (!equals(null, vb)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two arrays of scalars.
     * <p>
     * {@code null} values are equivalent to empty arrays.
     */
    public static boolean equals(Object[] a, Object[] b) {
        if (a != null && a.length == 0) {
            a = null;
        }
        if (b != null && b.length == 0) {
            b = null;
        }
        // we have scalars, Arrays.equals() is enough
        return Arrays.equals(a, b);
    }

    /**
     * Compares two {@link List}s.
     * <p>
     * {@code null} values are equivalent to empty lists.
     */
    public static boolean equals(List<Serializable> a, List<Serializable> b) {
        if (a != null && a.isEmpty()) {
            a = null;
        }
        if (b != null && b.isEmpty()) {
            b = null;
        }
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
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
     * Makes a deep copy of a value.
     */
    public static Serializable deepCopy(Object value) {
        if (value == null) {
            return (Serializable) value;
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
        return (Serializable) value;
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

    /**
     * Does a diff of two values.
     *
     * @return a {@link StateDiff}, a {@link ListDiff}, {@link #NOP}, or an
     *         actual value (including {@code null})
     */
    public static Serializable diff(Object a, Object b) {
        if (equals(a, b)) {
            return NOP;
        }
        if (a instanceof Object[] && b instanceof Object[]) {
            return diff((Object[]) a, (Object[]) b);
        }
        if (a instanceof List && b instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> la = (List<Object>) a;
            @SuppressWarnings("unchecked")
            List<Object> lb = (List<Object>) b;
            return (Serializable) diff(la, lb);
        }
        if (a instanceof State && b instanceof State) {
            StateDiff diff = diff((State) a, (State) b);
            return diff.isEmpty() ? NOP : diff;
        }
        return (Serializable) b;
    }

    public static Serializable diff(Object[] a, Object[] b) {
        List<Object> la = Arrays.asList(a);
        List<Object> lb = Arrays.asList(b);
        Serializable diff = diff(la, lb);
        if (diff instanceof List) {
            return b;
        }
        ListDiff listDiff = (ListDiff) diff;
        listDiff.isArray = true;
        return listDiff;
    }

    public static Serializable diff(List<Object> a, List<Object> b) {
        ListDiff listDiff = new ListDiff();
        listDiff.isArray = false;
        int aSize = a.size();
        int bSize = b.size();
        // TODO configure zero-length "a" case
        boolean doRPush = aSize > 0 && aSize < bSize;
        // TODO configure zero-length "b" case
        boolean doRPop = bSize > 0 && aSize - 1 == bSize;
        // we can use a list diff if lists are the same size,
        // or we have a rpush or rpop
        boolean doDiff = aSize == bSize || doRPush || doRPop;
        if (!doDiff) {
            return (Serializable) b;
        }
        int len = Math.min(aSize, bSize);
        List<Object> diff = new ArrayList<>(len);
        int nops = 0;
        int diffs = 0;
        for (int i = 0; i < len; i++) {
            Serializable elemDiff = diff(a.get(i), b.get(i));
            if (elemDiff == NOP) {
                nops++;
            } else if (elemDiff instanceof StateDiff) {
                diffs++;
            }
            // TODO if the individual element diffs are big StateDiffs,
            // do a full State replacement instead
            diff.add(elemDiff);
        }
        if (nops == len) {
            // only nops
            diff = null;
        } else if (diffs == 0) {
            // only setting elements or nops
            // TODO use a higher ratio than 0% of diffs
            return (Serializable) b;
        }
        listDiff.diff = diff;
        if (doRPush) {
            List<Object> rpush = new ArrayList<>(bSize - aSize);
            for (int i = aSize; i < bSize; i++) {
                rpush.add(b.get(i));
            }
            listDiff.rpush = rpush;
        }
        if (doRPop) {
            listDiff.rpop = true;
        }
        return listDiff;
    }

    /**
     * Makes a diff copy of two {@link State} maps.
     * <p>
     * The returned diff state contains only the key/values that changed.
     * {@code null} values are equivalent to absent values.
     * <p>
     * For values set to null or removed, the value is null.
     * <p>
     * For sub-documents, a recursive diff is returned.
     *
     * @return a {@link StateDiff} which, when applied to a, gives b.
     */
    public static StateDiff diff(State a, State b) {
        StateDiff diff = new StateDiff();
        for (Entry<String, Serializable> en : a.entrySet()) {
            Serializable va = en.getValue();
            if (va == null) {
                // checked by loop on b
                continue;
            }
            String key = en.getKey();
            Serializable vb = b.get(key);
            if (vb == null) {
                // value must be cleared
                diff.put(key, null);
            } else {
                // compare values
                Serializable elemDiff = diff(va, vb);
                if (elemDiff != NOP) {
                    diff.put(key, elemDiff);
                }
            }
        }
        for (Entry<String, Serializable> en : b.entrySet()) {
            String key = en.getKey();
            Serializable va = a.get(key);
            if (va != null) {
                // already checked by loop on a
                continue;
            }
            Serializable vb = en.getValue();
            if (!equals(null, vb)) {
                // value must be added
                diff.put(key, vb);
            }
        }
        return diff;
    }

    /**
     * Applies a {@link StateDiff} in-place onto a base {@link State}.
     */
    public static void applyDiff(State state, StateDiff stateDiff) {
        for (Entry<String, Serializable> en : stateDiff.entrySet()) {
            String key = en.getKey();
            Serializable diffElem = en.getValue();
            if (diffElem instanceof StateDiff) {
                Serializable old = state.get(key);
                if (old == null) {
                    old = new State();
                    state.put(key, old);
                    // enter the next if
                }
                if (!(old instanceof State)) {
                    throw new UnsupportedOperationException(
                            "Cannot apply StateDiff on non-State: " + old);
                }
                applyDiff((State) old, (StateDiff) diffElem);
            } else if (diffElem instanceof ListDiff) {
                state.put(key, applyDiff(state.get(key), (ListDiff) diffElem));
            } else {
                state.put(key, diffElem);
            }
        }
    }

    /**
     * Applies a {@link ListDiff} onto an array or {@link List}, and returns the
     * resulting value.
     */
    public static Serializable applyDiff(Serializable value, ListDiff listDiff) {
        // internally work on a list
        if (listDiff.isArray && value != null) {
            if (!(value instanceof Object[])) {
                throw new UnsupportedOperationException(
                        "Cannot apply ListDiff on non-array: " + value);
            }
            value = new ArrayList<Object>(Arrays.asList((Object[]) value));
        }
        if (value == null) {
            value = (Serializable) Collections.emptyList();
        }
        if (!(value instanceof List)) {
            throw new UnsupportedOperationException(
                    "Cannot apply ListDiff on non-List: " + value);
        }
        @SuppressWarnings("unchecked")
        List<Serializable> list = (List<Serializable>) value;
        if (listDiff.diff != null) {
            int i = 0;
            for (Object diffElem : listDiff.diff) {
                if (i >= list.size()) {
                    // TODO log error applying diff to shorter list
                    break;
                }
                if (diffElem instanceof StateDiff) {
                    applyDiff((State) list.get(i), (StateDiff) diffElem);
                } else if (diffElem != NOP) {
                    list.set(i, deepCopy(diffElem));
                }
                i++;
            }
        }
        if (listDiff.rpush != null) {
            List<Serializable> newList = new ArrayList<>(list.size()
                    + listDiff.rpush.size());
            newList.addAll(list);
            for (Object v : listDiff.rpush) {
                newList.add(deepCopy(v));
            }
            list = newList;
        }
        if (listDiff.rpop) {
            list = new ArrayList<>(list.subList(0, list.size() - 1));
        }
        // convert back to array if needed
        if (listDiff.isArray) {
            return list.toArray(new Object[0]);
        } else {
            return (Serializable) list;
        }
    }

}

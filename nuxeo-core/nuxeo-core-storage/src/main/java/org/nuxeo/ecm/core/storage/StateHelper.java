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

import static org.nuxeo.ecm.core.storage.State.NOP;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.runtime.api.Framework;

/**
 * Helpers for deep copy and deep diff of {@link State} objects.
 */
public class StateHelper {

    private static final String DISABLED_DELTA_PROP = "org.nuxeo.core.delta.disabled";

    /** Utility class. */
    private StateHelper() {
    }

    /**
     * Checks if we have a base type compatible with {@link State} helper processing.
     */
    public static boolean isScalar(Object value) {
        return value instanceof String //
                || value instanceof Boolean //
                || value instanceof Long //
                || value instanceof Double //
                || value instanceof Calendar //
                || value instanceof Delta;
    }

    /**
     * Compares two values.
     */
    public static boolean equalsStrict(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (a instanceof State && b instanceof State) {
            return equalsStrict((State) a, (State) b);
        } else if (a instanceof List && b instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> la = (List<Serializable>) a;
            @SuppressWarnings("unchecked")
            List<Serializable> lb = (List<Serializable>) b;
            return equalsStrict(la, lb);
        } else if (a instanceof Object[] && b instanceof Object[]) {
            return equalsStrict((Object[]) a, (Object[]) b);
        } else if (a instanceof ListDiff && b instanceof ListDiff) {
            ListDiff lda = (ListDiff) a;
            ListDiff ldb = (ListDiff) b;
            return lda.isArray == ldb.isArray && equalsStrict(lda.diff, ldb.diff) && equalsStrict(lda.rpush, ldb.rpush);
        } else if (isScalar(a) && isScalar(b)) {
            return a.equals(b);
        } else {
            return false;
        }
    }

    /**
     * Compares two {@link State}s.
     */
    public static boolean equalsStrict(State a, State b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
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
            if (!equalsStrict(va, vb)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two arrays of scalars.
     */
    public static boolean equalsStrict(Object[] a, Object[] b) {
        // we have scalars, Arrays.equals() is enough
        return Arrays.equals(a, b);
    }

    /**
     * Compares two {@link List}s.
     */
    public static boolean equalsStrict(List<Serializable> a, List<Serializable> b) {
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
            if (!equalsStrict(ita.next(), itb.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two values.
     * <p>
     * A {@code null} value or an empty array or {@code List} is equivalent to an absent value. A {@code null}
     * {@link State} is equivalent to an empty {@link State} (or a {@link State} containing only absent values).
     */
    public static boolean equalsLoose(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a instanceof State && b instanceof State //
                || a instanceof State && b == null //
                || a == null && b instanceof State) {
            return equalsLoose((State) a, (State) b);
        } else if (a instanceof List && b instanceof List //
                || a instanceof List && b == null //
                || a == null && b instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> la = (List<Serializable>) a;
            @SuppressWarnings("unchecked")
            List<Serializable> lb = (List<Serializable>) b;
            return equalsLoose(la, lb);
        } else if (a instanceof Object[] && b instanceof Object[] //
                || a instanceof Object[] && b == null //
                || a == null && b instanceof Object[]) {
            return equalsLoose((Object[]) a, (Object[]) b);
        } else if (a instanceof ListDiff && b instanceof ListDiff) {
            ListDiff lda = (ListDiff) a;
            ListDiff ldb = (ListDiff) b;
            return lda.isArray == ldb.isArray && equalsLoose(lda.diff, ldb.diff) && equalsLoose(lda.rpush, ldb.rpush);
        } else if (isScalar(a) && isScalar(b)) {
            return a.equals(b); // NOSONAR
        } else {
            return false;
        }
    }

    /**
     * Compares two {@link State}s.
     * <p>
     * A {@code null} value or an empty array or {@code List} is equivalent to an absent value. A {@code null}
     * {@link State} is equivalent to an empty {@link State} (or a {@link State} containing only absent values).
     */
    public static boolean equalsLoose(State a, State b) {
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
            if (!equalsLoose(va, vb)) {
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
            if (!equalsLoose(null, vb)) {
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
    public static boolean equalsLoose(Object[] a, Object[] b) {
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
    public static boolean equalsLoose(List<Serializable> a, List<Serializable> b) {
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
            if (!equalsLoose(ita.next(), itb.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Makes a deep copy of a value.
     */
    public static Serializable deepCopy(Object value) {
        return deepCopy(value, false);
    }

    /**
     * Makes a deep copy of a value, optionally thread-safe.
     *
     * @param threadSafe if {@code true}, then thread-safe datastructures are used
     */
    public static Serializable deepCopy(Object value, boolean threadSafe) {
        if (value == null) {
            return (Serializable) value;
        } else if (value instanceof State) {
            return deepCopy((State) value, threadSafe);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) value;
            return (Serializable) deepCopy(list, threadSafe);
        } else if (value instanceof Object[]) {
            // array values are supposed to be scalars
            return ((Object[]) value).clone();
        }
        // else scalar value -- check anyway (debug)
        else if (!isScalar(value)) {
            throw new UnsupportedOperationException("Cannot deep copy: " + value.getClass().getName());
        }
        return (Serializable) value;
    }

    /**
     * Makes a deep copy of a {@link State} map.
     */
    public static State deepCopy(State state) {
        return deepCopy(state, false);
    }

    /**
     * Makes a deep copy of a {@link State} map, optionally thread-safe.
     *
     * @param threadSafe if {@code true}, then thread-safe datastructures are used
     */
    public static State deepCopy(State state, boolean threadSafe) {
        State copy = new State(state.size(), threadSafe);
        for (Entry<String, Serializable> en : state.entrySet()) {
            copy.put(en.getKey(), deepCopy(en.getValue(), threadSafe));
        }
        return copy;
    }

    /**
     * Makes a deep copy of a {@link List}.
     */
    public static List<Serializable> deepCopy(List<Serializable> list) {
        return deepCopy(list, false);
    }

    /**
     * Makes a deep copy of a {@link List}, optionally thread-safe.
     *
     * @param threadSafe if {@code true}, then thread-safe datastructures are used
     */
    public static List<Serializable> deepCopy(List<Serializable> list, boolean threadSafe) {
        List<Serializable> copy = threadSafe ? new CopyOnWriteArrayList<Serializable>() : new ArrayList<Serializable>(
                list.size());
        for (Serializable v : list) {
            copy.add(deepCopy(v, threadSafe));
        }
        return copy;
    }

    /**
     * Does a diff of two values.
     *
     * @return a {@link StateDiff}, a {@link ListDiff}, {@link #NOP}, or an actual value (including {@code null})
     */
    public static Serializable diff(Object a, Object b) {
        if (equalsLoose(a, b)) {
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
        // we can use a list diff if lists are the same size,
        // or we have a rpush
        boolean doDiff = aSize == bSize || doRPush;
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
        return listDiff;
    }

    /**
     * Makes a diff copy of two {@link State} maps.
     * <p>
     * The returned diff state contains only the key/values that changed. {@code null} values are equivalent to absent
     * values.
     * <p>
     * For values set to null or removed, the value is null.
     * <p>
     * When setting a delta, the old value is checked to know if the delta should be kept or if a full value should be
     * set instead.
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
                    if (elemDiff instanceof Delta) {
                        Delta delta = (Delta) elemDiff;
                        Serializable deltaBase = delta.getBase();
                        if (!Objects.equals(va, deltaBase)) {
                            // delta's base is not the old value
                            // -> set a new value, don't use a delta update
                            elemDiff = delta.getFullValue();
                        }
                        // else delta's base is the in-database value
                        // because base is consistent with old value, assume the delta is already properly computed
                    }
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
            if (!equalsLoose(null, vb)) {
                // value must be added
                diff.put(key, vb);
            }
        }
        return diff;
    }

    /**
     * Changes the deltas stored into actual full values.
     *
     * @since 6.0
     */
    public static void resetDeltas(State state) {
        if (Boolean.parseBoolean(Framework.getProperty(DISABLED_DELTA_PROP, "false"))) {
            return;
        }
        for (Entry<String, Serializable> en : state.entrySet()) {
            Serializable value = en.getValue();
            if (value instanceof State) {
                resetDeltas((State) value);
            } else if (value instanceof Delta) {
                state.put(en.getKey(), ((Delta) value).getFullValue());
            }
        }
    }

}

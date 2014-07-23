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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.storage.State.DiffOp.RPOP;
import static org.nuxeo.ecm.core.storage.State.DiffOp.RPUSH;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.State.Diff;

public class TestStateHelper {

    // more compact syntax
    private static final Object[] asArray(Object... objects) {
        return objects;
    }

    // de-genericize Arrays.asList to always return List<Serializable>
    private static final List<Serializable> asList(Serializable... strings) {
        return Arrays.asList(strings);
    }

    private static final Diff asDiff(Serializable... values) {
        assertTrue(values.length % 2 == 0);
        Diff diff = new Diff();
        for (int i = 0; i < values.length; i += 2) {
            diff.put((String) values[i], values[i + 1]);
        }
        return diff;
    }

    private static final State asState(Serializable... values) {
        return asDiff(values);
    }

    private static void assertEquals(Serializable a, Serializable b) {
        assertTrue(StateHelper.equals(a, b));
    }

    private static void assertNotEquals(Serializable a, Serializable b) {
        assertFalse(StateHelper.equals(a, b));
    }

    @Test
    public void testEquals() {
        // null
        assertEquals((Serializable) null, (Serializable) null);
        assertNotEquals((Serializable) null, "foo");

        // Serializable
        assertEquals("foo", "foo");
        assertNotEquals("foo", "bar");
        assertEquals(Long.valueOf(123456), Long.valueOf(123456));
        assertNotEquals(Long.valueOf(123456), Long.valueOf(789123));
        assertNotEquals("foo", Long.valueOf(123456));

        // mixed
        assertNotEquals("foo", new State());
        assertNotEquals("foo", new ArrayList<Serializable>());
        assertNotEquals(new State(), new ArrayList<Serializable>());

        // Arrays
        assertEquals(new String[] {}, new String[] {});
        assertEquals(new String[] { "foo" }, new String[] { "foo" });
        assertNotEquals(new String[] { "foo" }, new String[] { "bar" });

        // States
        State a = new State();
        State b = new State();
        assertEquals(a, b);
        a.put("foo", "bar");
        assertNotEquals(a, b);
        b.put("foo", "bar");
        assertEquals(a, b);
        b.put("foo", "moo");
        assertNotEquals(a, b);

        a.put("foo", new State());
        b.put("foo", new State());
        assertEquals(a, b);

        // Lists
        ArrayList<Serializable> la = new ArrayList<Serializable>();
        ArrayList<Serializable> lb = new ArrayList<Serializable>();
        assertEquals(la, lb);
        la.add(new State());
        assertNotEquals(la, lb);
        lb.add(new State());
        assertEquals(la, lb);
        ((State) la.get(0)).put("foo", "bar");
        assertNotEquals(la, lb);
        ((State) lb.get(0)).put("foo", "bar");
        assertEquals(la, lb);
        ((State) lb.get(0)).put("foo", "moo");
        assertNotEquals(la, lb);
    }

    private static void assertDiffArray(Object[] expected, Object[] a,
            Object[] b) {
        Object[] diff = StateHelper.diff(a, b);
        assertTrue(Arrays.equals(expected, diff));
    }

    private static void assertDiffList(List<Serializable> expected,
            List<Serializable> a, List<Serializable> b) {
        List<Serializable> diff = StateHelper.diff(a, b);
        assertEquals((Serializable) expected, (Serializable) diff);
    }

    private static void assertDiffState(Diff expected, State a, State b) {
        Diff diff = StateHelper.diff(a, b);
        assertEquals(expected, diff);
    }

    @Test
    public void testDiffArray() {
        assertDiffArray(asArray(), //
                asArray(), asArray());
        // overwrite
        assertDiffArray(asArray("B"), //
                asArray("A"), asArray("B"));
        assertDiffArray(asArray("B", "C"), //
                asArray("A"), asArray("B", "C"));
        // RPUSH
        assertDiffArray(asArray(RPUSH, "B"), //
                asArray("A"), asArray("A", "B"));
        assertDiffArray(asArray(RPUSH, "C", "D"), asArray("A", "B"),
                asArray("A", "B", "C", "D"));
        // overwrite for zero-length "a"
        assertDiffArray(asArray("A"), //
                asArray(), asArray("A"));
        assertDiffArray(asArray("A", "B"), //
                asArray(), asArray("A", "B"));
        // RPOP
        assertDiffArray(asArray(RPOP), //
                asArray("A", "B"), asArray("A"));
        assertDiffArray(asArray(RPOP), //
                asArray("A", "B", "C"), asArray("A", "B"));
        // overwrite for zero-length "b"
        assertDiffArray(asArray(), //
                asArray("A"), asArray());
    }

    @Test
    public void testDiffList() {
        assertDiffList(asList(), //
                asList(), asList());
        // overwrite
        assertDiffList(asList("B"), //
                asList("A"), asList("B"));
        assertDiffList(asList("B", "C"), //
                asList("A"), asList("B", "C"));
        // RPUSH
        assertDiffList(asList(RPUSH, "B"), //
                asList("A"), asList("A", "B"));
        assertDiffList(asList(RPUSH, "C", "D"), asList("A", "B"),
                asList("A", "B", "C", "D"));
        // overwrite for zero-length "a"
        assertDiffList(asList("A"), //
                asList(), asList("A"));
        assertDiffList(asList("A", "B"), //
                asList(), asList("A", "B"));
        // RPOP
        assertDiffList(asList(RPOP), //
                asList("A", "B"), asList("A"));
        assertDiffList(asList(RPOP), //
                asList("A", "B", "C"), asList("A", "B"));
        // overwrite for zero-length "b"
        assertDiffList(asList(), //
                asList("A"), asList());
    }

    @Test
    public void testDiffState() {
        assertDiffState(asDiff(), asState(), asState());

        // added keys
        assertDiffState(asDiff(), //
                asState("A", "B"), asState("A", "B"));
        assertDiffState(asDiff("A", "B"), //
                asState(), asState("A", "B"));
        assertDiffState(asDiff("C", "D"), //
                asState("A", "B"), asState("A", "B", "C", "D"));
        // removed keys
        assertDiffState(asDiff("A", null), //
                asState("A", "B"), asState());
        assertDiffState(asDiff("A", null), //
                asState("A", "B", "C", "D"), asState("C", "D"));

        // changed values
        assertDiffState(asDiff("A", "C"), //
                asState("A", "B"), asState("A", "C"));
        assertDiffState(asDiff("A", "C"), //
                asState("1", "2", "A", "B"), asState("1", "2", "A", "C"));
        // changed values which are diffs
        assertDiffState(asDiff("A", asArray(RPUSH, "C")), //
                asState("A", asArray("B")), asState("A", asArray("B", "C")));
        assertDiffState(asDiff("A", asArray(RPOP)), //
                asState("A", asArray("B", "C")), asState("A", asArray("B")));
        assertDiffState(
                asDiff("A", (Serializable) asList(RPUSH, "C")), //
                asState("A", (Serializable) asList("B")),
                asState("A", (Serializable) asList("B", "C")));
        assertDiffState(asDiff("A", asArray(RPOP)), //
                asState("A", asArray("B", "C")), asState("A", asArray("B")));
        assertDiffState(
                asDiff("A", asDiff("B", "D")), //
                asState("A", asState("1", "2", "B", "C")),
                asState("A", asState("1", "2", "B", "D")));
    }

    private static void assertApplyDiff(State expected, State state, Diff diff) {
        StateHelper.applyDiff(state, diff);
        assertEquals(expected, state);
    }

    @Test
    public void testApplyDiff() {
        assertApplyDiff(asState("A", "B"), //
                asState("A", "B"), //
                asDiff());
        assertApplyDiff(asState("A", "B"), //
                asState(), //
                asDiff("A", "B"));

        assertApplyDiff(asState("A", asState("B", "D")), //
                asState("A", asState("B", "C")), //
                asDiff("A", asDiff("B", "D")));

        assertApplyDiff(asState("A", asState("B", "C", "D", "E")), //
                asState("A", asState("B", "C")), //
                asDiff("A", asDiff("D", "E")));

        assertApplyDiff(asState("A", asArray("B", "C")), //
                asState("A", asArray("B")), //
                asDiff("A", asArray(RPUSH, "C")));
        assertApplyDiff(asState("A", asArray("B")), //
                asState("A", asArray("B", "C")), //
                asDiff("A", asArray(RPOP)));

        assertApplyDiff(asState("A", (Serializable) asList("B", "C")),
                asState("A", (Serializable) asList("B")),
                asDiff("A", (Serializable) asList(RPUSH, "C")));
        assertApplyDiff(asState("A", (Serializable) asList("B")),
                asState("A", (Serializable) asList("B", "C")),
                asDiff("A", (Serializable) asList(RPOP)));
    }

}

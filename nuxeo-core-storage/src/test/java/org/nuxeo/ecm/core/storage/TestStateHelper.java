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
import static org.nuxeo.ecm.core.storage.State.NOP;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;

public class TestStateHelper {

    // always return a List<Serializable> that is Serializable
    private static final ArrayList<Object> list(Object... strings) {
        return new ArrayList<>(Arrays.asList(strings));
    }

    private static final StateDiff stateDiff(Serializable... values) {
        assertTrue(values.length % 2 == 0);
        StateDiff diff = new StateDiff();
        for (int i = 0; i < values.length; i += 2) {
            diff.put((String) values[i], values[i + 1]);
        }
        return diff;
    }

    private static final ListDiff listDiff(List<Object> diff,
            List<Object> rpush, boolean rpop) {
        ListDiff listDiff = new ListDiff();
        listDiff.diff = diff;
        listDiff.rpush = rpush;
        listDiff.rpop = rpop;
        return listDiff;
    }

    private static final ListDiff listDiff(Object... diffs) {
        return listDiff(list(diffs), null, false);
    }

    private static final ListDiff rpush(Object... values) {
        return listDiff(null, list(values), false);
    }

    private static final ListDiff rpop() {
        return listDiff(null, null, true);
    }

    private static final State state(Serializable... values) {
        return stateDiff(values);
    }

    private static void assertEquals(Serializable a, Serializable b) {
        assertEquals(null, a, b);
    }

    private static void assertEquals(String message, Serializable a,
            Serializable b) {
        assertTrue(message, StateHelper.equals(a, b));
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

    private static void assertDiff(Serializable expected, Serializable a,
            Serializable b) {
        Serializable diff = StateHelper.diff(a, b);
        assertEquals(diff.toString(), expected, diff);
    }

    @Test
    public void testDiffList() {
        assertDiff(NOP, //
                list(), list());
        // overwrite
        assertDiff(list("B"), //
                list("A"), list("B"));
        assertDiff(list("B", "C"), //
                list("A"), list("B", "C"));
        // "RPUSH"
        assertDiff(rpush("B"), //
                list("A"), list("A", "B"));
        assertDiff(rpush("C", "D"), //
                list("A", "B"), list("A", "B", "C", "D"));
        // overwrite for zero-length "a"
        assertDiff(list("A"), //
                list(), list("A"));
        assertDiff(list("A", "B"), //
                list(), list("A", "B"));
        // "RPOP"
        assertDiff(rpop(), //
                list("A", "B"), list("A"));
        assertDiff(rpop(), //
                list("A", "B", "C"), list("A", "B"));
        // overwrite for zero-length "b"
        assertDiff(list(), //
                list("A"), list());
    }

    @Test
    public void testDiffListComplex() {
        assertDiff(NOP, //
                list(state("A", "B"), state("C", "D")), //
                list(state("A", "B"), state("C", "D")));
        assertDiff(rpush(state("C", "D")), //
                list(state("A", "B")), //
                list(state("A", "B"), state("C", "D")));

        // TODO check this
        assertDiff(listDiff(state("A", "B")), //
                list(state()), //
                list(state("A", "B")));

        assertDiff(
                listDiff(list(state("C", "D")), list(state("E", "F")), false), //
                list(state("A", "B")), //
                list(state("A", "B", "C", "D"), state("E", "F")));
    }

    @Test
    public void testDiffState() {
        assertDiff(NOP, state(), state());

        // added keys
        assertDiff(NOP, //
                state("A", "B"), state("A", "B"));
        assertDiff(stateDiff("A", "B"), //
                state(), state("A", "B"));
        assertDiff(stateDiff("C", "D"), //
                state("A", "B"), state("A", "B", "C", "D"));
        // removed keys
        assertDiff(stateDiff("A", null), //
                state("A", "B"), state());
        assertDiff(stateDiff("A", null), //
                state("A", "B", "C", "D"), state("C", "D"));

        // changed values
        assertDiff(stateDiff("A", "C"), //
                state("A", "B"), state("A", "C"));
        assertDiff(stateDiff("A", "C"), //
                state("1", "2", "A", "B"), state("1", "2", "A", "C"));
        // changed values which are diffs
        assertDiff(
                stateDiff("A", rpush("C")), //
                state("A", (Serializable) list("B")),
                state("A", (Serializable) list("B", "C")));
        assertDiff(stateDiff("A", rpop()), //
                state("A", list("B", "C")), state("A", list("B")));
        assertDiff(
                stateDiff("A", stateDiff("B", "D")), //
                state("A", state("1", "2", "B", "C")),
                state("A", state("1", "2", "B", "D")));
    }

    private static void assertApplyDiff(State expected, State state,
            StateDiff diff) {
        StateHelper.applyDiff(state, diff);
        assertEquals(state.toString(), expected, state);
    }

    @Test
    public void testApplyDiff() {
        assertApplyDiff(state("A", "B"), //
                state("A", "B"), //
                stateDiff());
        assertApplyDiff(state("A", "B"), //
                state(), //
                stateDiff("A", "B"));

        assertApplyDiff(state("A", state("B", "D")), //
                state("A", state("B", "C")), //
                stateDiff("A", stateDiff("B", "D")));

        assertApplyDiff(state("A", state("B", "C", "D", "E")), //
                state("A", state("B", "C")), //
                stateDiff("A", stateDiff("D", "E")));

        assertApplyDiff(state("A", list("B", "C")), //
                state("A", list("B")), //
                stateDiff("A", rpush("C")));
        assertApplyDiff(state("A", list("B")), //
                state("A", list("B", "C")), //
                stateDiff("A", rpop()));
    }

}

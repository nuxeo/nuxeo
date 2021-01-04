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

    private static final ListDiff listDiff(List<Object> diff, List<Object> rpush, List<Object> pull) {
        ListDiff listDiff = new ListDiff();
        listDiff.diff = diff;
        listDiff.rpush = rpush;
        listDiff.pull = pull;
        return listDiff;
    }

    private static final ListDiff listDiff(Object... diffs) {
        return listDiff(list(diffs), null, null);
    }

    private static final ListDiff rpush(Object... values) {
        return listDiff(null, list(values), null);
    }

    private static final ListDiff pull(Object... values) {
        return listDiff(null, null, list(values));
    }

    private static final State state(Serializable... values) {
        return stateDiff(values);
    }

    private static void assertEqualsStrict(Serializable a, Serializable b) {
        assertEqualsStrict(a + "!=" + b, a, b);
    }

    private static void assertEqualsStrict(String message, Serializable a, Serializable b) {
        assertTrue(message, StateHelper.equalsStrict(a, b));
    }

    private static void assertNotEqualsStrict(Serializable a, Serializable b) {
        assertFalse(StateHelper.equalsStrict(a, b));
    }

    private static void assertEqualsLoose(Serializable a, Serializable b) {
        assertTrue(a + "!=" + b, StateHelper.equalsLoose(a, b));
    }

    /**
     * StateHelper.equals is used internally for the tests.
     *
     * @since 5.9.5
     */
    @Test
    public void testEqualsStrict() {
        // null
        assertEqualsStrict((Serializable) null, (Serializable) null);
        assertNotEqualsStrict((Serializable) null, "foo");

        // Serializable
        assertEqualsStrict("foo", "foo");
        assertNotEqualsStrict("foo", "bar");
        assertEqualsStrict(Long.valueOf(123456), Long.valueOf(123456));
        assertNotEqualsStrict(Long.valueOf(123456), Long.valueOf(789123));
        assertNotEqualsStrict("foo", Long.valueOf(123456));

        // mixed
        assertNotEqualsStrict("foo", new State());
        assertNotEqualsStrict("foo", new ArrayList<Serializable>());
        assertNotEqualsStrict(new State(), new ArrayList<Serializable>());

        // Arrays
        assertEqualsStrict(new String[0], new String[0]);
        assertEqualsStrict(new String[] { "foo" }, new String[] { "foo" });
        assertNotEqualsStrict(new String[] { "foo" }, new String[] { "bar" });

        // States
        State a = new State();
        State b = new State();
        assertEqualsStrict(a, b);
        a.put("foo", "bar");
        assertNotEqualsStrict(a, b);
        b.put("foo", "bar");
        assertEqualsStrict(a, b);
        b.put("foo", "moo");
        assertNotEqualsStrict(a, b);

        a.put("foo", new State());
        b.put("foo", new State());
        assertEqualsStrict(a, b);

        // Lists
        ArrayList<Serializable> la = new ArrayList<Serializable>();
        ArrayList<Serializable> lb = new ArrayList<Serializable>();
        assertEqualsStrict(la, lb);
        la.add(new State());
        assertNotEqualsStrict(la, lb);
        lb.add(new State());
        assertEqualsStrict(la, lb);
        ((State) la.get(0)).put("foo", "bar");
        assertNotEqualsStrict(la, lb);
        ((State) lb.get(0)).put("foo", "bar");
        assertEqualsStrict(la, lb);
        ((State) lb.get(0)).put("foo", "moo");
        assertNotEqualsStrict(la, lb);
    }

    @Test
    public void testEqualsLoose() {
        // Arrays
        assertEqualsLoose(null, new String[0]);
        assertEqualsLoose(new String[0], null);

        // Lists
        assertEqualsLoose(null, new ArrayList<Serializable>());
        assertEqualsLoose(new ArrayList<Serializable>(), null);

        // States
        assertEqualsLoose(new State(), null);
        assertEqualsLoose(null, new State());

        State a = new State();
        State b = new State();
        assertEqualsLoose(a, b);

        a.put("foo", null);
        a.put("bar", null);
        assertEqualsLoose(a, b);

        a.put("foo", "bar");
        b.put("foo", "bar");
        assertEqualsLoose(a, b);

        b.put("gee", null);
        assertEqualsLoose(a, b);

        // empty elements considered null
        State c = new State();
        assertEqualsLoose(c, null);
        c.put("foo", list());
        assertEqualsLoose(c, null);
    }

    private static void assertDiff(Serializable expected, Serializable a, Serializable b) {
        Serializable diff = StateHelper.diff(a, b);
        assertEqualsStrict(String.valueOf(diff), expected, diff);
    }

    @Test
    public void testDiffList() {
        assertDiff(NOP, //
                null, null);
        assertDiff(NOP, //
                list(), list());
        // overwrite
        assertDiff(list("B"), //
                list("A"), list("B"));
        assertDiff(list("B", "C"), //
                list("A"), list("B", "C"));
        // "RPUSH"
        assertDiff(rpush("A"), //
                null, list("A"));
        assertDiff(rpush("A"), //
                list(), list("A"));
        assertDiff(rpush("A", "B"), //
                null, list("A", "B"));
        assertDiff(rpush("A", "B"), //
                list(), list("A", "B"));
        assertDiff(rpush("B"), //
                list("A"), list("A", "B"));
        assertDiff(rpush("C", "D"), //
                list("A", "B"), list("A", "B", "C", "D"));
        // "PULL"
        assertDiff(pull("A"), //
                list("A", "B"), list("B"));
        assertDiff(pull("A"), //
                list("B", "A"), list("B"));
        assertDiff(pull("A"), //
                list("A", "B", "A"), list("B"));
        assertDiff(pull("B"), //
                list("A", "B", "C"), list("A", "C"));
        // PULL of more than one element
        assertDiff(pull("A", "B"), //
                list("A", "B", "C", "B", "A"), list("C"));
        // PULL even for zero-length "b"
        assertDiff(pull("A"), //
                list("A"), null);
        assertDiff(pull("A"), //
                list("A", "A"), null);
        assertDiff(pull("A"), //
                list("A"), list());
        assertDiff(pull("A"), //
                list("A", "A"), list());
        // not a PULL (PULL removes all instances of the pulled value)
        assertDiff(list("A", "B"), //
                list("A", "B", "A"), list("A", "B"));
    }

    @Test
    public void testDiffListComplex() {
        assertDiff(NOP, //
                list(state("A", "B"), state("C", "D")), //
                list(state("A", "B"), state("C", "D")));
        assertDiff(rpush(state("A", "B")), //
                null, //
                list(state("A", "B")));
        assertDiff(rpush(state("A", "B")), //
                null, //
                list(state("A", "B")));
        assertDiff(rpush(state("A", "B")), //
                list(), //
                list(state("A", "B")));
        assertDiff(rpush(state("C", "D")), //
                list(state("A", "B")), //
                list(state("A", "B"), state("C", "D")));

        // TODO check this
        assertDiff(listDiff(state("A", "B")), //
                list(state()), //
                list(state("A", "B")));

        assertDiff(listDiff(list(state("C", "D")), list(state("E", "F")), null), //
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
        assertDiff(stateDiff("A", rpush("B")), //
                state(), state("A", list("B")));
        assertDiff(stateDiff("A", rpush("C")), //
                state("A", (Serializable) list("B")), state("A", (Serializable) list("B", "C")));
        assertDiff(stateDiff("A", stateDiff("B", "D")), //
                state("A", state("1", "2", "B", "C")), state("A", state("1", "2", "B", "D")));
    }

    @Test
    public void testDiffStateNulls() {
        assertDiff(NOP, state("X", null), state());
        assertDiff(NOP, state(), state("X", null));

        assertDiff(stateDiff("A", "B"), //
                state("X", null), state("A", "B"));
        assertDiff(stateDiff("A", "B"), //
                state(), state("A", "B", "X", null));
    }

}

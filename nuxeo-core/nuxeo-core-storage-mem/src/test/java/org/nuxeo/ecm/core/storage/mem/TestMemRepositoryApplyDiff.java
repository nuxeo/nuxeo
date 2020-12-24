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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mem;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.StateHelper;

public class TestMemRepositoryApplyDiff {

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

    private static final ListDiff rpush(Object... values) {
        return listDiff(null, list(values), null);
    }

    private static final State state(Serializable... values) {
        return stateDiff(values);
    }

    private static void assertEqualsStrict(String message, Serializable a, Serializable b) {
        assertTrue(message, StateHelper.equalsStrict(a, b));
    }

    private static void assertApplyDiff(State expected, State state, StateDiff diff) {
        MemRepository.applyDiff(state, diff);
        assertEqualsStrict(state.toString(), expected, state);
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
    }

    @Test
    public void testCopiesValues() {
        ArrayList<Object> list1 = list("L1");
        State state = state("A", list1);
        ArrayList<Object> list2 = list("L2");
        MemRepository.applyDiff(state, (StateDiff) state("A", list2));
        // make sure we don't end up with the actual list2 in the new state, but with a copy
        Serializable list3 = state.get("A");
        assertEqualsStrict("Should be equal", list2, list3);
        assertNotSame(list2, list3);
    }

    @Test
    public void testCopiesListDiffDiff() {
        ArrayList<Object> list = list(list("L1"));
        ArrayList<Object> list1 = list("L2");
        MemRepository.applyDiff(list, listDiff(list(list1), null, null)); // diff
        // make sure we don't end up with the actual list1 in the new list, but with a copy
        Serializable list2 = (Serializable) list.get(0);
        assertEqualsStrict("Should be equal", list1, list2);
        assertNotSame(list1, list2);
    }

    @Test
    public void testCopiesListDiffRpush() {
        ArrayList<Object> list = list();
        ArrayList<Object> list1 = list("L1");
        MemRepository.applyDiff(list, listDiff(null, list(list1), null)); // rpush
        // make sure we don't end up with the actual list1 in the new list, but with a copy
        Serializable list2 = (Serializable) list.get(0);
        assertEqualsStrict("Should be equal", list1, list2);
        assertNotSame(list1, list2);
    }

    @Test
    public void testCopiesArrayDiffRpushOnNull() {
        ArrayList<Object> list = list("L1");
        ListDiff listDiff = listDiff(null, list, null);
        listDiff.isArray = true;
        Serializable res = MemRepository.applyDiff(null, listDiff); // rpush
        assertTrue(res.getClass().toString(), res instanceof String[]);
        String[] array = (String[]) res;
        assertArrayEquals(list.toArray(), array);
    }

    @Test
    public void testCopiesListDiffPull() {
        ArrayList<Object> list = list("A", "B", "C", "B", "A");
        MemRepository.applyDiff(list, listDiff(null, null, list("A", "B"))); // pull
        assertEqualsStrict("Should be equal", (Serializable) Arrays.asList("C"), list);
    }

}

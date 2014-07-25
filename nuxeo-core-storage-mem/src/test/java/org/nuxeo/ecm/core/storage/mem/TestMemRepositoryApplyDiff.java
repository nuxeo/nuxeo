/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mem;

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

    private static final ListDiff listDiff(List<Object> diff, List<Object> rpush) {
        ListDiff listDiff = new ListDiff();
        listDiff.diff = diff;
        listDiff.rpush = rpush;
        return listDiff;
    }

    private static final ListDiff rpush(Object... values) {
        return listDiff(null, list(values));
    }

    private static final State state(Serializable... values) {
        return stateDiff(values);
    }

    private static void assertEqualsStrict(String message, Serializable a,
            Serializable b) {
        assertTrue(message, StateHelper.equalsStrict(a, b));
    }

    private static void assertApplyDiff(State expected, State state,
            StateDiff diff) {
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

}

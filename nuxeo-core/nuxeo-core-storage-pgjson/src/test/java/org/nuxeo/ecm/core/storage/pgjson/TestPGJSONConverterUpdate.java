/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.pgjson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.storage.State.NOP;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_JSON;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.pgjson.PGJSONConverter.UpdateBuilder;
import org.nuxeo.ecm.core.storage.pgjson.PGType.PGTypeAndValue;

public class TestPGJSONConverterUpdate {

    // always return a List<Serializable> that is Serializable
    private static final ArrayList<Object> list(Object... strings) {
        return new ArrayList<>(Arrays.asList(strings));
    }

    private static final State state(Serializable... values) {
        assertTrue(values.length % 2 == 0);
        State state = new State();
        for (int i = 0; i < values.length; i += 2) {
            state.put((String) values[i], values[i + 1]);
        }
        return state;
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

    private static final ListDiff listDiff(Object... diffs) {
        return listDiff(list(diffs), null);
    }

    private static final ListDiff rpush(Object... values) {
        return listDiff(null, list(values));
    }

    @Test
    public void testUpdateStateDiff() {
        // toplevel update

        // A: V
        //
        // jsonb_set(DOC, '{A}', '"V"')
        // DOC || '{"A":"V"}'
        assertUpdate(stateDiff("A", "V"), "DOC || '{\"A\":\"V\"}'");
        assertUpdate(stateDiff("A", Long.valueOf(123)), "DOC || '{\"A\":123}'");
        assertUpdate(stateDiff("A", list("V", "W")), "DOC || '{\"A\":[\"V\",\"W\"]}'");
        assertUpdate(stateDiff("A", state("B", "V")), "DOC || '{\"A\":{\"B\":\"V\"}}'");

        // A: V
        // B: W
        //
        // jsonb_set(jsonb_set(DOC, '{A}', '"V"'), '{B}', '"W"')
        // DOC || '{"A":"V","B":"W"}'
        assertUpdate( //
                stateDiff("A", "V", "B", "W"), //
                "DOC || '{\"A\":\"V\",\"B\":\"W\"}'");

        // toplevel removal

        // A: removed
        //
        // DOC - 'A'
        assertUpdate(stateDiff("A", null), "DOC - 'A'");

        // A: removed
        // B: removed
        //
        // DOC - '{A,B}'
        assertUpdate(stateDiff("A", null, "B", null), "DOC - '{A,B}'");

        // toplevel update + removal

        // A: V
        // B: W
        // C: removed
        // D: removed
        //
        // DOC || '{"A":"V", "B":"W"}' - '{C,D}'
        assertUpdate( //
                stateDiff("A", "V", "B", "W", "C", null, "D", null), //
                "DOC || '{\"A\":\"V\",\"B\":\"W\"}' - '{C,D}'");

        // recursion

        // A.B: V
        //
        // jsonb_set(DOC, '{A,B}', '"V"')
        // jsonb_set(DOC, '{A}', (DOC->'A') || '{"B":"V"}')
        assertUpdate( //
                stateDiff("A", stateDiff("B", "V")), //
                "jsonb_set(DOC, '{A}', (DOC->'A') || '{\"B\":\"V\"}')");

        // A.B: removed
        //
        // jsonb_set(DOC, '{A}', (DOC->'A') - 'B')
        assertUpdate( //
                stateDiff("A", stateDiff("B", null)), //
                "jsonb_set(DOC, '{A}', (DOC->'A') - 'B')");

        // A.B: V
        // A.C: removed
        //
        // jsonb_set(DOC, '{A}', (DOC->'A') || '{"B":"V"}' - 'C')
        assertUpdate( //
                stateDiff("A", stateDiff("B", "V", "C", null)), //
                "jsonb_set(DOC, '{A}', (DOC->'A') || '{\"B\":\"V\"}' - 'C')");

        // A.B: V
        // C: W
        //
        // jsonb_set(DOC, '{A,B}', '"V"') || '{"C":"W"}')
        // jsonb_set(DOC, '{A}', (DOC->'A') || '{"B":"V"}') || '{"C":"W"}'
        assertUpdate( //
                stateDiff("A", stateDiff("B", "V"), "C", "W"), //
                "jsonb_set(DOC, '{A}', (DOC->'A') || '{\"B\":\"V\"}') || '{\"C\":\"W\"}'");

        // A.B: V
        // A.C: W
        //
        // jsonb_set(jsonb_set(DOC, '{A,B}', '"V"'), '{A,C}', '"W"')
        // jsonb_set(DOC, '{A}', (DOC->'A') || '{"B":"V", "C":"W"}')
        assertUpdate( //
                stateDiff("A", stateDiff("B", "V", "C", "W")), //
                "jsonb_set(DOC, '{A}', (DOC->'A') || '{\"B\":\"V\",\"C\":\"W\"}')");

        // A.B: V
        // A.C: W
        // A.D: X
        //
        // jsonb_set(jsonb_set(jsonb_set(DOC, '{A,B}', '"V"'), '{A,C}', '"W"'), '{A,D}', '"X"')
        // jsonb_set(DOC, '{A}', (DOC->'A') || '{"B":"V", "C":"W", "D":"X"}')
        assertUpdate( //
                stateDiff("A", stateDiff("B", "V", "C", "W", "D", "X")), //
                "jsonb_set(DOC, '{A}', (DOC->'A') || '{\"B\":\"V\",\"C\":\"W\",\"D\":\"X\"}')");

        // A.B: V
        // C.D: W
        //
        // jsonb_set(jsonb_set(DOC, '{A,B}', '"V"'), '{C,D}', '"W"')
        // jsonb_set(jsonb_set(DOC, '{A}', (DOC->'A') || '{"B":"V"}'), '{C}', (DOC->'C') || '{"D":"W"}')
        assertUpdate( //
                stateDiff("A", stateDiff("B", "V"), "C", stateDiff("D", "W")), //
                "jsonb_set(jsonb_set(DOC, '{A}', (DOC->'A') || '{\"B\":\"V\"}'), '{C}', (DOC->'C') || '{\"D\":\"W\"}')");

        // A.B.C: V
        //
        // jsonb_set(DOC, '{A,B,C}', '"V"')
        // jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', (DOC->'A'->'B') || '{"C":"V"}'))
        assertUpdate( //
                stateDiff("A", stateDiff("B", stateDiff("C", "V"))), //
                "jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', (DOC->'A'->'B') || '{\"C\":\"V\"}'))");

        // A.B: V
        // A.C.D: W
        //
        // jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{C}', (DOC->'A'->'C') || '{"D":"W"}') || '{"B":"V"}')

        assertUpdate( //
                stateDiff("A", stateDiff("B", "V", "C", stateDiff("D", "W"))), //
                "jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{C}', (DOC->'A'->'C') || '{\"D\":\"W\"}') || '{\"B\":\"V\"}')");

        // A.B: V
        // A.C.D: W
        // A.E.F: X
        //
        // jsonb_set(DOC, '{A}', jsonb_set(jsonb_set(DOC->'A', '{C}', (DOC->'A'->'C') || '{"D":"W"}'), '{E}',
        // (DOC->'A'->'E') || '{"F":"X"}') || '{"B":"V"}')
        assertUpdate( //
                stateDiff("A", stateDiff("B", "V", "C", stateDiff("D", "W"), "E", stateDiff("F", "X"))), //
                "jsonb_set(DOC, '{A}', jsonb_set(jsonb_set(DOC->'A', '{C}', (DOC->'A'->'C') || '{\"D\":\"W\"}'), '{E}', (DOC->'A'->'E') || '{\"F\":\"X\"}') || '{\"B\":\"V\"}')");

        // removal

        // A.B: removed
        //
        // jsonb_set(DOC, '{A}', (DOC->'A') - 'B')
        assertUpdate( //
                stateDiff("A", stateDiff("B", null)), //
                "jsonb_set(DOC, '{A}', (DOC->'A') - 'B')");

        // A.B.C: removed
        //
        // jsonb_set(DOC, '{A,B}', (DOC#>'{A,B}') - 'C')
        // jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', (DOC->'A'->'B') - 'C'))
        assertUpdate( //
                stateDiff("A", stateDiff("B", stateDiff("C", null))), //
                "jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', (DOC->'A'->'B') - 'C'))");

        // A.B: removed
        // A.C: removed
        //
        // jsonb_set(DOC, '{A}', (DOC->'A') - '{B,C}')
        assertUpdate( //
                stateDiff("A", stateDiff("B", null, "C", null)), //
                "jsonb_set(DOC, '{A}', (DOC->'A') - '{B,C}')");

        // A.B.C: removed
        // A.B.D: removed
        //
        // jsonb_set(DOC, '{A,B}', (DOC#>'{A,B}') - '{C,D}')
        // jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', (DOC->'A'->'B') - '{C,D}'))
        assertUpdate( //
                stateDiff("A", stateDiff("B", stateDiff("C", null, "D", null))), //
                "jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', (DOC->'A'->'B') - '{C,D}'))");

        // A.B: removed
        // A.C: V
        //
        // jsonb_set(DOC, '{A}', (DOC->'A') || '{"C":"V"}' - 'B')
        assertUpdate( //
                stateDiff("A", stateDiff("B", null, "C", "V")), //
                "jsonb_set(DOC, '{A}', (DOC->'A') || '{\"C\":\"V\"}' - 'B')");

        // A.B.C: removed
        // A.B.D: V
        //
        // jsonb_set(DOC, '{A,B}', (DOC#>'{A,B}') || '{"D":"V"}' - 'C')
        // jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', (DOC->'A'->'B') || '{"D":"V"}' - 'C'))
        assertUpdate( //
                stateDiff("A", stateDiff("B", stateDiff("C", null, "D", "V"))), //
                "jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', (DOC->'A'->'B') || '{\"D\":\"V\"}' - 'C'))");

        // more general case
        // A: B
        // C: removed
        // D.E: F
        // D.G: removed
        // D.H.I: J
        // D.H.K: L
        //
        // jsonb_set(DOC, '{D}', jsonb_set(DOC->'D', '{H}', (DOC->'D'->'H') || '{"I":"J","K":"L"}')
        // || '{"E":"F"}' - 'G') || '{"A":"B"}' - 'C'
        assertUpdate( //
                stateDiff("A", "B", "C", null, "D", stateDiff("E", "F", "G", null, "H", stateDiff("I", "J", "K", "L"))), //
                "jsonb_set(DOC, '{D}', jsonb_set(DOC->'D', '{H}', (DOC->'D'->'H') || '{\"I\":\"J\",\"K\":\"L\"}')"
                        + " || '{\"E\":\"F\"}' - 'G') || '{\"A\":\"B\"}' - 'C'");
    }

    @Test
    public void testUpdateListDiff() {
        // toplevel update

        // ListDiff[V]
        // jsonb_set(DOC, '{0}', '"V"')
        assertUpdate(listDiff("V"), "jsonb_set(DOC, '{0}', '\"V\"')");

        // ListDiff[NOP,V,NOP,W]
        // jsonb_set(jsonb_set(DOC, '{1}', '"V"'), '{2}', '"W"')
        assertUpdate( //
                listDiff(NOP, "V", NOP, "W"), //
                "jsonb_set(jsonb_set(DOC, '{1}', '\"V\"'), '{3}', '\"W\"')");

        // mixed

        // A: ListDiff[V]
        //
        // jsonb_set(DOC, '{A,0}', '"V"')
        // jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{0}', '"V"'))
        assertUpdate( //
                stateDiff("A", listDiff("V")), //
                "jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{0}', '\"V\"'))");

        // A.B: ListDiff[NOP,StateDiff{C: V}]
        //
        // jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', jsonb_set(DOC->'A'->'B', '{1}',
        // (DOC->'A'->'B'->1) || '{"C":"V"}')))
        assertUpdate( //
                stateDiff("A", stateDiff("B", listDiff(NOP, stateDiff("C", "V")))), //
                "jsonb_set(DOC, '{A}', jsonb_set(DOC->'A', '{B}', jsonb_set(DOC->'A'->'B', '{1}',"
                        + " (DOC->'A'->'B'->1) || '{\"C\":\"V\"}')))");
    }

    protected static void assertUpdate(Serializable diff, String expected) {
        UpdateBuilder builder = new PGJSONConverter.UpdateBuilder();
        String actual = builder.build("DOC", TYPE_JSON, diff);
        actual = replacePreparedStatementParams(actual, builder.values);
        assertEquals(expected, actual);
    }

    protected static String replacePreparedStatementParams(String actual, List<PGTypeAndValue> values) {
        PGJSONConverter converter = new PGJSONConverter(null);
        final String PARAM = "?::jsonb";
        // replace params with values
        while (actual.contains(PARAM) && !values.isEmpty()) {
            PGTypeAndValue value = values.remove(0);
            String json = converter.valueToJson(value.value);
            actual = actual.replaceFirst(Pattern.quote(PARAM), "'" + json + "'");
        }
        if (actual.contains(PARAM)) {
            fail("Expected extra values: " + actual);
        } else if (!values.isEmpty()) {
            fail("Extra values after: " + actual + ", " + values);
        }
        actual = actual.replace("::text[]", ""); // easier to read in tests
        return actual;
    }

}

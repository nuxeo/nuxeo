/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDBSExpressionEvaluator extends NXRuntimeTestCase {

    // always return a List<Serializable> that is Serializable
    private static final ArrayList<Object> list(Object... values) {
        return new ArrayList<>(Arrays.asList(values));
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

    private static final State state(Serializable... values) {
        return stateDiff(values);
    }

    private static final Map<String, Serializable> map(Serializable... values) {
        assertTrue(values.length % 2 == 0);
        Map<String, Serializable> map = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put((String) values[i], values[i + 1]);
        }
        return map;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.storage.dbs.tests", "OSGI-INF/test-complex.xml");
    }

    @Test
    public void testMatch() throws Exception {
        SQLQuery query = SQLQueryParser.parse("SELECT ecm:uuid, cmp:addresses/*1/street FROM D WHERE " //
                + "cmp:addresses/*1/city = 'Paris'");
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, query, null, false);
        evaluator.parse();
        assertTrue(evaluator.hasWildcardProjection());
        State state = state(//
                "ecm:id", "id1", //
                "cmp:addresses",
                list( //
                        state("city", "Paris", "street", "Champs Elysees"), //
                        state("city", "Paris", "street", "Boulevard Peripherique")));
        List<Map<String, Serializable>> projections = evaluator.matches(state);
        assertEquals(
                list( //
                        map("ecm:uuid", "id1", "cmp:addresses/*1/street", "Champs Elysees"), //
                        map("ecm:uuid", "id1", "cmp:addresses/*1/street", "Boulevard Peripherique")), //
                projections);
    }

    @Test
    public void testMatch2() throws Exception {
        SQLQuery query = SQLQueryParser.parse("SELECT ecm:uuid, cmp:addresses/*1/street FROM D WHERE "
                + "cmp:addresses/*1/city = 'Paris' OR cmp:addresses/*1/number = 1");
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, query, null, false);
        evaluator.parse();
        assertTrue(evaluator.hasWildcardProjection());
        State state = state(//
                "ecm:id", "id1", //
                "cmp:addresses",
                list( //
                        state("city", "New York", "street", "Broadway", "number", 1L), //
                        state("city", "Paris", "street", "Champs Elysees", "number", 1L), //
                        state("city", "Paris", "street", "Boulevard Peripherique", "number", 2L)));
        List<Map<String, Serializable>> projections = evaluator.matches(state);
        assertEquals(
                list( //
                        map("ecm:uuid", "id1", "cmp:addresses/*1/street", "Broadway"), //
                        map("ecm:uuid", "id1", "cmp:addresses/*1/street", "Champs Elysees"), //
                        map("ecm:uuid", "id1", "cmp:addresses/*1/street", "Boulevard Peripherique")), //
                projections);
    }

    @Test
    public void testWildcardCrossProduct() throws Exception {
        SQLQuery query = SQLQueryParser.parse("SELECT cmp:addresses/*1/city, cmp:addresses/*2/city FROM D WHERE " //
                + "ecm:uuid <> 'nothing'");
        DBSExpressionEvaluator evaluator = new DBSExpressionEvaluator(null, query, null, false);
        evaluator.parse();
        assertTrue(evaluator.hasWildcardProjection());
        State state = state(//
                "ecm:id", "id1", //
                "cmp:addresses",
                list( //
                        state("city", "Paris"), //
                        state("city", "London")));
        List<Map<String, Serializable>> projections = evaluator.matches(state);
        assertEquals(
                list( //
                        map("cmp:addresses/*1/city", "Paris", "cmp:addresses/*2/city", "Paris"), //
                        map("cmp:addresses/*1/city", "Paris", "cmp:addresses/*2/city", "London"), //
                        map("cmp:addresses/*1/city", "London", "cmp:addresses/*2/city", "Paris"), //
                        map("cmp:addresses/*1/city", "London", "cmp:addresses/*2/city", "London")), //
                projections);
    }

}

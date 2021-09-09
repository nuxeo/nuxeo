/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.storage.State.NOP;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_SET;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_UNSET;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.ONE;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.bson.Document;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.mongodb.MongoDBConverter.ConditionsAndUpdates;

/**
 * @since 2021.9
 */
public class TestMongoDBConverterUpdateBuilder {

    @Test
    public void testUpdateStateDiff() {
        var docDiff = newStateDiff("primitive", "value");

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_SET));
        var mongodbSet = (Document) update.get(MONGODB_SET);
        assertEquals("value", mongodbSet.get("primitive"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(0, conditions.size());
    }

    @Test
    public void testUpdateStateDiffUnset() {
        var docDiff = newStateDiff("primitive", null);

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_UNSET));
        var mongodbUnset = (Document) update.get(MONGODB_UNSET);
        assertEquals(ONE, mongodbUnset.get("primitive"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(0, conditions.size());
    }

    @Test
    public void testUpdateListDiff() {
        var docDiff = newStateDiff("array", newListDiffDiff("value0"));

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_SET));
        var mongodbSet = (Document) update.get(MONGODB_SET);
        assertEquals("value0", mongodbSet.get("array.0"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(Set.of("array"), conditions.keySet());
    }

    @Test
    public void testUpdateListDiffStateDiff() {
        var docDiff = newStateDiff("array", newListDiffDiff(newStateDiff("key", "value")));

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_SET));
        var mongodbSet = (Document) update.get(MONGODB_SET);
        assertEquals("value", mongodbSet.get("array.0.key"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(Set.of("array"), conditions.keySet());
    }

    @Test
    public void testUpdateListDiffStateDiffListDiffStateDiff() {
        var docDiff = newStateDiff("array",
                newListDiffDiff(newStateDiff("array", newListDiffDiff(NOP, newStateDiff("key", "value")))));

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_SET));
        var mongodbSet = (Document) update.get(MONGODB_SET);
        assertEquals("value", mongodbSet.get("array.0.array.1.key"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(2, conditions.size());
        assertEquals(Set.of("array", "array.0.array"), conditions.keySet());
    }

    // conflict tests extracted form former TestMongoUpdateConflict

    @Test
    public void testComplexPropNoConflict() throws Exception {
        var docDiff = newStateDiff("foo", newListDiffDiff(newStateDiff("bar", "val", "zoo", "val")));

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(Set.of("foo"), conditions.keySet());
    }

    @Test
    public void testComplexPropNoConflict2() throws Exception {
        var docDiff = newStateDiff("foo", newListDiffDiff(newStateDiff("zoo", newStateDiff("x", "val", "y", "val"))));

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(Set.of("foo"), conditions.keySet());
    }

    @Test
    public void testComplexPropNoConflict3() throws Exception {
        var docDiff = newStateDiff("foo",
                newListDiffDiff(newStateDiff("bar", newStateDiff("zoo", newStateDiff("x", "val"), "z", "val"))));

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(Set.of("foo"), conditions.keySet());
    }

    @Test
    public void testConflict1() throws Exception {
        var listDiff = new ListDiff();
        listDiff.diff = List.of(newStateDiff("bar", "val"));
        listDiff.rpush = List.of(newStateDiff("bar", "val2"));
        var docDiff = newStateDiff("foo", listDiff);

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(2, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(Set.of("foo"), conditions.keySet());
    }

    @Test
    public void testConflict2() throws Exception {
        var listDiff = new ListDiff();
        listDiff.diff = List.of(newStateDiff("bar", "val"));
        listDiff.pull = List.of(newStateDiff("bar", "val2"));
        var docDiff = newStateDiff("foo", listDiff);

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(2, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(Set.of("foo"), conditions.keySet());
    }

    @Test
    public void testConflict3() throws Exception {
        var listDiff = new ListDiff();
        listDiff.rpush = List.of(newStateDiff("bar", "val"));
        listDiff.pull = List.of(newStateDiff("bar", "val2"));
        var docDiff = newStateDiff("foo", listDiff);

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(2, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(0, conditions.size());
    }

    @Test
    public void testBigComplex() throws Exception {
        var docDiff = new StateDiff();
        IntStream.range(0, 10_000).forEach(i -> docDiff.put("foo." + i + ".bar.zoo", "val"));

        var ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(0, conditions.size());
    }

    protected StateDiff newStateDiff(String key, Serializable value) {
        var stateDiff = new StateDiff();
        stateDiff.put(key, value);
        return stateDiff;
    }

    protected StateDiff newStateDiff(String key1, Serializable value1, String key2, Serializable value2) {
        var stateDiff = newStateDiff(key1, value1);
        stateDiff.put(key2, value2);
        return stateDiff;
    }

    protected ListDiff newListDiffDiff(Object... diffs) {
        var list = new ListDiff();
        list.diff = List.of(diffs);
        return list;
    }
}

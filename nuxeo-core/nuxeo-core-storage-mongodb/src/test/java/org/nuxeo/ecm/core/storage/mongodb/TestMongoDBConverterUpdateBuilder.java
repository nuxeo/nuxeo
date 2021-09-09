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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.storage.State.NOP;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_SET;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_UNSET;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.ONE;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import org.bson.Document;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.mongodb.MongoDBConverter.ConditionsAndUpdates;
import org.nuxeo.ecm.core.storage.mongodb.MongoDBConverter.UpdateBuilder;

/**
 * @since 2021.9
 */
public class TestMongoDBConverterUpdateBuilder {

    @Test
    public void testUpdateStateDiff() {
        StateDiff docDiff = newStateDiff("primitive", "value");

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_SET));
        Document mongodbSet = (Document) update.get(MONGODB_SET);
        assertEquals("value", mongodbSet.get("primitive"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(0, conditions.size());
    }

    @Test
    public void testUpdateStateDiffUnset() {
        StateDiff docDiff = newStateDiff("primitive", null);

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_UNSET));
        Document mongodbUnset = (Document) update.get(MONGODB_UNSET);
        assertEquals(ONE, mongodbUnset.get("primitive"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(0, conditions.size());
    }

    @Test
    public void testUpdateListDiff() {
        StateDiff docDiff = newStateDiff("array", newListDiffDiff("value0"));

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_SET));
        Document mongodbSet = (Document) update.get(MONGODB_SET);
        assertEquals("value0", mongodbSet.get("array.0"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(singleton("array"), conditions.keySet());
    }

    @Test
    public void testUpdateListDiffStateDiff() {
        StateDiff docDiff = newStateDiff("array", newListDiffDiff(newStateDiff("key", "value")));

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_SET));
        Document mongodbSet = (Document) update.get(MONGODB_SET);
        assertEquals("value", mongodbSet.get("array.0.key"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(singleton("array"), conditions.keySet());
    }

    @Test
    public void testUpdateListDiffStateDiffListDiffStateDiff() {
        StateDiff docDiff = newStateDiff("array",
                newListDiffDiff(newStateDiff("array", newListDiffDiff(NOP, newStateDiff("key", "value")))));

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());
        Document update = updates.get(0);
        assertEquals(1, update.size());
        assertTrue(update.containsKey(MONGODB_SET));
        Document mongodbSet = (Document) update.get(MONGODB_SET);
        assertEquals("value", mongodbSet.get("array.0.array.1.key"));

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(2, conditions.size());
        assertEquals(new HashSet<>(asList("array", "array.0.array")), conditions.keySet());
    }

    // conflict tests extracted form former TestMongoUpdateConflict

    @Test
    public void testComplexPropNoConflict() throws Exception {
        StateDiff docDiff = newStateDiff("foo", newListDiffDiff(newStateDiff("bar", "val", "zoo", "val")));

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(singleton("foo"), conditions.keySet());
    }

    @Test
    public void testComplexPropNoConflict2() throws Exception {
        StateDiff docDiff = newStateDiff("foo", newListDiffDiff(newStateDiff("zoo", newStateDiff("x", "val", "y", "val"))));

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(singleton("foo"), conditions.keySet());
    }

    @Test
    public void testComplexPropNoConflict3() throws Exception {
        StateDiff docDiff = newStateDiff("foo",
                newListDiffDiff(newStateDiff("bar", newStateDiff("zoo", newStateDiff("x", "val"), "z", "val"))));

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(singleton("foo"), conditions.keySet());
    }

    @Test
    public void testConflict1() throws Exception {
        ListDiff listDiff = new ListDiff();
        listDiff.diff = singletonList(newStateDiff("bar", "val"));
        listDiff.rpush = singletonList(newStateDiff("bar", "val2"));
        StateDiff docDiff = newStateDiff("foo", listDiff);

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(2, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(singleton("foo"), conditions.keySet());
    }

    @Test
    public void testConflict2() throws Exception {
        ListDiff listDiff = new ListDiff();
        listDiff.diff = singletonList(newStateDiff("bar", "val"));
        listDiff.pull = singletonList(newStateDiff("bar", "val2"));
        StateDiff docDiff = newStateDiff("foo", listDiff);

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(2, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(1, conditions.size());
        assertEquals(singleton("foo"), conditions.keySet());
    }

    @Test
    public void testConflict3() throws Exception {
        ListDiff listDiff = new ListDiff();
        listDiff.rpush = singletonList(newStateDiff("bar", "val"));
        listDiff.pull = singletonList(newStateDiff("bar", "val2"));
        StateDiff docDiff = newStateDiff("foo", listDiff);

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(2, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(0, conditions.size());
    }

    @Test
    public void testBigComplex() throws Exception {
        StateDiff docDiff = new StateDiff();
        IntStream.range(0, 10_000).forEach(i -> docDiff.put("foo." + i + ".bar.zoo", "val"));

        UpdateBuilder ub = new MongoDBConverter().new UpdateBuilder();
        ConditionsAndUpdates conditionsAndUpdates = ub.build(docDiff);

        List<Document> updates = conditionsAndUpdates.updates;
        assertEquals(1, updates.size());

        Document conditions = conditionsAndUpdates.conditions;
        assertEquals(0, conditions.size());
    }

    protected StateDiff newStateDiff(String key, Serializable value) {
        StateDiff stateDiff = new StateDiff();
        stateDiff.put(key, value);
        return stateDiff;
    }

    protected StateDiff newStateDiff(String key1, Serializable value1, String key2, Serializable value2) {
        StateDiff stateDiff = newStateDiff(key1, value1);
        stateDiff.put(key2, value2);
        return stateDiff;
    }

    protected ListDiff newListDiffDiff(Object... diffs) {
        ListDiff list = new ListDiff();
        list.diff = asList(diffs);
        return list;
    }
}

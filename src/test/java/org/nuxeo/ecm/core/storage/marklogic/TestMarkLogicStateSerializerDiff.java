/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.storage.marklogic;

import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.StateHelper;

/**
 * Tests the {@link StateDiff} serialization for update purposes.
 * 
 * @since 10.2
 */
public class TestMarkLogicStateSerializerDiff extends AbstractSerializerTest {

    @Test
    public void testDiffWithSimpleValue() throws Exception {
        // build previous state
        State previousState = createState("key", Boolean.FALSE);

        // build updated state
        State updatedState = createState("key", Boolean.TRUE);

        // build the diff
        StateDiff diff = StateHelper.diff(previousState, updatedState);
        String xml = MarkLogicStateSerializer.serialize(diff);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer-diff/diff-with-simple-value.xml", xml);
    }

    @Test
    public void testDiffWithDeltaValue() throws Exception {
        StateDiff diff = new StateDiff();
        diff.put("key ", DeltaLong.valueOf(1L, 2));
        String xml = MarkLogicStateSerializer.serialize(diff);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer-diff/diff-with-delta-value.xml", xml);
    }

    @Test
    public void testDiffWithSubDiff() throws Exception {
        // build previous state
        State previousState = new State();
        previousState.put("subDiff", createState("key", "value1"));

        // build updated state
        State updatedState = new State();
        updatedState.put("subDiff", createState("key", "value2"));

        // build the diff
        StateDiff diff = StateHelper.diff(previousState, updatedState);
        String xml = MarkLogicStateSerializer.serialize(diff);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer-diff/diff-with-sub-diff.xml", xml);
    }

    @Test
    public void testDiffWithListDiffOfStateDiffWithUpdate() throws Exception {
        // build previous state
        State previousState = new State();
        List<State> previousList = new ArrayList<>();
        previousList.add(createState("key1", "value011", "key2", "value021"));
        previousList.add(createState("key1", "value111", "key2", "value121"));
        previousState.put("list", (Serializable) previousList);

        // build updated state
        State updatedState = new State();
        List<State> updatedList = new ArrayList<>();
        updatedList.add(createState("key1", "value011", "key2", "value021"));
        updatedList.add(createState("key1", "value111", "key2", "value122"));
        updatedState.put("list", (Serializable) updatedList);

        // build the diff
        StateDiff diff = StateHelper.diff(previousState, updatedState);
        String xml = MarkLogicStateSerializer.serialize(diff);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer-diff/diff-with-list-of-state-for-update.xml", xml);
    }

    @Test
    public void testDiffWithListDiffOfStateDiffWithRemove() throws Exception {
        // build previous state
        State previousState = new State();
        List<State> previousList = new ArrayList<>();
        previousList.add(createState("key1", "value011", "key2", "value021"));
        previousList.add(createState("key1", "value111", "key2", "value121"));
        previousState.put("list", (Serializable) previousList);

        // build updated state
        State updatedState = new State();
        List<State> updatedList = new ArrayList<>();
        updatedList.add(createState("key1", "value011", "key2", "value021"));
        updatedList.add(createState("key1", "value111", "key2", null));
        updatedState.put("list", (Serializable) updatedList);

        // build the diff
        StateDiff diff = StateHelper.diff(previousState, updatedState);
        String xml = MarkLogicStateSerializer.serialize(diff);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer-diff/diff-with-list-of-state-for-remove.xml", xml);
    }

    @Test
    public void testDiffWithListDiffOfStateDiffWithRpush() throws Exception {
        // build previous state
        State previousState = new State();
        List<State> previousList = new ArrayList<>();
        previousList.add(createState("key1", "value011", "key2", "value021"));
        previousState.put("list", (Serializable) previousList);

        // build updated state
        State updatedState = new State();
        List<State> updatedList = new ArrayList<>();
        updatedList.add(createState("key1", "value011", "key2", "value021"));
        updatedList.add(createState("key1", "value111", "key2", "value121"));
        updatedState.put("list", (Serializable) updatedList);

        // build the diff
        StateDiff diff = StateHelper.diff(previousState, updatedState);
        String xml = MarkLogicStateSerializer.serialize(diff);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer-diff/diff-with-list-of-state-for-rpush.xml", xml);
    }

    @Test
    public void testDiffWithListOfStateWithListStateRemove() throws Exception {
        // build previous state
        State previousState = new State();
        List<State> previousList = new ArrayList<>();
        previousList.add(createState("key1", "value011", "key2", "value021"));
        previousList.add(createState("key1", "value111", "key2", "value121"));
        previousState.put("list", (Serializable) previousList);

        // build updated state
        State updatedState = new State();
        List<State> updatedList = new ArrayList<>();
        updatedList.add(createState("key1", "value111", "key2", "value121"));
        updatedState.put("list", (Serializable) updatedList);

        // build the diff
        StateDiff diff = StateHelper.diff(previousState, updatedState);
        String xml = MarkLogicStateSerializer.serialize(diff);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer-diff/diff-with-list-of-state-for-list-state-remove.xml", xml);
    }

    /**
     * @return a basic {@link State} containing the key/value tuples.
     */
    private State createState(String key, Serializable value) {
        State state = new State();
        state.put(key, value);
        return state;
    }

    /**
     * @return a basic {@link State} containing the key/value tuples.
     */
    private State createState(String key1, Serializable value1, String key2, Serializable value2) {
        State state = createState(key1, value1);
        state.put(key2, value2);
        return state;
    }

}

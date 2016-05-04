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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.function.Function;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.State;

public class TestMarkLogicStateDeserializer extends AbstractSerializerTest {

    @Test
    public void testEmptyState() throws Exception {
        String xml = readFile("serializer/empty-state.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        assertEquals(new State(), state);
    }

    @Test
    public void testStateWithSimpleValue() throws Exception {
        String xml = readFile("serializer/state-with-simple-value.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        State expectedState = createStateWithSimpleValue();
        assertEquals(expectedState, state);
    }

    @Test
    public void testStateWithSimpleCalendarValue() throws Exception {
        String xml = readFile("serializer/state-with-simple-calendar-value.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        State expectedState = createStateWithSimpleCalendarValue();
        assertEquals(expectedState, state);
    }

    @Test
    public void testStateWithSubState() throws Exception {
        String xml = readFile("serializer/state-with-sub-state.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        State expectedState = createStateWithSubState();
        assertEquals(expectedState, state);
    }

    @Test
    public void testStateWithList() throws Exception {
        String xml = readFile("serializer/state-with-list.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        State expectedState = createStateWithList();
        assertEquals(expectedState, state);
    }

    @Test
    public void testStateWithArray() throws Exception {
        String xml = readFile("serializer/state-with-array.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        State expectedState = createStateWithArray();
        assertEquals(expectedState, state);
    }

    @Test
    public void testStateWithEmptyArray() throws Exception {
        String xml = readFile("serializer/state-with-empty-array.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        State expectedState = createStateWithEmptyArray();
        assertEquals(expectedState, state);
    }

    @Test
    public void testBijunction() throws Exception {
        String xml = readFile("serializer/bijunction.xml");
        Function<String, State> deserializer = MarkLogicStateDeserializer::deserialize;
        Function<State, String> serializer = MarkLogicStateSerializer::serialize;
        assertXMLEquals(xml, deserializer.andThen(serializer).apply(xml));
    }

    @Test
    public void testOnlyDeserializationForBijunction() throws Exception {
        String xml = readFile("serializer/bijunction.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        State expectedState = createStateForBijunction();
        assertEquals(expectedState, state);
    }

    /*
     * Test deserialization of state issued from TestSQLRepositoryAPI#testMarkDirtyForList.
     */
    @Test
    public void testMarkDirtyForList() throws Exception {
        String xml = readFile("serializer/mark-dirty-for-list.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        State expectedState = createStateForMarkDirtyForList();
        assertEquals(expectedState, state);
    }

    /*
     * Test deserialization of state issued from TestDocument#testSetValueErrors2.
     */
    @Test
    public void testStateWithEmptyStateInList() throws Exception {
        String xml = readFile("serializer/state-with-empty-state-in-list.xml");
        State state = MarkLogicStateDeserializer.deserialize(xml);
        assertNotNull(state);
        State expectedState = createStateForStateWithEmptyMapInList();
        assertEquals(expectedState, state);
    }

}

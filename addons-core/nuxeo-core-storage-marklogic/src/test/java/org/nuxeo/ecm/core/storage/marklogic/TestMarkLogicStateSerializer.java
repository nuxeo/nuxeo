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

public class TestMarkLogicStateSerializer extends AbstractSerializerTest {

    @Test
    public void testEmptyState() throws Exception {
        String xml = MarkLogicStateSerializer.serialize(new State());
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/empty-state.xml", xml);
    }

    @Test
    public void testStateWithNullValue() throws Exception {
        State state = new State();
        state.put("ecm:id", null);
        state.put("ecm:parentId", "ID");
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-null-value-serialization.xml", xml);
    }

    @Test
    public void testStateWithEmptyValue() throws Exception {
        State state = createStateWithEmptyValue();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-empty-value.xml", xml);
    }

    @Test
    public void testStateWithSimpleValue() throws Exception {
        State state = createStateWithSimpleValue();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-simple-value.xml", xml);
    }

    @Test
    public void testStateWithSimpleCalendarValue() throws Exception {
        State state = createStateWithSimpleCalendarValue();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-simple-calendar-value.xml", xml);
    }

    /*
     * NXP-22382
     */
    @Test
    public void testStateWithUnderscoreKey() throws Exception {
        State state = createStateWithUnderscoreKey();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-underscore-key.xml", xml);
    }

    @Test
    public void testStateWithSubState() throws Exception {
        State state = createStateWithSubState();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-sub-state.xml", xml);
    }

    @Test
    public void testStateWithList() throws Exception {
        State state = createStateWithList();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-list.xml", xml);
    }

    @Test
    public void testStateWithArray() throws Exception {
        State state = createStateWithArray();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-array.xml", xml);
    }

    @Test
    public void testStateWithEmptyList() throws Exception {
        State state = createStateWithEmptyList();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-empty-list.xml", xml);
    }

    @Test
    public void testBijunction() {
        State state = createStateForBijunction();
        Function<State, String> serializer = MarkLogicStateSerializer::serialize;
        Function<String, State> deserializer = MarkLogicStateDeserializer::deserialize;
        assertEquals(state, serializer.andThen(deserializer).apply(state));
    }

    @Test
    public void testOnlySerializationForBijunction() throws Exception {
        State state = createStateForBijunction();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/bijunction.xml", xml);
    }

    /*
     * Test serialization of state issued from TestSQLRepositoryAPI#testMarkDirtyForList.
     */
    @Test
    public void testMarkDirtyForList() throws Exception {
        State state = createStateForMarkDirtyForList();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/mark-dirty-for-list.xml", xml);
    }

    /*
     * Test serialization of state issued from Gatling tests.
     */
    @Test
    public void testStateWithEmptyState() throws Exception {
        State state = createStateForStateWithEmptyState();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-empty-state.xml", xml);
    }

    /*
     * Test serialization of state issued from TestDocument#testSetValueErrors2.
     */
    @Test
    public void testStateWithEmptyStateInList() throws Exception {
        State state = createStateForStateWithEmptyStateInList();
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-empty-state-in-list.xml", xml);
    }

}

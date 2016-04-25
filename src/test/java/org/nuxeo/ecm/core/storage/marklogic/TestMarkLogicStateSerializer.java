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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Function;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.State;

public class TestMarkLogicStateSerializer extends AbstractTest {

    @Test
    public void testEmptyState() throws Exception {
        String xml = MarkLogicStateSerializer.serialize(new State());
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/empty-state.xml", xml);
    }

    @Test
    public void testStateWithNullValue() throws Exception {
        State state = new State();
        state.put("ecm:id", "ID");
        state.put("subState", null);
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-null-value.xml", xml);
    }

    @Test
    public void testStateWithSimpleValue() throws Exception {
        State state = new State();
        state.put("ecm:id", "ID");
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-simple-value.xml", xml);
    }

    @Test
    public void testStateWithSimpleCalendarValue() throws Exception {
        State state = new State();
        state.put("ecm:id", "ID");
        Calendar creationDate = MarkLogicHelper.deserializeCalendar("1970-01-01T00:00:00.001");
        state.put("dub:creationDate", creationDate);
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-simple-calendar-value.xml", xml);
    }

    @Test
    public void testStateWithSubState() throws Exception {
        State state = new State();
        state.put("ecm:id", "ID");
        State subState = new State();
        subState.put("nbValues", 2L);
        subState.put("valuesPresent", false);
        state.put("subState", subState);
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-sub-state.xml", xml);
    }

    @Test
    public void testStateWithList() throws Exception {
        State state = new State();
        state.put("ecm:id", "ID");
        state.put("nbValues", 2L);
        State state1 = new State();
        state1.put("item", "itemState1");
        state1.put("read", true);
        state1.put("write", true);
        State state2 = new State();
        state2.put("item", "itemState2");
        state2.put("read", true);
        state2.put("write", false);
        state.put("values", new ArrayList<>(Arrays.asList(state1, state2)));
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-list.xml", xml);
    }

    @Test
    public void testStateWithArray() throws Exception {
        State state = new State();
        state.put("ecm:id", "ID");
        state.put("nbValues", 2L);
        state.put("values", new Long[] { 3L, 4L });
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/state-with-array.xml", xml);
    }

    @Test
    public void testBijunction() throws Exception {
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
        State state = new State();
        state.put("ecm:id", "672f3fc9-38e3-43ec-8b31-f15f6e89f486");
        state.put("ecm:primaryType", "ComplexDoc");
        state.put("ecm:name", "doc");
        state.put("ecm:parentId", "00000000-0000-0000-0000-000000000000");
        State attachedFile = new State();
        ArrayList<State> vignettes = new ArrayList<>();
        State vignette = new State();
        vignette.put("width", 111L);
        vignettes.add(vignette);
        attachedFile.put("vignettes", vignettes);
        state.put("cmpf:attachedFile", attachedFile);
        state.put("ecm:ancestorIds", new Object[] { "00000000-0000-0000-0000-000000000000" });
        state.put("ecm:lifeCyclePolicy", "undefined");
        state.put("ecm:lifeCycleState", "undefined");
        state.put("ecm:majorVersion", 0L);
        state.put("ecm:minorVersion", 0L);
        state.put("ecm:racl", new String[] { "Administrator", "administrators", "members" });
        String xml = MarkLogicStateSerializer.serialize(state);
        assertNotNull(xml);
        assertXMLFileAgainstString("serializer/mark-dirty-for-list.xml", xml);
    }

}

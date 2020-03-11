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
package org.nuxeo.ecm.core.storage.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.storage.State;

public class TestDBSStateFlatter {

    @Test
    public void testFlatterWithEmptyState() {
        State state = new State();
        Map<String, Serializable> flattened = new DBSStateFlattener().flatten(state);
        assertNotNull(flattened);
        assertTrue(flattened.isEmpty());
    }

    @Test
    public void testFlatterWithPropertyNotReturnedToCaller() {
        State state = new State();
        state.put(KEY_ACP, "whatever");
        Map<String, Serializable> flattened = new DBSStateFlattener().flatten(state);
        assertNotNull(flattened);
        assertTrue(flattened.isEmpty());
    }

    @Test
    public void testFlatterWithECMId() {
        State state = new State();
        state.put(KEY_ID, "ID");
        Map<String, Serializable> flattened = new DBSStateFlattener().flatten(state);
        assertNotNull(flattened);
        assertEquals("ID", flattened.get(NXQL.ECM_UUID));
    }

    @Test
    public void testFlatterWithRegularProperty() {
        State state = new State();
        state.put("color", "black");
        Map<String, Serializable> flattened = new DBSStateFlattener().flatten(state);
        assertNotNull(flattened);
        assertEquals(1, flattened.size());
        assertEquals("black", flattened.get("color"));
    }

    @Test
    public void testFlatterWithSubState() {
        State state = new State();
        State subState = new State();
        subState.put("color", "black");
        state.put("sub", subState);
        Map<String, Serializable> flattened = new DBSStateFlattener().flatten(state);
        assertNotNull(flattened);
        assertEquals("black", flattened.get("sub/color"));
    }

    @Test
    public void testFlatterWithArray() {
        State state = new State();
        state.put("colors", new String[] { "black", "white" });
        Map<String, Serializable> flattened = new DBSStateFlattener().flatten(state);
        assertNotNull(flattened);
        assertEquals(2, flattened.size());
        assertEquals("black", flattened.get("colors/0"));
        assertEquals("white", flattened.get("colors/1"));
    }

    @Test
    public void testFlatterWithMappings() {
        State state = new State();
        Map<String, String> mappings = new HashMap<>();
        mappings.put("king", "queen");
        state.put("king", "kong");
        state.put("ping", "pong");
        Map<String, Serializable> flattened = new DBSStateFlattener(mappings).flatten(state);
        assertNotNull(flattened);
        assertEquals(2, flattened.size());
        assertEquals("kong", flattened.get("queen"));
        assertEquals("pong", flattened.get("ping"));
    }

    @Test
    public void testFlatterWithListOfPrimitive() {
        State state = new State();
        state.put("colors", new ArrayList<>(Arrays.asList("black", "white")));
        Map<String, Serializable> flattened = new DBSStateFlattener().flatten(state);
        assertNotNull(flattened);
        assertEquals(2, flattened.size());
        assertEquals("black", flattened.get("colors/0"));
        assertEquals("white", flattened.get("colors/1"));
    }

    @Test
    public void testFlatterWithListOfState() {
        State state = new State();
        State subState1 = new State();
        subState1.put("main", "black");
        State subState2 = new State();
        subState2.put("main", "white");
        state.put("colors", new ArrayList<>(Arrays.asList(subState1, subState2)));
        Map<String, Serializable> flattened = new DBSStateFlattener().flatten(state);
        assertNotNull(flattened);
        assertEquals(2, flattened.size());
        assertEquals("black", flattened.get("colors/0/main"));
        assertEquals("white", flattened.get("colors/1/main"));
    }

}

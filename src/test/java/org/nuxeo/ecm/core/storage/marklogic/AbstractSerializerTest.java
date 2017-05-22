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

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_GRANT;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_PERMISSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_USER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LIFECYCLE_POLICY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LIFECYCLE_STATE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MAJOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MINOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_READ_ACL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.nuxeo.ecm.core.storage.State;

public abstract class AbstractSerializerTest extends AbstractTest {

    protected State createStateWithEmptyValue() {
        State state = new State();
        state.put(KEY_ID, "");
        return state;
    }

    protected State createStateWithSimpleValue() {
        State state = new State();
        state.put(KEY_ID, "ID");
        return state;
    }

    protected State createStateWithSimpleCalendarValue() {
        State state = new State();
        state.put(KEY_ID, "ID");
        state.put("dc:creationDate", MarkLogicHelper.deserializeCalendar("1970-01-01T00:00:00.001"));
        return state;
    }

    protected State createStateWithUnderscoreKey() {
        State state = new State();
        state.put(KEY_ID, "ID");
        state.put("status:administrative_status", "active");
        return state;
    }

    protected State createStateWithSubState() {
        State state = new State();
        state.put(KEY_ID, "ID");
        State subState = new State();
        subState.put("nbValues", 2L);
        subState.put("valuesPresent", false);
        state.put("subState", subState);
        return state;
    }

    protected State createStateWithList() {
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
        return state;
    }

    protected State createStateWithArray() {
        State state = new State();
        state.put("ecm:id", "ID");
        state.put("nbValues", 2L);
        state.put("values", new Long[] { 3L, 4L });
        return state;
    }

    protected State createStateWithEmptyList() {
        State state = new State();
        state.put("ecm:id", "ID");
        state.put("values", new ArrayList<>());
        return state;
    }

    protected State createStateForBijunction() {
        State state = new State();
        state.put(KEY_ID, "ID");
        state.put("dc:creationDate", MarkLogicHelper.deserializeCalendar("2016-03-21T18:01:43.113"));
        State subState = new State();
        subState.put("nbValues", 2L);
        State state1 = new State();
        state1.put("item", "itemState1");
        state1.put("read", true);
        state1.put("write", true);
        State state2 = new State();
        state2.put("item", "itemState2");
        state2.put("read", true);
        state2.put("write", false);
        subState.put("values", new ArrayList<>(Arrays.asList(state1, state2)));
        subState.put("valuesAsArray", new Long[] { 3L, 4L });
        state.put("subState", subState);
        return state;
    }

    protected State createStateForMarkDirtyForList() {
        State expectedState = new State();
        expectedState.put(KEY_ID, "672f3fc9-38e3-43ec-8b31-f15f6e89f486");
        expectedState.put(KEY_PRIMARY_TYPE, "ComplexDoc");
        expectedState.put(KEY_NAME, "doc");
        expectedState.put(KEY_PARENT_ID, "00000000-0000-0000-0000-000000000000");
        State attachedFile = new State();
        ArrayList<State> vignettes = new ArrayList<>();
        State vignette = new State();
        vignette.put("width", 111L);
        vignettes.add(vignette);
        attachedFile.put("vignettes", vignettes);
        expectedState.put("cmpf:attachedFile", attachedFile);
        expectedState.put(KEY_ANCESTOR_IDS, new String[] { "00000000-0000-0000-0000-000000000000" });
        expectedState.put(KEY_LIFECYCLE_POLICY, "undefined");
        expectedState.put(KEY_LIFECYCLE_STATE, "undefined");
        expectedState.put(KEY_MAJOR_VERSION, 0L);
        expectedState.put(KEY_MINOR_VERSION, 0L);
        expectedState.put(KEY_READ_ACL, new String[] { "Administrator", "administrators", "members" });
        return expectedState;
    }

    protected State createStateForStateWithEmptyState() {
        State state = new State();
        state.put(KEY_ID, "1b783a64-8789-4b8f-8fbc-4d25f5f6d4f4");
        state.put(KEY_PRIMARY_TYPE, "Workspace");
        state.put(KEY_NAME, "Bench_Gatling");
        state.put("dc:description", "Gatling bench folder");
        state.put("content", new State());
        return state;
    }

    protected State createStateForStateWithEmptyStateInList() {
        State state = new State();
        state.put(KEY_ID, "326203a5-56fb-4ae7-8983-123b814c9388");
        state.put(KEY_PRIMARY_TYPE, "TestDocument");
        state.put(KEY_NAME, "doc");
        state.put(KEY_PARENT_ID, "00000000-0000-0000-0000-000000000000");
        state.put(KEY_ANCESTOR_IDS, new Object[] { "00000000-0000-0000-0000-000000000000" });
        state.put(KEY_READ_ACL, new String[] { "Administrator", "administrators", "members" });
        state.put("tp:complexList", (Serializable) Collections.singletonList(new State()));
        return state;
    }

    protected State createStateForStateFromTestACLEscaping() {
        State state = new State();
        state.put(KEY_NAME, "folder1");
        state.put(KEY_MAJOR_VERSION, 0L);
        State acpItem = new State();
        acpItem.put(KEY_ACL_NAME, "local");
        acpItem.put(KEY_ACL,
                new ArrayList<>(Arrays.asList(createACLItem(true, true, "xyz"),
                        createACLItem(true, true, "abc@def<&>/"), createACLItem(true, true, "café"),
                        createACLItem(true, true, "o'hara"), createACLItem(true, true, "A_x1234_"))));
        state.put(KEY_ACP, new ArrayList<>(Collections.singletonList(acpItem)));
        state.put(KEY_LIFECYCLE_POLICY, "default");
        state.put(KEY_READ_ACL, new String[] { "A_x1234_", "Administrator", "abc@def<&>/", "administrators",
                "anonymous", "café", "members", "o'hara", "xyz" });
        state.put(KEY_PRIMARY_TYPE, "Folder");
        state.put(KEY_ID, "5f2abfac-931f-4db7-bd37-46a2a6655ef2");
        state.put(KEY_PARENT_ID, "00000000-0000-0000-0000-000000000000");
        state.put(KEY_ANCESTOR_IDS, new String[] { "00000000-0000-0000-0000-000000000000" });
        state.put(KEY_MINOR_VERSION, 0L);
        state.put(KEY_LIFECYCLE_STATE, "project");
        return state;
    }

    private State createACLItem(boolean read, boolean grant, String user) {
        State state = new State();
        state.put(KEY_ACE_PERMISSION, read ? "Read" : "Write");
        state.put(KEY_ACE_GRANT, grant);
        state.put(KEY_ACE_USER, user);
        return state;
    }

}

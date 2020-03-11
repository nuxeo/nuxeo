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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.runtime.cluster.ClusterFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(ClusterFeature.class)
public class TestDBSCachingRepository {

    private DBSCachingRepository repository;

    private DBSCachingConnection connection;

    private DBSConnection subConnection;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        subConnection = mock(DBSConnection.class);
        when(subConnection.readState(any())).then(invocation -> newState(invocation.getArguments()[0].toString()));
        when(subConnection.readStates(anyListOf(String.class))).then(
                invocation -> ((List<String>) invocation.getArguments()[0]).stream().map(id -> {
                    State state = new State();
                    state.setSingle(KEY_ID, id);
                    return state;
                }).collect(Collectors.toList()));
        when(subConnection.readChildState(any(), any(), any())).then(invocation -> {
            Object parentId = invocation.getArguments()[0];
            Object name = invocation.getArguments()[1];
            State state = newState(parentId.toString() + "_" + name);
            state.setSingle(KEY_PARENT_ID, parentId);
            state.setSingle(KEY_NAME, name);
            return state;
        });
        DBSRepositoryDescriptor descriptor = newDBSRepositoryDescriptor();
        DBSRepository subRepository = mock(DBSRepository.class);
        repository = new DBSCachingRepository(subRepository, descriptor);
        connection = new DBSCachingConnection(subConnection, repository);
    }

    @After
    public void after() {
        // Used to remove metrics
        repository.shutdown();
    }

    @Test
    public void testReadState() {
        String id = "ID";

        // First read - call sub repository
        State dbState = connection.readState(id);
        verify(subConnection, times(1)).readState(eq(id));

        // Second read - call cache
        State cachedState = connection.readState(id);
        verify(subConnection, times(1)).readState(eq(id));

        assertEquals(dbState, cachedState);
    }

    @Test
    public void testReadStates() {
        String id1 = "ID1";
        String id2 = "ID2";

        // First read - call sub repository
        List<State> dbStates = connection.readStates(Collections.singletonList(id1));
        verify(subConnection, times(1)).readStates(eq(Collections.singletonList(id1)));

        // Second read - call cache for id1 and repository for id2
        List<State> cachedStates = connection.readStates(Arrays.asList(id1, id2));
        verify(subConnection, times(1)).readStates(eq(Collections.singletonList(id2)));

        assertEquals(1, dbStates.size());
        assertEquals(2, cachedStates.size());
        assertEquals(dbStates.get(0), cachedStates.get(0));
        List<State> states = new ArrayList<>(dbStates);
        states.add(cachedStates.get(1));
        assertEquals(states, cachedStates);
    }

    @Test
    public void testUpdateState() {
        String id = "ID";

        // First add a state in cache
        connection.readState(id);
        connection.readState(id);
        verify(subConnection, times(1)).readState(eq(id));

        // Second update this state
        connection.updateState(id, mock(StateDiff.class), null);
        verify(subConnection, times(1)).updateState(eq(id), any(), any());

        // Check state is no longer in cache
        connection.readState(id);
        verify(subConnection, times(2)).readState(eq(id));
    }

    @Test
    public void testDeleteStates() {
        String id = "ID";

        // First add a state in cache
        connection.readState(id);
        connection.readState(id);
        verify(subConnection, times(1)).readState(eq(id));

        // Second delete this state
        connection.deleteStates(Collections.singleton(id));
        verify(subConnection, times(1)).deleteStates(eq(Collections.singleton(id)));

        // Check state is no longer in cache
        connection.readState(id);
        verify(subConnection, times(2)).readState(eq(id));
    }

    @Test
    public void testReadChildState() {
        String parentId = "PARENT-ID";
        String name = "NAME";
        String id = parentId + "_" + name;

        // First read - call sub repository
        State dbState = connection.readChildState(parentId, name, Collections.emptySet());
        verify(subConnection, times(1)).readChildState(eq(parentId), eq(name), any());

        // Second read - call cache
        State cachedState = connection.readChildState(parentId, name, Collections.emptySet());
        verify(subConnection, times(1)).readChildState(eq(parentId), eq(name), any());

        assertEquals(dbState, cachedState);

        // Third read cached state from cache with readState
        cachedState = connection.readState(id);
        assertEquals(dbState, cachedState);
    }

    private State newState(String id) {
        State state = new State();
        state.setSingle(KEY_ID, id);
        return state;
    }

    private DBSRepositoryDescriptor newDBSRepositoryDescriptor() {
        DBSRepositoryDescriptor descriptor = new DBSRepositoryDescriptor();
        descriptor.setCacheEnabled(true);
        descriptor.cacheTTL = 10L;
        descriptor.cacheMaxSize = 1000L;
        descriptor.cacheConcurrencyLevel = 1000;
        return descriptor;
    }

}

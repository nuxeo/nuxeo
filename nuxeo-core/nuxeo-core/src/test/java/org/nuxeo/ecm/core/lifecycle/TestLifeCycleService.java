/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test the lifecycle service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core:OSGI-INF/LifeCycleService.xml")
@Deploy("org.nuxeo.ecm.core.tests:LifeCycleManagerTestExtensions.xml")
public class TestLifeCycleService {

    private LifeCycleService lifeCycleService;

    @Before
    public void setUp() throws Exception {
        lifeCycleService = Framework.getService(LifeCycleService.class);
        assertNotNull(lifeCycleService);
    }

    /**
     * Tests the life cycles registration.
     */
    @Test
    public void testLifeCycleRegistration() throws Exception {
        Collection<LifeCycle> lcs = lifeCycleService.getLifeCycles();
        assertEquals(1, lcs.size());

        // Test life cycle registration

        LifeCycle lcd = lifeCycleService.getLifeCycleByName("default");
        assertNotNull(lcd);

        assertEquals("default", lcd.getName());

        // Test states registration

        Collection<LifeCycleState> states = lcd.getStates();
        assertEquals(4, states.size());

        List<String> expected = new ArrayList<>();
        expected.add("work");
        expected.add("approved");
        expected.add("cancelled");
        expected.add("obsolete");
        Collections.sort(expected);

        List<String> stateNames = new ArrayList<>();
        for (LifeCycleState state : states) {
            stateNames.add(state.getName());
        }
        Collections.sort(stateNames);

        assertEquals(expected, stateNames);

        // Test the initial state
        assertEquals("work", lcd.getDefaultInitialStateName());

        // Test all initial states
        List<String> expectedInitialStates = new ArrayList<>();
        expectedInitialStates.add("work");
        expectedInitialStates.add("approved");
        Collections.sort(expectedInitialStates);
        List<String> initialStates = new ArrayList<>(lcd.getInitialStateNames());
        Collections.sort(initialStates);
        assertEquals(expectedInitialStates, initialStates);

        // Test transitions
        Collection<String> transitions;

        // work
        transitions = lcd.getAllowedStateTransitionsFrom("work");
        assertEquals(3, transitions.size());
        assertTrue(transitions.contains("approve"));
        assertTrue(transitions.contains("cancel"));
        assertTrue(transitions.contains("obsolete"));

        // check mutation fails
        try {
            transitions.remove("approve");
            fail("Mutation should fail");
        } catch (Exception e) {
            // ok
        }
        transitions = lcd.getAllowedStateTransitionsFrom("work");
        assertEquals(3, transitions.size());

        // approved
        transitions = lcd.getAllowedStateTransitionsFrom("approved");
        assertEquals(1, transitions.size());
        assertTrue(transitions.contains("obsolete"));

        // canceled
        transitions = lcd.getAllowedStateTransitionsFrom("cancelled");
        assertEquals(1, transitions.size());
        assertTrue(transitions.contains("backToWork"));

        // obsolete
        transitions = lcd.getAllowedStateTransitionsFrom("obsolete");
        assertEquals(0, transitions.size());

        // Test transitions

        Collection<LifeCycleTransition> lifeCycleDefinitions = lcd.getTransitions();
        assertEquals(4, lifeCycleDefinitions.size());
    }

    @Test
    public void testLifeCycle() {
        LifeCycle lifeCycle = lifeCycleService.getLifeCycleByName("default");

        // work state
        LifeCycleState workState = lifeCycle.getStateByName("work");
        assertEquals("work", workState.getName());

        Collection<String> transitions = workState.getAllowedStateTransitions();
        assertEquals(3, transitions.size());
        assertTrue(transitions.contains("approve"));
        assertTrue(transitions.contains("cancel"));
        assertTrue(transitions.contains("obsolete"));

        // approved state
        workState = lifeCycle.getStateByName("approved");
        transitions = workState.getAllowedStateTransitions();
        assertEquals(1, transitions.size());
        assertTrue(transitions.contains("obsolete"));

        // reject state
        workState = lifeCycle.getStateByName("cancelled");
        assertEquals("cancelled", workState.getName());

        transitions = workState.getAllowedStateTransitions();
        assertEquals(1, transitions.size());
        assertTrue(transitions.contains("backToWork"));

        // obsolete state
        workState = lifeCycle.getStateByName("obsolete");
        assertEquals("obsolete", workState.getName());

        transitions = workState.getAllowedStateTransitions();
        assertEquals(0, transitions.size());

        LifeCycleTransition transition = lifeCycle.getTransitionByName("approve");
        assertNotNull(transition);

        String destinationName = lifeCycle.getTransitionByName("approve").getDestinationStateName();
        assertEquals("approved", destinationName);

    }

    @Test
    public void testLifeCycleTypesMappingRegistration() {
        Map<String, String> mapping = lifeCycleService.getTypesMapping();

        assertTrue(mapping.keySet().contains("File"));
        assertTrue(mapping.keySet().contains("Folder"));

        assertEquals("default", mapping.get("File"));
        assertEquals("default", mapping.get("Folder"));
    }

    @Test
    public void testLifeCycleTypesMappingAPI() {
        Collection<String> types = lifeCycleService.getTypesFor("default");
        assertTrue(types.contains("File"));
        assertTrue(types.contains("Folder"));
    }

    @Test
    public void testTypeLifeCycleMapping() {
        String lifeCycleName = lifeCycleService.getLifeCycleNameFor("File");
        assertEquals("default", lifeCycleName);
        List<String> noRecursion = lifeCycleService.getNonRecursiveTransitionForDocType("File");
        assertEquals(3, noRecursion.size());
        assertTrue(noRecursion.contains("toBar"));
        noRecursion = lifeCycleService.getNonRecursiveTransitionForDocType("Folder");
        assertTrue(noRecursion.isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.tests:LifeCycleManagerReverseTestExtensions.xml")
    public void testLifeCycleReverse() throws Exception {

        LifeCycle lifeCycle = lifeCycleService.getLifeCycleByName("defaultReverse");

        // work state
        LifeCycleState workState = lifeCycle.getStateByName("work");
        assertEquals("work", workState.getName());

        Collection<String> transitions = workState.getAllowedStateTransitions();
        assertEquals(3, transitions.size());
        assertTrue(transitions.contains("approve"));
        assertTrue(transitions.contains("cancel"));
        assertTrue(transitions.contains("obsolete"));

        // approved state
        workState = lifeCycle.getStateByName("approved");
        transitions = workState.getAllowedStateTransitions();
        assertEquals(1, transitions.size());
        assertTrue(transitions.contains("obsolete"));

        // reject state
        workState = lifeCycle.getStateByName("cancelled");
        assertEquals("cancelled", workState.getName());

        transitions = workState.getAllowedStateTransitions();
        assertEquals(1, transitions.size());
        assertTrue(transitions.contains("backToWork"));

        // obsolete state
        workState = lifeCycle.getStateByName("obsolete");
        assertEquals("obsolete", workState.getName());

        transitions = workState.getAllowedStateTransitions();
        assertEquals(0, transitions.size());

        LifeCycleTransition transition = lifeCycle.getTransitionByName("approve");
        assertNotNull(transition);

        String destinationName = transition.getDestinationStateName();
        assertEquals("approved", destinationName);
    }

}

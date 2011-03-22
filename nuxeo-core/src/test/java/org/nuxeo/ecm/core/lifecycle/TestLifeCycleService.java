/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 * $Id: TestLifeCycleService.java 28609 2008-01-09 16:38:30Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.CoreUTConstants;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test the lifecycle service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestLifeCycleService extends NXRuntimeTestCase {

    private LifeCycleService lifeCycleService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreUTConstants.CORE_BUNDLE,
                "OSGI-INF/LifeCycleService.xml");
        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "LifeCycleManagerTestExtensions.xml");

        lifeCycleService = NXCore.getLifeCycleService();
        assertNotNull(lifeCycleService);
    }

    /**
     * Tests the life cycles registration.
     */
    public void testLifeCycleRegistration() throws LifeCycleException {
        Collection<LifeCycle> lcs = lifeCycleService.getLifeCycles();
        assertEquals(1, lcs.size());

        // Test life cycle registration

        LifeCycle lcd = lifeCycleService.getLifeCycleByName("default");
        assertNotNull(lcd);

        assertEquals("default", lcd.getName());

        // Test states registration

        Collection<LifeCycleState> states = lcd.getStates();
        assertEquals(4, states.size());

        List<String> expected = new ArrayList<String>();
        expected.add("work");
        expected.add("approved");
        expected.add("cancelled");
        expected.add("obsolete");
        Collections.sort(expected);

        List<String> stateNames = new ArrayList<String>();
        for (LifeCycleState state : states) {
            stateNames.add(state.getName());
        }
        Collections.sort(stateNames);

        assertEquals(expected, stateNames);

        // Test the initial state
        assertEquals("work", lcd.getDefaultInitialStateName());

        // Test all initial states
        List<String> expectedInitialStates = new ArrayList<String>();
        expectedInitialStates.add("work");
        expectedInitialStates.add("approved");
        Collections.sort(expectedInitialStates);
        List<String> initialStates = new ArrayList<String>(
                lcd.getInitialStateNames());
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

    public void testLifeCycleTypesMappingRegistration() {
        Map<String, String> mapping = lifeCycleService.getTypesMapping();
        assertEquals(2, mapping.size());

        assertTrue(mapping.keySet().contains("File"));
        assertTrue(mapping.keySet().contains("Folder"));

        assertEquals("default", mapping.get("File"));
        assertEquals("default", mapping.get("Folder"));
    }

    public void testLifeCycleTypesMappingAPI() {
        Collection<String> types = lifeCycleService.getTypesFor("default");
        assertEquals(2, types.size());
        assertTrue(types.contains("File"));
        assertTrue(types.contains("Folder"));
    }

    public void testTypeLifeCycleMapping() {
        String lifeCycleName = lifeCycleService.getLifeCycleNameFor("File");
        assertEquals("default", lifeCycleName);
        List<String> noRecursion = lifeCycleService.getNonRecursiveTransitionForDocType("File");
        assertEquals(3, noRecursion.size());
        assertTrue(noRecursion.contains("toBar"));
        noRecursion = lifeCycleService.getNonRecursiveTransitionForDocType("Folder");
        assertTrue(noRecursion.isEmpty());
    }

    public void testLifeCycleReverse() throws Exception {

        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "LifeCycleManagerReverseTestExtensions.xml");

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

/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.io.registry.reflect;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.core.io.pojo.Marshallers.ChildListWriter;
import org.nuxeo.ecm.core.io.pojo.Marshallers.ChildWriter;
import org.nuxeo.ecm.core.io.pojo.Marshallers.GrandParentListWriter;
import org.nuxeo.ecm.core.io.pojo.Marshallers.GrandParentWriter;
import org.nuxeo.ecm.core.io.pojo.Marshallers.ParentListWriter;
import org.nuxeo.ecm.core.io.pojo.Marshallers.ParentWriter;

/**
 * Checks that {@link MarshallerInspector} respect the {@link Comparable} contract.
 *
 * @since 2025.6
 */
public class TestMarshallerInspectorComparable {

    protected final MarshallerInspector grandParentInspector = new MarshallerInspector(GrandParentWriter.class);

    protected final MarshallerInspector grandParentListInspector = new MarshallerInspector(GrandParentListWriter.class);

    protected final MarshallerInspector parentInspector = new MarshallerInspector(ParentWriter.class);

    protected final MarshallerInspector parentListInspector = new MarshallerInspector(ParentListWriter.class);

    protected final MarshallerInspector childInspector = new MarshallerInspector(ChildWriter.class);

    protected final MarshallerInspector childListInspector = new MarshallerInspector(ChildListWriter.class);

    @Test
    public void textExpectedOrder() {
        assertTrue(grandParentListInspector.compareTo(grandParentInspector) > 0);
        assertTrue(grandParentInspector.compareTo(parentListInspector) > 0);
        assertTrue(parentListInspector.compareTo(parentInspector) > 0);
        assertTrue(parentInspector.compareTo(childListInspector) > 0);
        assertTrue(childListInspector.compareTo(childInspector) > 0);
    }

    @Test
    public void testBijectivityOnInheritance() {
        assertTrue(grandParentInspector.compareTo(parentInspector) > 0);
        assertTrue(parentInspector.compareTo(grandParentInspector) < 0);

        assertTrue(parentInspector.compareTo(childInspector) > 0);
        assertTrue(childInspector.compareTo(parentInspector) < 0);
    }

    @Test
    public void testBijectivityOnInheritanceAndList() {
        assertTrue(grandParentListInspector.compareTo(parentListInspector) > 0);
        assertTrue(parentListInspector.compareTo(grandParentListInspector) < 0);

        assertTrue(parentListInspector.compareTo(childListInspector) > 0);
        assertTrue(childListInspector.compareTo(parentListInspector) < 0);
    }

    @Test
    public void testBijectivityOnTypeAndListOfType() {
        assertTrue(grandParentListInspector.compareTo(grandParentInspector) > 0);
        assertTrue(grandParentInspector.compareTo(grandParentListInspector) < 0);

        assertTrue(parentListInspector.compareTo(parentInspector) > 0);
        assertTrue(parentInspector.compareTo(parentListInspector) < 0);

        assertTrue(childListInspector.compareTo(childInspector) > 0);
        assertTrue(childInspector.compareTo(childListInspector) < 0);
    }

    @Test
    public void testTransitivityOnAllPermutations() {
        assertTransitivity(grandParentListInspector, grandParentInspector, parentInspector);
        assertTransitivity(grandParentListInspector, grandParentInspector, parentListInspector);
        assertTransitivity(grandParentListInspector, grandParentInspector, childInspector);
        assertTransitivity(grandParentListInspector, grandParentInspector, childListInspector);
        assertTransitivity(grandParentListInspector, parentListInspector, parentInspector);
        assertTransitivity(grandParentListInspector, parentListInspector, childInspector);
        assertTransitivity(grandParentListInspector, parentListInspector, childListInspector);
        assertTransitivity(grandParentListInspector, parentInspector, childInspector);
        assertTransitivity(grandParentListInspector, parentInspector, childListInspector);
        assertTransitivity(grandParentListInspector, childListInspector, childInspector);

        assertTransitivity(grandParentListInspector, parentListInspector, parentInspector);
        assertTransitivity(grandParentListInspector, parentListInspector, childInspector);
        assertTransitivity(grandParentListInspector, parentListInspector, childListInspector);
        assertTransitivity(grandParentListInspector, parentInspector, childInspector);
        assertTransitivity(grandParentListInspector, parentInspector, childListInspector);
        assertTransitivity(grandParentListInspector, childListInspector, childInspector);

        assertTransitivity(parentListInspector, parentInspector, childInspector);
        assertTransitivity(parentListInspector, parentInspector, childListInspector);
        assertTransitivity(parentListInspector, childListInspector, childInspector);

        assertTransitivity(parentListInspector, childListInspector, childInspector);
    }

    protected void assertTransitivity(MarshallerInspector a, MarshallerInspector b, MarshallerInspector c) {
        assertTrue(a.getMarshallerClass() + " is not greater than " + b.getMarshallerClass(), a.compareTo(b) > 0);
        assertTrue(b.getMarshallerClass() + " is not greater than " + c.getMarshallerClass(), b.compareTo(c) > 0);
        assertTrue(a.getMarshallerClass() + " is not greater than " + c.getMarshallerClass()
                + ", thus not respecting the Comparable contract", a.compareTo(c) > 0);
    }
}

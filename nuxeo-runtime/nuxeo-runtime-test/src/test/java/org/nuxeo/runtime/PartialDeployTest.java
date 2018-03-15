/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.Test;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.StreamRef;
import org.nuxeo.runtime.model.URLStreamRef;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.test.runner.TargetExtensions;

public class PartialDeployTest extends NXRuntimeTestCase {

    public static final String COMPONENT_NAME = "my.comp4";

    public static final String PARTIAL_COMPONENT_NAME = COMPONENT_NAME + "-partial";

    @Test
    public void deployWithoutExtensions() throws Exception {
        assertNull(getComponent(COMPONENT_NAME));
        assertNull(getComponent(PARTIAL_COMPONENT_NAME));

        StreamRef compRef = new URLStreamRef(getResource("MyComp4.xml"));
        deployPartialComponent(getContext(), Collections.emptySet(), compRef);
        applyInlineDeployments();

        assertNull(getComponent(COMPONENT_NAME));
        assertNotNull(getComponent(PARTIAL_COMPONENT_NAME));
        assertNumberOfExtensionsEquals(0, PARTIAL_COMPONENT_NAME);
    }

    @Test
    public void deployWithExtensions() throws Exception {
        assertNull(getComponent(COMPONENT_NAME));
        assertNull(getComponent(PARTIAL_COMPONENT_NAME));

        TargetExtensions te = new TestTargetExtensions();
        StreamRef compRef = new URLStreamRef(getResource("MyComp4.xml"));
        deployPartialComponent(getContext(), Collections.singleton(te), compRef);
        applyInlineDeployments();

        assertNull(getComponent(COMPONENT_NAME));
        assertNotNull(getComponent(PARTIAL_COMPONENT_NAME));
        assertNumberOfExtensionsEquals(1, PARTIAL_COMPONENT_NAME);
    }

    protected void assertNumberOfExtensionsEquals(int length, String name) {
        RegistrationInfo ri = runtime.getComponentManager().getRegistrationInfo(toCompName(name));
        assertEquals(length, ri.getExtensions().length);
    }

    protected ComponentInstance getComponent(String name) {
        return (ComponentInstance) runtime.getComponent(toCompName(name));
    }

    protected ComponentName toCompName(String name) {
        return new ComponentName(name);
    }

    protected class TestTargetExtensions extends TargetExtensions {
        @Override
        protected void initialize() {
            addTargetExtension("my.comp3.alias2", "xp");
        }
    }

}

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

import java.io.IOException;
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

    @Test
    public void deployWithoutExtensions() throws IOException {
        String name = "my.comp4";
        String partial = name + "-partial";
        StreamRef compRef = new URLStreamRef(getResource("MyComp4.xml"));

        assertNull(getComponent(name));
        assertNull(getComponent(partial));

        deployPartialComponent(getContext(), Collections.emptySet(), compRef);
        assertNull(getComponent(name));
        assertNotNull(getComponent(partial));
        assertNumberOfExtensionsEquals(0, partial);
    }

    @Test
    public void deployWithExtensions() throws IOException {
        String name = "my.comp4";
        String partial = name + "-partial";
        StreamRef compRef = new URLStreamRef(getResource("MyComp4.xml"));

        assertNull(getComponent(name));
        assertNull(getComponent(partial));

        TargetExtensions te = new TargetExtensions() {
            @Override
            protected void initialize() {
                addTargetExtension("my.comp3.alias2", "xp");
            }
        };
        deployPartialComponent(getContext(), Collections.singleton(te), compRef);

        assertNull(getComponent(name));
        assertNotNull(getComponent(partial));
        assertNumberOfExtensionsEquals(1, partial);
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
}

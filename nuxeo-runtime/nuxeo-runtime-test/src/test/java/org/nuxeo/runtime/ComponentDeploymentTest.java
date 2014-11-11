/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.runtime;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ComponentDeploymentTest extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.runtime.test.tests", "MyComp1.xml");
        deployContrib("org.nuxeo.runtime.test.tests", "MyComp2.xml");
    }

    @Test
    public void testContributions() {
        RuntimeService runtime = Framework.getRuntime();
        ComponentManager mgr = runtime.getComponentManager();
        assertTrue(mgr.size() > 0);

        ComponentInstance co = runtime.getComponentInstance("service:my.comp1");
        assertNotNull(co);
        assertEquals(co.getName(), new ComponentName("service:my.comp1"));

        co = runtime.getComponentInstance("service:my.comp2");
        assertNotNull(co);
        assertEquals(co.getName(), new ComponentName("service:my.comp2"));

        mgr.unregister(new ComponentName("service:my.comp2"));
        co = runtime.getComponentInstance("service:my.comp2");
        assertNull(co);
        co = runtime.getComponentInstance("service:my.comp1");
        assertNotNull(co);
    }

}

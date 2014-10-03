/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class ComponentAliasTest extends NXRuntimeTestCase {

    @Test
    public void testContributions() throws Exception {
        deployContrib("org.nuxeo.runtime.test.tests", "MyComp3.xml");
        deployContrib("org.nuxeo.runtime.test.tests", "MyComp4.xml");
        check();
    }

    @Test
    public void testContributionsPending() throws Exception {
        deployContrib("org.nuxeo.runtime.test.tests", "MyComp4.xml");
        // register the required one last
        deployContrib("org.nuxeo.runtime.test.tests", "MyComp3.xml");
        check();
    }

    protected void check() {
        RuntimeService runtime = Framework.getRuntime();

        ComponentManager mgr = runtime.getComponentManager();
        assertTrue(mgr.size() > 0);
        Map<ComponentName, Set<ComponentName>> pending = mgr.getPendingRegistrations();
        assertEquals(0, pending.size());

        ComponentInstance co = runtime.getComponentInstance("my.comp3");
        assertNotNull(co);
        assertEquals(new ComponentName("my.comp3"), co.getName());

        // lookup by alias
        co = runtime.getComponentInstance("my.comp3.alias");
        assertNotNull(co);
        assertEquals(new ComponentName("my.comp3"), co.getName());

        co = runtime.getComponentInstance("my.comp4");
        assertNotNull(co);
        assertEquals(new ComponentName("my.comp4"), co.getName());

        ComponentWithXPoint c = (ComponentWithXPoint) runtime.getComponent(new ComponentName(
                "my.comp3"));
        DummyContribution[] contribs = c.getContributions();
        assertEquals(3, contribs.length);
    }

}

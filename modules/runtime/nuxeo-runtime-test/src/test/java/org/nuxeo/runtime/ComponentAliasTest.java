/*
 * (C) Copyright 2012-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.impl.ComponentManagerImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class ComponentAliasTest {

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp3.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp4.xml")
    public void testContributions() throws Exception {
        check(3);
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp4.xml")
    // register the required one last
    @Deploy("org.nuxeo.runtime.test.tests:MyComp3.xml")
    public void testContributionsPending() throws Exception {
        check(3);
    }

    @Test
    // contrib to an alias of the component, not using a require
    @Deploy("org.nuxeo.runtime.test.tests:MyComp4b.xml")
    // the component itself
    @Deploy("org.nuxeo.runtime.test.tests:MyComp3.xml")
    public void testContributionsPendingOnAliasWithoutRequire() throws Exception {
        check(1);
    }

    protected void check(int ncontrib) {
        RuntimeService runtime = Framework.getRuntime();

        ComponentManagerImpl mgr = (ComponentManagerImpl) runtime.getComponentManager();
        assertTrue(mgr.size() > 0);
        assertEquals(0, mgr.getPendingRegistrations().size());
        assertEquals(0, mgr.getNeededRegistrations().size());

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

        ComponentWithXPoint c = (ComponentWithXPoint) runtime.getComponent(new ComponentName("my.comp3"));
        DummyContribution[] contribs = c.getContributions();
        assertEquals(ncontrib, contribs.length);
    }

}

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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/** @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> */
public class TestExtensionPoint extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.runtime.test.tests", "BaseXPoint.xml");
        deployContrib("org.nuxeo.runtime.test.tests", "OverridingXPoint.xml");
    }

    @Test
    public void testOverride() {
        ComponentWithXPoint co = (ComponentWithXPoint) Framework.getRuntime().getComponent(
                ComponentWithXPoint.NAME);
        DummyContribution[] contribs = co.getContributions();
        assertEquals(2, contribs.length);
        assertSame(contribs[0].getClass(), DummyContribution.class);
        assertSame(contribs[1].getClass(), DummyContributionOverriden.class);
        assertEquals("XP contrib", contribs[0].message);
        assertEquals("OverXP contrib", contribs[1].message);
        assertEquals("My duty is to override", ((DummyContributionOverriden) contribs[1]).name);
    }

}

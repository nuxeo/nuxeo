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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/** @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> */
public class TestExtensionPoint extends NXRuntimeTestCase {

    @Test
    public void testOverride() throws Exception {
        deployContrib("org.nuxeo.runtime.test.tests", "BaseXPoint.xml");
        deployContrib("org.nuxeo.runtime.test.tests", "OverridingXPoint.xml");
        ComponentWithXPoint co = ComponentWithXPoint.instance;
        assertEquals(2, co.contribs.size());
        DummyContributionOverriden overxpContrib = (DummyContributionOverriden) co.contribs.get("OverXP contrib");
        assertEquals("My duty is to override", overxpContrib.name);
    }

    @Test
    public void testParameters() throws Exception {
        Framework.getProperties().put("comp1", "set in fmw scope");
        deployContrib("org.nuxeo.runtime.test.tests", "BaseXPoint.xml");
        deployContrib("org.nuxeo.runtime.test.tests", "BaseExtensionParameters.xml");
        ComponentWithXPoint co = ComponentWithXPoint.instance;
        DummyContribution contrib = co.contribs.get("test");
        assertEquals("set in fmw scope", contrib.comp1); // fmw is defined in comp and taken from runtime
        assertEquals("set in xp scope", contrib.comp2); // comp is defined in cimp and  overriden in xp
        assertEquals("set in xt scope", contrib.xp); // xp is defined in xp and  overriden in xt
        assertEquals("set in xt scope", contrib.xt1); // xt is defined in xt
        assertEquals("set in descriptor", contrib.xt2); // xt is defined in descriptor
    }

}

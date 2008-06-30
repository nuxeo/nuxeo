/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestExtensionPoint extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("BaseXPoint.xml");
        deployContrib("OverridingXPoint.xml");
    }


    public void testOverride() {
        ComponentWithXPoint co = (ComponentWithXPoint)Framework.getRuntime().getComponent(ComponentWithXPoint.NAME);
        ContributionTest[] contribs = co.getContributions();
        assertEquals(2, contribs.length);
        assertTrue(contribs[0].getClass() == ContributionTest.class);
        assertTrue(contribs[1].getClass() == ContributionTestOverrided.class);
        System.out.println(((ContributionTest)contribs[0]).message);
        System.out.println(((ContributionTest)contribs[1]).message);
        System.out.println(((ContributionTestOverrided)contribs[1]).name);
    }

}

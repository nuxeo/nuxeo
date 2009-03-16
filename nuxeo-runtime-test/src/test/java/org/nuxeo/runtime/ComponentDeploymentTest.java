/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

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
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.runtime.test.tests", "MyComp1.xml");
        deployContrib("org.nuxeo.runtime.test.tests", "MyComp2.xml");
    }

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

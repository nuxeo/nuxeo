/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.runtime.registry;

import junit.framework.Assert;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class RegistryTest extends NXRuntimeTestCase {

    public void testReload() throws Exception {
        deployContrib("org.nuxeo.runtime.test.tests", "CompA.xml");

        Assert.assertNull(Framework.getRuntime().getComponent(
                new ComponentName("CompA")));

        deployContrib("org.nuxeo.runtime.test.tests", "CompB.xml");

        Assert.assertNull(Framework.getRuntime().getComponent(
                new ComponentName("CompA")));

        deployContrib("org.nuxeo.runtime.test.tests", "CompC.xml");

        Assert.assertNotNull(Framework.getRuntime().getComponent(
                new ComponentName("CompA")));
        Assert.assertNotNull(Framework.getRuntime().getComponent(
                new ComponentName("CompB")));

        undeployContrib("org.nuxeo.runtime.test.tests", "CompC.xml");
        undeployContrib("org.nuxeo.runtime.test.tests", "CompA.xml");
        undeployContrib("org.nuxeo.runtime.test.tests", "CompB.xml");

        Assert.assertNull(Framework.getRuntime().getComponent(
                new ComponentName("CompA")));
        Assert.assertNull(Framework.getRuntime().getComponent(
                new ComponentName("CompB")));

        deployContrib("org.nuxeo.runtime.test.tests", "CompA.xml");

        Assert.assertNull(Framework.getRuntime().getComponent(
                new ComponentName("CompA")));

        deployContrib("org.nuxeo.runtime.test.tests", "CompB.xml");

        Assert.assertNull(Framework.getRuntime().getComponent(
                new ComponentName("CompA")));

        deployContrib("org.nuxeo.runtime.test.tests", "CompC.xml");

        Assert.assertNotNull(Framework.getRuntime().getComponent(
                new ComponentName("CompA")));
        Assert.assertNotNull(Framework.getRuntime().getComponent(
                new ComponentName("CompB")));
        Assert.assertNotNull(Framework.getRuntime().getComponent(
                new ComponentName("CompC")));

    }

}

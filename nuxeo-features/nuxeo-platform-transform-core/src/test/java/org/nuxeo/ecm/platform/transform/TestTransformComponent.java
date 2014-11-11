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
 * $Id: TestTransformComponent.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestTransformComponent extends NXRuntimeTestCase {
    private TransformServiceCommon service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.transform.tests",
                "PlatformService.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.tests",
                "DefaultPlatform.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.tests",
                "nxmimetype-service.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.tests",
                "nxtransform-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.tests",
                "nxtransform-test-plugins-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.tests",
                "nxtransform-platform-contrib.xml");
        service = Framework.getLocalService(TransformServiceCommon.class);
    }

    public void testPlugin() {
        Plugin plugin = service.getPluginByName("any2pdf");
        assertNotNull(plugin);
    }

    public void testTransformerFixtures() {
        Transformer transformer = service.getTransformerByName("any2pdf");
        assertNotNull(transformer);

        assertEquals("any2pdf", transformer.getName());

        List<Plugin> chain = transformer.getPluginChains();
        assertEquals(1, chain.size());
        assertNotNull(chain.get(0));

        Map<String, Map<String, Serializable>> options = transformer.getDefaultOptions();
        assertEquals(1, options.size());

        Map<String, Serializable> pluginOptions = options.get("any2pdf");
        assertEquals("anotherhost", pluginOptions.get("host"));
        assertEquals("9900", pluginOptions.get("port"));

        assertEquals("application/pdf", transformer.getMimeTypeDestination());
        List<String> mtypes = transformer.getMimeTypeSources();
        assertEquals(3, mtypes.size());

        assertTrue(mtypes.contains("application/msword"));
        assertTrue(mtypes.contains("application/vnd.ms-powerpoint"));
        assertTrue(mtypes.contains("application/vnd.ms-excel"));
    }

    public void testMimetypeSupportByPlugin() {
        assertTrue(service.isMimetypeSupportedByPlugin("any2pdf",
                "application/msword"));
        assertFalse(service.isMimetypeSupportedByPlugin("any2pdf",
                "a wrong mimetype"));
    }

}

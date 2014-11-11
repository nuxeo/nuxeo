/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class TestPageProviderService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-contrib.xml");
    }

    public void testRegistration() throws Exception {
        PageProviderService service = Framework.getService(PageProviderService.class);
        assertNotNull(service);

        assertNull(service.getPageProviderDefinition("foo"));

        PageProviderDefinition def = service.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(def);
        assertEquals("CURRENT_DOCUMENT_CHILDREN", def.getName());
        // TODO: test given provider information
    }

}

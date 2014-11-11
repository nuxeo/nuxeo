/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.html;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.resources.ResourceType;
import org.nuxeo.theme.types.TypeFamily;

public class TestResourceExpansion extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.html.tests", "resource-expansion.xml");
    }

    public void testResourceExpansion() {
        String RESOURCE_NAME = "resource-with-partial-urls.css";
        ResourceType resource = (ResourceType) Manager.getTypeRegistry().lookup(
                TypeFamily.RESOURCE, RESOURCE_NAME);
        assertEquals(RESOURCE_NAME, resource.getName());
        assertEquals("resource.css", resource.getPath());
        assertEquals("/nuxeo/css/", resource.getContextPath());
    }
}

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

package org.nuxeo.theme.test.resources;

import java.util.List;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.themes.ThemeManager;

public class TestResourceOrdering extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "resource-ordering.xml");
    }

    public void testResourceOrdering() {
        ThemeManager themeManager = Manager.getThemeManager();
        List<String> ordering = themeManager.getResourceOrdering();
        assertTrue(ordering.indexOf("1") > ordering.indexOf("5"));
        assertTrue(ordering.indexOf("2") > ordering.indexOf("1"));
        assertTrue(ordering.indexOf("2") > ordering.indexOf("3"));
        assertTrue(ordering.indexOf("3") > ordering.indexOf("4"));
        assertTrue(ordering.indexOf("5") > ordering.indexOf("6"));
        assertTrue(ordering.indexOf("7") > ordering.indexOf("2"));
    }
}

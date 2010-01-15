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

package org.nuxeo.theme.test.webwidgets;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.webwidgets.DecorationType;
import org.nuxeo.theme.webwidgets.Service;

public class TestProviderType extends NXRuntimeTestCase {

    private Service service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core", "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.ecm.core.persistence", "OSGI-INF/persistence-service.xml");
        deployContrib("org.nuxeo.theme.webwidgets", "OSGI-INF/nxthemes-webwidgets-service.xml");
        deployContrib("org.nuxeo.theme.test.webwidgets", "webwidgets-contrib.xml");
        service = (Service) runtime.getComponent(Service.ID);
    }

    @Override
    public void tearDown() throws Exception {
        service = null;
        super.tearDown();
    }

    public void testGetWindowDecoration() {
        DecorationType decorationType = service.getDecorationType("test");
        assertEquals("<span>%WIDGET_NAME%</span>\n",
                decorationType.getWidgetDecoration("view").getContent());
        assertEquals("<span>%ACTION_EDIT_PREFERENCES%</span>\n",
                decorationType.getWidgetDecoration("edit").getContent());
    }

}

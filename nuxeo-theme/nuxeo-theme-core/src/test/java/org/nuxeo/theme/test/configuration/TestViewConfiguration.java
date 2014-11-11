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

package org.nuxeo.theme.test.configuration;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.views.ViewType;

public class TestViewConfiguration extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "model-config.xml");
        deployContrib("org.nuxeo.theme.core.tests", "view-config.xml");
    }

    public void testRegisterView() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();

        ViewType view1 = (ViewType) typeRegistry.lookup(TypeFamily.VIEW,
                "widget/page/page frame/menu item/default/*/jsf-facelets");
        assertNotNull(view1);

        assertEquals("page", view1.getElementType().getTypeName());
        assertEquals("widget", view1.getFormatType().getTypeName());
        assertEquals("menu item", view1.getModelType().getTypeName());
        assertEquals("nxthemes/jsf/widgets/page-frame.xml", view1.getTemplate());
        assertEquals("style.css", view1.getResources().get(0));
        assertEquals("script.js", view1.getResources().get(1));
        assertEquals("jsf-facelets", view1.getTemplateEngine());
    }

    public void testRegisterViewMerge() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();

        ViewType view1 = (ViewType) typeRegistry.lookup(TypeFamily.VIEW,
                "widget/*/menu/*/default/*/jsf-facelets");
        assertNotNull(view1);

        assertEquals("widget", view1.getFormatType().getTypeName());
        assertEquals("nxthemes/jsf/widgets/new-menu.xml", view1.getTemplate());
        assertEquals("new-style.css", view1.getResources().get(0));
        assertEquals("script.js", view1.getResources().get(1));
        assertEquals("style-addon.css", view1.getResources().get(2));
        assertEquals("script-addon.js", view1.getResources().get(3));
        assertEquals("other-script-addon.js", view1.getResources().get(4));
        assertEquals("jsf-facelets", view1.getTemplateEngine());
    }

}

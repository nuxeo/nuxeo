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

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.views.ViewType;

public class TestViewConfiguration extends NXRuntimeTestCase {

    private ViewType view1;

    private TypeRegistry typeRegistry;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deploy("nxthemes-core-service.xml");
        deploy("nxthemes-core-contrib.xml");
        deploy("model-config.xml");
        deploy("view-config.xml");

        ThemeService themeService = (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);
        typeRegistry = (TypeRegistry) themeService.getRegistry("types");
    }

    public void testRegisterView() throws Exception {
        view1 = (ViewType) typeRegistry.lookup(TypeFamily.VIEW,
                "widget/page/page frame/menu item/default/*");
        assertNotNull(view1);

        assertEquals("page", view1.getElementType().getTypeName());
        assertEquals("widget", view1.getFormatType().getTypeName());
        assertEquals("menu item", view1.getModelType().getTypeName());
        assertEquals("nxthemes/jsf/widgets/page-frame.xml", view1.getTemplate());
        assertEquals("css/style.css", view1.getResources()[0]);
        assertEquals("script/script.js", view1.getResources()[1]);
    }

}

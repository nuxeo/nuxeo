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
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class TestEngineConfiguration extends NXRuntimeTestCase {

    private EngineType engine1;

    private EngineType engine2;

    private TypeRegistry typeRegistry;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deploy("nxthemes-core-service.xml");
        deploy("nxthemes-core-contrib.xml");
        deploy("engine-config.xml");
        ThemeService themeService = (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);
        typeRegistry = (TypeRegistry) themeService.getRegistry("types");
    }

    public void testRegisterEngine1() throws Exception {
        // engine 1
        engine1 = (EngineType) typeRegistry.lookup(TypeFamily.ENGINE,
                "test-engine");
        assertNotNull(engine1);
        assertEquals("test-engine", engine1.getTypeName());
        assertEquals("[widget, style]",
                engine1.getRenderers().get("theme").getFilters().toString());
    }

    public void testRegisterEngine2() throws Exception {
        // engine 2
        engine2 = (EngineType) typeRegistry.lookup(TypeFamily.ENGINE,
                "test-engine-2");
        assertNotNull(engine2);
        assertEquals("test-engine-2", engine2.getTypeName());
        assertEquals("[widget, style, page filter]",
                engine2.getRenderers().get("page").getFilters().toString());
    }

}

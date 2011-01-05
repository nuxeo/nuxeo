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
import org.nuxeo.theme.models.ModelType;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class TestModelConfiguration extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "model-config.xml");
    }

    public void testRegisterModel() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        // model 1
        ModelType model1 = (ModelType) typeRegistry.lookup(TypeFamily.MODEL,
                "test model 1");
        assertNotNull(model1);
        assertEquals("test model 1", model1.getTypeName());
        assertEquals("org.nuxeo.theme.test.DummyModel", model1.getClassName());
    }

}

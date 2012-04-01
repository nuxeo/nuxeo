/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestTypeService.java 26932 2007-11-07 15:05:49Z gracinet $
 */

package org.nuxeo.ecm.core.schema;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:sf@nuxeo.com">Stefane Fermigier</a>
 */
public class TestTypeService extends NXRuntimeTestCase {

    private TypeService ts;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        ts = (TypeService) Framework.getRuntime().getComponent(
                TypeService.NAME);
    }

    @Test
    public void testTypeService() {
        assertNull(ts.getConfiguration());
        assertNotNull(ts.getSchemaLoader());
        assertNotNull(ts.getTypeManager());
    }

}

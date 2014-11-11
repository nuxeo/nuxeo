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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.schema.types;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

public class TestSimpleType {

    SimpleType simpleType;

    @Before
    public void setUp() {
        simpleType = new SimpleTypeImpl(StringType.INSTANCE, SchemaNames.BUILTIN, "type name");
    }

    @After
    public void tearDown() {
        simpleType = null;
    }

    @Test
    public void testIsPrimitive() {
        assertFalse(simpleType.isPrimitive());
    }

    @Test
    public void testIsSimpleType() {
        assertTrue(simpleType.isSimpleType());
    }

    @Test
    public void testValidateNull() throws Exception {
        assertTrue(simpleType.validate(null));
    }

}

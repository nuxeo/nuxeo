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
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl.osm;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.StringProperty;

public class TestTypeAnnotationRegistry extends TestCase {

    TypeAnnotationRegistry<String> mgr;

    @Override
    public void setUp() {
        mgr = new TypeAnnotationRegistry<String>();
    }

    public void test() {
        mgr.put(Property.class, "prop");
        mgr.put(ComplexProperty.class, "cprop");

        assertEquals("cprop", mgr.get(MapProperty.class));
        assertEquals("prop", mgr.get(StringProperty.class));

        mgr.remove(Property.class);

        assertEquals("cprop", mgr.get(MapProperty.class));
        assertNull(mgr.get(StringProperty.class));
    }

}

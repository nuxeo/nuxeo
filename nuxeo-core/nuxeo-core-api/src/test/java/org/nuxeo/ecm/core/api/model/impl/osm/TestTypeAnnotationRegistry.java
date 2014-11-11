/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

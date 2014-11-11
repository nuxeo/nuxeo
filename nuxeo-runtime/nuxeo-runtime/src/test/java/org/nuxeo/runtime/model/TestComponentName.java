/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.runtime.model;

import junit.framework.TestCase;

public class TestComponentName extends TestCase {

    public void test() {
        ComponentName cn1 = new ComponentName("foo:bar");
        ComponentName cn2 = new ComponentName("foo", "bar");
        ComponentName cn3 = new ComponentName("fu:baz");

        assertEquals("foo", cn1.getType());
        assertEquals("bar", cn1.getName());
        assertEquals("foo:bar", cn1.getRawName());

        assertEquals(cn1, cn2);
        assertEquals(cn1.hashCode(), cn2.hashCode());
        assertEquals(cn1.toString(), cn2.toString());

        assertNotNull(cn1);
        assertFalse(cn1.equals(cn3));
        assertFalse(cn3.equals(cn1));
    }

}

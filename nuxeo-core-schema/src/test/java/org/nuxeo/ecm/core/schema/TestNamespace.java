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

package org.nuxeo.ecm.core.schema;

import junit.framework.TestCase;

public class TestNamespace extends TestCase {

    public void testNameSpace() {
        Namespace ns1 = new Namespace("uri", "prefix");
        Namespace ns2 = new Namespace("uri", "prefix");
        Namespace ns3 = new Namespace("uri", null);
        Namespace ns4 = new Namespace("uri2", "prefix");

        assertTrue(ns1.hasPrefix());
        assertFalse(ns3.hasPrefix());

        assertEquals(ns1, ns1);
        assertEquals(ns1, ns2);
        assertEquals(ns1.hashCode(), ns2.hashCode());
        assertEquals(ns1.toString(), ns2.toString());

        assertNotNull(ns1);
        assertFalse(ns1.equals(ns3));
        assertFalse(ns3.equals(ns1));
        assertFalse(ns1.equals(ns4));
        assertFalse(ns4.equals(ns1));

        assertFalse(ns1.toString().equals(ns3.toString()));
        assertFalse(ns1.toString().equals(ns4.toString()));
    }

}

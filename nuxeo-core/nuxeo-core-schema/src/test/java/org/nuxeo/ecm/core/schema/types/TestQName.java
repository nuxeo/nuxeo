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

package org.nuxeo.ecm.core.schema.types;

import junit.framework.TestCase;

public class TestQName extends TestCase {

    public void testQName() {
        QName qname = new QName("local name", "prefix");

        assertEquals("local name", qname.getLocalName());
        assertEquals("prefix", qname.getPrefix());
        assertEquals("prefix:local name", qname.getPrefixedName());
        assertEquals("prefix:local name", qname.toString());
        assertEquals(qname, qname);

        QName qname2 = new QName("local name", "prefix");
        assertEquals(qname, qname2);
        assertEquals(qname.hashCode(), qname2.hashCode());
        assertSame(qname.getLocalName(), qname2.getLocalName());
        assertSame(qname.getPrefix(), qname2.getPrefix());
        assertSame(qname.getPrefixedName(), qname2.getPrefixedName());

        QName qname3 = new QName("another name", "prefix");
        assertFalse(qname.equals(qname3));
        assertFalse(qname3.equals(qname));

        QName qname4 = new QName("local name", "another prefix");
        assertFalse(qname.equals(qname4));
        assertFalse(qname4.equals(qname));

        QName qname5 = new QName("local name", "");
        assertFalse(qname.equals(qname5));
        assertFalse(qname5.equals(qname));

        QName qname6 = new QName("local name");
        assertFalse(qname.equals(qname6));
        assertFalse(qname6.equals(qname));
        assertEquals(qname5, qname6);
    }

    public void testQNameValueOfWithPrefix() {
        QName qname = new QName("local name", "prefix");

        QName qname1 = QName.valueOf("prefix:local name");
        assertEquals(qname, qname1);

        QName qname2 = QName.valueOf("local name", "prefix");
        assertEquals(qname, qname2);
    }

    public void testQNameValueOfWithNoPrefix() {
        QName qname = new QName("local name");

        QName qname1 = QName.valueOf("local name");
        assertEquals(qname, qname1);

        QName qname2 = QName.valueOf("local name");
        assertEquals(qname, qname2);
    }

}

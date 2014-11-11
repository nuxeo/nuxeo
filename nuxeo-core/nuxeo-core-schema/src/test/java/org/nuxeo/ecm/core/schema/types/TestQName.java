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

import org.junit.Test;
import static org.junit.Assert.*;

public class TestQName {

    @Test
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

    @Test
    public void testQNameValueOfWithPrefix() {
        QName qname = new QName("local name", "prefix");

        QName qname1 = QName.valueOf("prefix:local name");
        assertEquals(qname, qname1);

        QName qname2 = QName.valueOf("local name", "prefix");
        assertEquals(qname, qname2);
    }

    @Test
    public void testQNameValueOfWithNoPrefix() {
        QName qname = new QName("local name");

        QName qname1 = QName.valueOf("local name");
        assertEquals(qname, qname1);

        QName qname2 = QName.valueOf("local name");
        assertEquals(qname, qname2);
    }

}

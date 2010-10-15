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

package org.nuxeo.theme.test.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import junit.framework.TestCase;

import org.nuxeo.theme.properties.OrderedProperties;

public class TestOrderedProperties extends TestCase {

    public void testPut() {
        Properties properties = new OrderedProperties();
        properties.put("1", "1");
        properties.put("2", "2");
        properties.put("3", "3");
        properties.put("4", "4");

        Enumeration<?> keys = properties.propertyNames();
        assertEquals("1", keys.nextElement());
        assertEquals("2", keys.nextElement());
        assertEquals("3", keys.nextElement());
        assertEquals("4", keys.nextElement());
    }

    public void testSetPropertyt() {
        Properties properties = new OrderedProperties();
        properties.setProperty("1", "1");
        properties.setProperty("2", "2");
        properties.setProperty("3", "3");
        properties.setProperty("4", "4");

        Enumeration<?> keys = properties.propertyNames();
        assertEquals("1", keys.nextElement());
        assertEquals("2", keys.nextElement());
        assertEquals("3", keys.nextElement());
        assertEquals("4", keys.nextElement());
    }

    public void testLoad() throws IOException {
        Properties properties = new OrderedProperties();

        InputStream in = getClass().getClassLoader().getResourceAsStream(
                "ordered.properties");
        properties.load(in);

        Enumeration<?> keys = properties.propertyNames();
        assertEquals("1", keys.nextElement());
        assertEquals("2", keys.nextElement());
        assertEquals("3", keys.nextElement());
        assertEquals("4", keys.nextElement());

        in.close();
    }

}

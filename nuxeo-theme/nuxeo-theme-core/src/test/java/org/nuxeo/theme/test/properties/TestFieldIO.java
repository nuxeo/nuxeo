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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.test.DummyObject;

public class TestFieldIO extends TestCase {

    public void testUpdateStringFieldsFromProperties() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("width", "200px");
        properties.setProperty("height", "100px");

        DummyObject object = new DummyObject();
        FieldIO.updateFieldsFromProperties(object, properties);
        assertEquals("200px", object.width);
        assertEquals("100px", object.height);
    }

    public void testUpdateBooleanFieldsFromProperties() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("selected", "true");

        DummyObject object = new DummyObject();
        FieldIO.updateFieldsFromProperties(object, properties);
        assertTrue(object.selected);

        properties.setProperty("selected", "false");
        FieldIO.updateFieldsFromProperties(object, properties);
        assertFalse(object.selected);

        properties.setProperty("booleanClass", "true");
        FieldIO.updateFieldsFromProperties(object, properties);
        assertTrue(object.booleanClass);

        properties.setProperty("booleanClass", "false");
        FieldIO.updateFieldsFromProperties(object, properties);
        assertFalse(object.booleanClass);
    }

    public void testUpdateIntegerFieldsFromProperties() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("maxItems", "2");

        DummyObject object = new DummyObject();
        FieldIO.updateFieldsFromProperties(object, properties);
        assertTrue(object.maxItems == 2);

        properties.setProperty("maxItems", "3");
        FieldIO.updateFieldsFromProperties(object, properties);
        assertTrue(object.maxItems == 3);

        properties.setProperty("integerClass", "2");
        FieldIO.updateFieldsFromProperties(object, properties);
        assertTrue(object.integerClass == 2);

        properties.setProperty("integerClass", "3");
        FieldIO.updateFieldsFromProperties(object, properties);
        assertTrue(object.integerClass == 3);
    }

    public void testUpdateListFieldsFromProperties() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("stringSequence", "1,2,3");

        List<String> expected = new ArrayList<String>();
        expected.add("1");
        expected.add("2");
        expected.add("3");

        DummyObject object = new DummyObject();
        FieldIO.updateFieldsFromProperties(object, properties);
        assertEquals(expected, object.stringSequence);
    }

}

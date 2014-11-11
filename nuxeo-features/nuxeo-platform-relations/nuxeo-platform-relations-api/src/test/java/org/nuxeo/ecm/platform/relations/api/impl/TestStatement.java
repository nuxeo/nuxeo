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
 * $Id: TestStatement.java 22853 2007-07-22 21:09:50Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidPredicateException;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidStatementException;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidSubjectException;

@SuppressWarnings({"ResultOfObjectAllocationIgnored"})
public class TestStatement extends TestCase {

    private Resource subject;

    private Resource predicate;

    private LiteralImpl literal;

    private Resource object;

    private BlankImpl blank;

    private Resource propertyResource;

    private Node[] propertyValues;

    private Map<Resource, Node[]> properties;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        subject = NodeFactory.createResource("http://toto");
        predicate = NodeFactory.createResource("http://says");
        literal = NodeFactory.createLiteral("Hello");
        object = NodeFactory.createResource("http://hello");
        blank = NodeFactory.createBlank("blank");
        properties = new HashMap<Resource, Node[]>();
        propertyResource = NodeFactory.createResource("http://heardBy");
        propertyValues = new Node[] { NodeFactory.createResource("http://God"),
                NodeFactory.createLiteral("echo") };
        properties.put(propertyResource, propertyValues);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        properties = null;
        propertyValues = null;
    }

    public void testCreateStatementNull() {
        Statement st = new StatementImpl();
        assertNotNull(st);
        assertNull(st.getSubject());
        assertNull(st.getPredicate());
        assertNull(st.getObject());

        // change null values
        st = new StatementImpl(null, null, null);
        assertNotNull(st);
        assertNull(st.getSubject());
        assertNull(st.getPredicate());
        assertNull(st.getObject());

        st = new StatementImpl(null, predicate, object);
        assertNotNull(st);
        assertNull(st.getSubject());
        assertEquals(st.getPredicate(), predicate);
        assertEquals(st.getObject(), object);

        st = new StatementImpl(subject, null, object);
        assertNotNull(st);
        assertEquals(st.getSubject(), subject);
        assertNull(st.getPredicate());
        assertEquals(st.getObject(), object);

        st = new StatementImpl(subject, predicate, null);
        assertNotNull(st);
        assertEquals(st.getSubject(), subject);
        assertEquals(st.getPredicate(), predicate);
        assertNull(st.getObject());
    }

    public void testCreateStatementLiteral() {
        Statement st = new StatementImpl(subject, predicate, literal);
        assertNotNull(st);
        assertEquals(st.getSubject(), subject);
        assertEquals(st.getPredicate(), predicate);
        assertEquals(st.getObject(), literal);
    }

    public void testCreateStatementBlank() {
        Statement st = new StatementImpl(subject, predicate, blank);
        assertNotNull(st);
        assertEquals(st.getSubject(), subject);
        assertEquals(st.getPredicate(), predicate);
        assertEquals(st.getObject(), blank);
    }

    public void testCreateStatementResource() {
        Statement st = new StatementImpl(subject, predicate, object);
        assertNotNull(st);
        assertEquals(st.getSubject(), subject);
        assertEquals(st.getPredicate(), predicate);
        assertEquals(st.getObject(), object);
    }

    public void testCreateStatementBlankSubject() {
        Statement st = new StatementImpl(blank, predicate, object);
        assertNotNull(st);
        assertEquals(st.getSubject(), blank);
        assertEquals(st.getPredicate(), predicate);
        assertEquals(st.getObject(), object);
    }

    public void testCreateStatementInvalidSubjectException() {
        try {
            new StatementImpl(literal, predicate, literal);
            fail("Should have raised an InvalidSubjectException");
        } catch (InvalidSubjectException e) {
        }
    }

    public void testCreateStatementInvalidPredicateException() {
        try {
            new StatementImpl(subject, literal, literal);
            fail("Should have raised an InvalidPredicateException");
        } catch (InvalidPredicateException e) {
        }
    }

    public void testCreateStatementInvalidStatementException() {
        try {
            new StatementImpl(literal, blank, literal);
            fail("Should have raised an InvalidStatementException");
        } catch (InvalidStatementException e) {
        }
    }

    @SuppressWarnings({"ObjectEqualsNull"})
    public void testEquals() {
        Statement st1 = new StatementImpl(subject, predicate, literal);
        Statement st2 = new StatementImpl(subject, predicate, literal);
        Statement st3 = new StatementImpl(subject, predicate, object);
        assertEquals(st1, st1);
        assertEquals(st1, st2);
        assertEquals(st1.hashCode(), st2.hashCode());
        assertFalse(st1.equals(st3));
        assertFalse(st1.equals(null));
    }

    public void testGetSetProperties() {
        Statement st = new StatementImpl(subject, predicate, literal);
        assertNotNull(st.getProperties());
        assertEquals(0, st.getProperties().size());
        st.setProperties(properties);
        assertEquals(properties, st.getProperties());
        assertEquals(1, st.getProperties().size());
    }

    public void testGetProperty() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(propertyValues[0], st.getProperty(propertyResource));
        assertNull(st.getProperty(null));
        assertNull(st.getProperty(NodeFactory.createResource("http://foo")));
    }

    public void testGetPropertyList() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(propertyValues, st.getProperties(propertyResource));
        assertNull(st.getProperties(null));
        assertNull(st.getProperties(NodeFactory.createResource("http://foo")));
    }

    public void testSetProperty() {
        Statement st = new StatementImpl(subject, predicate, literal);
        Node godProperty = NodeFactory.createResource("http://God");
        st.setProperty(propertyResource, godProperty);
        Node[] values = new Node[] { godProperty };
        assertEquals(values.length, st.getProperties(propertyResource).length);
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], st.getProperties(propertyResource)[i]);
        }
        Node echoProperty = NodeFactory.createLiteral("echo");
        st.setProperty(propertyResource, echoProperty);
        Node[] newValues = new Node[] { echoProperty };
        assertEquals(newValues.length, st.getProperties(propertyResource).length);
        for (int i = 0; i < newValues.length; i++) {
            assertEquals(newValues[i], st.getProperties(propertyResource)[i]);
        }
    }

    public void testSetPropertyList() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(propertyResource, propertyValues);
        assertEquals(propertyValues, st.getProperties(propertyResource));
        Node[] otherValues = new Node[] { NodeFactory.createResource("http://foo") };
        st.setProperties(propertyResource, otherValues);
        assertEquals(otherValues, st.getProperties(propertyResource));
    }

    public void testDeletePropertiesAll() {
        Statement st = new StatementImpl(subject, predicate, literal);
        assertNotNull(st.getProperties());
        assertEquals(0, st.getProperties().size());
        st.setProperties(properties);
        st.setProperties(NodeFactory.createResource("http://bar"),
                propertyValues);
        assertEquals(2, st.getProperties().size());
        st.deleteProperties();
        assertEquals(0, st.getProperties().size());
    }

    public void testDeleteProperty() {
        Statement st = new StatementImpl(subject, predicate, literal);
        assertNotNull(st.getProperties());
        assertEquals(0, st.getProperties().size());
        st.setProperties(properties);
        st.setProperties(NodeFactory.createResource("http://bar"),
                propertyValues);
        assertEquals(2, st.getProperties().size());
        st.deleteProperty(null);
        assertEquals(2, st.getProperties().size());
        st.deleteProperty(NodeFactory.createResource("http://bar"));
        assertEquals(1, st.getProperties().size());
        assertEquals(properties, st.getProperties());
    }

    public void testDeleteProperties() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(properties, st.getProperties());
        st.deleteProperties(propertyResource, null);
        assertEquals(properties, st.getProperties());
        st.deleteProperties(null, propertyValues);
        assertEquals(properties, st.getProperties());
        Node[] deleteValues = new Node[] {
                NodeFactory.createResource("http://God") };
        st.deleteProperties(propertyResource, deleteValues);
        Node[] newValues = new Node[] { NodeFactory.createLiteral("echo") };
        assertEquals(newValues.length, st.getProperties(propertyResource).length);
        for (int i = 0; i < newValues.length; i++) {
            assertEquals(newValues[i], st.getProperties(propertyResource)[i]);
        }
        st.deleteProperties(propertyResource, propertyValues);
        assertEquals(0, st.getProperties().size());
    }

    public void testAddProperties() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(properties, st.getProperties());
        st.addProperties(null);
        assertEquals(properties, st.getProperties());
        Map<Resource, Node[]> newProperties = new HashMap<Resource, Node[]>();
        // set duplicates
        newProperties.putAll(properties);
        Resource otherPropertyResource = NodeFactory.createResource("http://foo");
        Node[] otherValues = new Node[] { NodeFactory.createLiteral("http://bar") };
        newProperties.put(otherPropertyResource, otherValues);
        st.addProperties(newProperties);
        assertEquals(newProperties, st.getProperties());
        assertEquals(otherValues, st.getProperties(otherPropertyResource));
        assertEquals(propertyValues, st.getProperties(propertyResource));
    }

    public void testAddProperty() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(properties, st.getProperties());
        st.addProperty(null, null);
        assertEquals(properties, st.getProperties());
        Resource otherPropertyResource = NodeFactory.createResource("http://foo");
        Literal newValue = NodeFactory.createLiteral("http://bar");
        st.addProperty(otherPropertyResource, null);
        assertEquals(properties, st.getProperties());
        st.addProperty(null, newValue);
        assertEquals(properties, st.getProperties());
        st.addProperty(otherPropertyResource, newValue);
        assertEquals(propertyValues, st.getProperties(propertyResource));
        Node[] otherValues = new Node[] { newValue };
        assertEquals(otherValues.length, st.getProperties(otherPropertyResource).length);
        for (int i = 0; i < otherValues.length; i++) {
            assertEquals(otherValues[i], st.getProperties(otherPropertyResource)[i]);
        }
    }

    public void testAddPropertyList() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(propertyResource, propertyValues);
        Resource otherPropertyResource = NodeFactory.createResource("http://foo");
        assertEquals(propertyValues, st.getProperties(propertyResource));
        assertNull(st.getProperties(otherPropertyResource));
        Literal newValue = NodeFactory.createLiteral("http://bar");
        Node[] otherValues = new Node[] { newValue };
        st.addProperties(otherPropertyResource, otherValues);
        assertEquals(otherValues, st.getProperties(otherPropertyResource));
        assertEquals(propertyValues, st.getProperties(propertyResource));
    }

    public void testClone() throws CloneNotSupportedException {
        StatementImpl st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(properties, st.getProperties());
        Statement clone = (Statement) st.clone();
        assertEquals(clone, st);
        //assertEquals(properties, st.getProperties());

        // add new properties to clone
        Map<Resource, Node[]> newProperties = new HashMap<Resource, Node[]>();
        Node[] newValues = new Node[] { NodeFactory.createLiteral("http://bar") };
        newProperties.put(propertyResource, newValues);
        clone.setProperties(newProperties);

        // make sure old statement properties have not changed
        assertEquals(properties, st.getProperties());
        assertEquals(newProperties, clone.getProperties());
    }

}

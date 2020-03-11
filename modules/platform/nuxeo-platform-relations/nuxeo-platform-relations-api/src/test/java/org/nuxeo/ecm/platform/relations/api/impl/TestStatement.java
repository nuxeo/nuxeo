/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestStatement.java 22853 2007-07-22 21:09:50Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidPredicateException;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidStatementException;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidSubjectException;

@SuppressWarnings({ "ResultOfObjectAllocationIgnored" })
public class TestStatement {

    private Resource subject;

    private Resource predicate;

    private LiteralImpl literal;

    private Resource object;

    private BlankImpl blank;

    private Resource propertyResource;

    private Node[] propertyValues;

    private Map<Resource, Node[]> properties;

    @Before
    public void setUp() throws Exception {
        subject = NodeFactory.createResource("http://toto");
        predicate = NodeFactory.createResource("http://says");
        literal = NodeFactory.createLiteral("Hello");
        object = NodeFactory.createResource("http://hello");
        blank = NodeFactory.createBlank("blank");
        properties = new HashMap<>();
        propertyResource = NodeFactory.createResource("http://heardBy");
        propertyValues = new Node[] { NodeFactory.createResource("http://God"), NodeFactory.createLiteral("echo") };
        properties.put(propertyResource, propertyValues);
    }

    @After
    public void tearDown() throws Exception {
        properties = null;
        propertyValues = null;
    }

    @Test
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

    @Test
    public void testCreateStatementLiteral() {
        Statement st = new StatementImpl(subject, predicate, literal);
        assertNotNull(st);
        assertEquals(st.getSubject(), subject);
        assertEquals(st.getPredicate(), predicate);
        assertEquals(st.getObject(), literal);
    }

    @Test
    public void testCreateStatementBlank() {
        Statement st = new StatementImpl(subject, predicate, blank);
        assertNotNull(st);
        assertEquals(st.getSubject(), subject);
        assertEquals(st.getPredicate(), predicate);
        assertEquals(st.getObject(), blank);
    }

    @Test
    public void testCreateStatementResource() {
        Statement st = new StatementImpl(subject, predicate, object);
        assertNotNull(st);
        assertEquals(st.getSubject(), subject);
        assertEquals(st.getPredicate(), predicate);
        assertEquals(st.getObject(), object);
    }

    @Test
    public void testCreateStatementBlankSubject() {
        Statement st = new StatementImpl(blank, predicate, object);
        assertNotNull(st);
        assertEquals(st.getSubject(), blank);
        assertEquals(st.getPredicate(), predicate);
        assertEquals(st.getObject(), object);
    }

    @Test
    public void testCreateStatementInvalidSubjectException() {
        try {
            new StatementImpl(literal, predicate, literal);
            fail("Should have raised an InvalidSubjectException");
        } catch (InvalidSubjectException e) {
        }
    }

    @Test
    public void testCreateStatementInvalidPredicateException() {
        try {
            new StatementImpl(subject, literal, literal);
            fail("Should have raised an InvalidPredicateException");
        } catch (InvalidPredicateException e) {
        }
    }

    @Test
    public void testCreateStatementInvalidStatementException() {
        try {
            new StatementImpl(literal, blank, literal);
            fail("Should have raised an InvalidStatementException");
        } catch (InvalidStatementException e) {
        }
    }

    @SuppressWarnings({ "ObjectEqualsNull" })
    @Test
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

    @Test
    public void testGetSetProperties() {
        Statement st = new StatementImpl(subject, predicate, literal);
        assertNotNull(st.getProperties());
        assertEquals(0, st.getProperties().size());
        st.setProperties(properties);
        assertEquals(properties, st.getProperties());
        assertEquals(1, st.getProperties().size());
    }

    @Test
    public void testGetProperty() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(propertyValues[0], st.getProperty(propertyResource));
        assertNull(st.getProperty(null));
        assertNull(st.getProperty(NodeFactory.createResource("http://foo")));
    }

    @Test
    public void testGetPropertyList() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(propertyValues, st.getProperties(propertyResource));
        assertNull(st.getProperties(null));
        assertNull(st.getProperties(NodeFactory.createResource("http://foo")));
    }

    @Test
    public void testSetProperty() {
        Statement st = new StatementImpl(subject, predicate, literal);
        Node godProperty = NodeFactory.createResource("http://God");
        st.setProperty(propertyResource, godProperty);
        Node[] values = { godProperty };
        assertEquals(values.length, st.getProperties(propertyResource).length);
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], st.getProperties(propertyResource)[i]);
        }
        Node echoProperty = NodeFactory.createLiteral("echo");
        st.setProperty(propertyResource, echoProperty);
        Node[] newValues = { echoProperty };
        assertEquals(newValues.length, st.getProperties(propertyResource).length);
        for (int i = 0; i < newValues.length; i++) {
            assertEquals(newValues[i], st.getProperties(propertyResource)[i]);
        }
    }

    @Test
    public void testSetPropertyList() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(propertyResource, propertyValues);
        assertEquals(propertyValues, st.getProperties(propertyResource));
        Node[] otherValues = { NodeFactory.createResource("http://foo") };
        st.setProperties(propertyResource, otherValues);
        assertEquals(otherValues, st.getProperties(propertyResource));
    }

    @Test
    public void testDeletePropertiesAll() {
        Statement st = new StatementImpl(subject, predicate, literal);
        assertNotNull(st.getProperties());
        assertEquals(0, st.getProperties().size());
        st.setProperties(properties);
        st.setProperties(NodeFactory.createResource("http://bar"), propertyValues);
        assertEquals(2, st.getProperties().size());
        st.deleteProperties();
        assertEquals(0, st.getProperties().size());
    }

    @Test
    public void testDeleteProperty() {
        Statement st = new StatementImpl(subject, predicate, literal);
        assertNotNull(st.getProperties());
        assertEquals(0, st.getProperties().size());
        st.setProperties(properties);
        st.setProperties(NodeFactory.createResource("http://bar"), propertyValues);
        assertEquals(2, st.getProperties().size());
        st.deleteProperty(null);
        assertEquals(2, st.getProperties().size());
        st.deleteProperty(NodeFactory.createResource("http://bar"));
        assertEquals(1, st.getProperties().size());
        assertEquals(properties, st.getProperties());
    }

    @Test
    public void testDeleteProperties() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(properties, st.getProperties());
        st.deleteProperties(propertyResource, null);
        assertEquals(properties, st.getProperties());
        st.deleteProperties(null, propertyValues);
        assertEquals(properties, st.getProperties());
        Node[] deleteValues = { NodeFactory.createResource("http://God") };
        st.deleteProperties(propertyResource, deleteValues);
        Node[] newValues = { NodeFactory.createLiteral("echo") };
        assertEquals(newValues.length, st.getProperties(propertyResource).length);
        for (int i = 0; i < newValues.length; i++) {
            assertEquals(newValues[i], st.getProperties(propertyResource)[i]);
        }
        st.deleteProperties(propertyResource, propertyValues);
        assertEquals(0, st.getProperties().size());
    }

    @Test
    public void testAddProperties() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(properties, st.getProperties());
        st.addProperties(null);
        assertEquals(properties, st.getProperties());
        Map<Resource, Node[]> newProperties = new HashMap<>();
        // set duplicates
        newProperties.putAll(properties);
        Resource otherPropertyResource = NodeFactory.createResource("http://foo");
        Node[] otherValues = { NodeFactory.createLiteral("http://bar") };
        newProperties.put(otherPropertyResource, otherValues);
        st.addProperties(newProperties);
        assertEquals(newProperties, st.getProperties());
        assertEquals(otherValues, st.getProperties(otherPropertyResource));
        assertEquals(propertyValues, st.getProperties(propertyResource));
    }

    @Test
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
        Node[] otherValues = { newValue };
        assertEquals(otherValues.length, st.getProperties(otherPropertyResource).length);
        for (int i = 0; i < otherValues.length; i++) {
            assertEquals(otherValues[i], st.getProperties(otherPropertyResource)[i]);
        }
    }

    @Test
    public void testAddPropertyList() {
        Statement st = new StatementImpl(subject, predicate, literal);
        st.setProperties(propertyResource, propertyValues);
        Resource otherPropertyResource = NodeFactory.createResource("http://foo");
        assertEquals(propertyValues, st.getProperties(propertyResource));
        assertNull(st.getProperties(otherPropertyResource));
        Literal newValue = NodeFactory.createLiteral("http://bar");
        Node[] otherValues = { newValue };
        st.addProperties(otherPropertyResource, otherValues);
        assertEquals(otherValues, st.getProperties(otherPropertyResource));
        assertEquals(propertyValues, st.getProperties(propertyResource));
    }

    @Test
    public void testClone() throws CloneNotSupportedException {
        StatementImpl st = new StatementImpl(subject, predicate, literal);
        st.setProperties(properties);
        assertEquals(properties, st.getProperties());
        Statement clone = (Statement) st.clone();
        assertEquals(clone, st);
        // assertEquals(properties, st.getProperties());

        // add new properties to clone
        Map<Resource, Node[]> newProperties = new HashMap<>();
        Node[] newValues = { NodeFactory.createLiteral("http://bar") };
        newProperties.put(propertyResource, newValues);
        clone.setProperties(newProperties);

        // make sure old statement properties have not changed
        assertEquals(properties, st.getProperties());
        assertEquals(newProperties, clone.getProperties());
    }

}

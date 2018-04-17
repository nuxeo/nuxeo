/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat
 */
package org.nuxeo.ecm.core.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.validation.DocumentValidationService.CTX_MAP_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolver;
import org.nuxeo.ecm.core.api.validation.ConstraintViolation;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.ObjectResolverConstraint;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolverService;
import org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver;
import org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver.Color;
import org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver.PrimaryColor;
import org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver.SecondaryColor;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-resolver-service-contrib.xml")
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestObjectResolverProperties {

    private static final String XPATH = "res:primaryColor";

    @Inject
    protected CoreSession session;

    @Inject
    protected ObjectResolverService references;

    @Inject
    protected SchemaManager metamodel;

    @Inject
    protected DocumentValidationService validator;

    protected DocumentModel doc;

    @Before
    public void setUp() {
        doc = session.createDocumentModel("/", "doc1", "TestResolver");
    }

    @Test
    public void testHasResolver() {
        assertNotNull(prop().getType().getObjectResolver());
        assertNotNull(prop().getObjectResolver());
        assertNotNull(doc.getObjectResolver(XPATH));
    }

    @Test
    public void testHasNoResolver() {
        assertNull(doc.getProperty("res:isNotReference1").getType().getObjectResolver());
        assertNull(doc.getProperty("res:isNotReference1").getObjectResolver());
        assertNull(doc.getObjectResolver("res:isNotReference1"));
    }

    @Test
    public void testAnyValueIsOkWithoutValidation() {
        prop().setValue("ernesto");
        doc = session.createDocument(doc);
        doc.setPropertyValue(XPATH, "bob");
        doc = session.saveDocument(doc);
    }

    @Test
    public void testPropertyValidationSucceed() {
        prop().setValue(PrimaryColor.BLUE.name());
        assertTrue(!validator.validate(prop()).hasError());
    }

    @Test
    public void testPropertyValidationFailed() {
        prop().setValue("bob");
        List<ConstraintViolation> violations = validator.validate(prop()).asList();
        assertEquals(1, violations.size());
        Constraint constraint = violations.get(0).getConstraint();
        assertTrue(constraint instanceof ObjectResolverConstraint);
        assertTrue(((ObjectResolverConstraint) constraint).getResolver() instanceof TestingColorResolver);
    }

    @Test(expected = DocumentValidationException.class)
    public void testPropertyValidationFailed1() {
        prop().setValue("coconut");
        doc.putContextData(CTX_MAP_KEY, Forcing.TURN_ON);
        session.createDocument(doc);
    }

    @Test(expected = DocumentValidationException.class)
    public void testPropertyValidationFailed2() {
        prop().setValue(SecondaryColor.ORANGE.name());
        doc.putContextData(CTX_MAP_KEY, Forcing.TURN_ON);
        session.createDocument(doc);
    }

    @Test
    public void testPropertyObjectResolverFetch() {
        prop().setValue("RED");
        session.createDocument(doc);
        PropertyObjectResolver objectResolver = prop().getObjectResolver();
        assertEquals(PrimaryColor.RED, objectResolver.fetch());
        assertEquals(PrimaryColor.RED, objectResolver.fetch(Color.class));
        assertEquals(PrimaryColor.RED, objectResolver.fetch(PrimaryColor.class));
    }

    @Test
    public void testDocumentObjectResolverFetch() {
        prop().setValue("RED");
        doc = session.createDocument(doc);
        PropertyObjectResolver objectResolver = doc.getObjectResolver(XPATH);
        assertEquals(PrimaryColor.RED, objectResolver.fetch());
        assertEquals(PrimaryColor.RED, objectResolver.fetch(Color.class));
        assertEquals(PrimaryColor.RED, objectResolver.fetch(PrimaryColor.class));
    }

    @Test
    public void testPropertyObjectResolverSetObject() {
        PropertyObjectResolver objectResolver = prop().getObjectResolver();
        objectResolver.setObject(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        objectResolver = prop().getObjectResolver();
        assertEquals(PrimaryColor.BLUE, objectResolver.fetch());
    }

    @Test
    public void testPropertyObjectResolverSetObjectSupportNull() {
        PropertyObjectResolver objectResolver = prop().getObjectResolver();
        objectResolver.setObject(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        objectResolver = prop().getObjectResolver();
        objectResolver.setObject(null);
        doc = session.saveDocument(doc);
        objectResolver = prop().getObjectResolver();
        assertNull(prop().getValue());
        assertNull(objectResolver.fetch());
    }

    @Test
    public void testPropertyObjectResolverSetObjectMakeNullStupidObjects() {
        PropertyObjectResolver objectResolver = prop().getObjectResolver();
        objectResolver.setObject(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        objectResolver = prop().getObjectResolver();
        objectResolver.setObject("romulus");
        doc = session.saveDocument(doc);
        objectResolver = prop().getObjectResolver();
        assertNull(prop().getValue());
        assertNull(objectResolver.fetch());
    }

    @Test
    public void testDocumentObjectResolverSetObject() {
        PropertyObjectResolver objectResolver = doc.getObjectResolver(XPATH);
        objectResolver.setObject(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        objectResolver = doc.getObjectResolver(XPATH);
        assertEquals(PrimaryColor.BLUE, objectResolver.fetch());
    }

    @Test
    public void testDocumentObjectResolverSetObjectSupportNull() {
        PropertyObjectResolver objectResolver = doc.getObjectResolver(XPATH);
        objectResolver.setObject(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        objectResolver = doc.getObjectResolver(XPATH);
        objectResolver.setObject(null);
        doc = session.saveDocument(doc);
        objectResolver = doc.getObjectResolver(XPATH);
        assertNull(prop().getValue());
        assertNull(objectResolver.fetch());
    }

    @Test
    public void testDocumentObjectResolverSetObjectMakeNullStupidObjects() {
        PropertyObjectResolver objectResolver = doc.getObjectResolver(XPATH);
        objectResolver.setObject(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        objectResolver = doc.getObjectResolver(XPATH);
        objectResolver.setObject("romulus");
        doc = session.saveDocument(doc);
        objectResolver = doc.getObjectResolver(XPATH);
        assertNull(prop().getValue());
        assertNull(objectResolver.fetch());
    }

    @Test
    public void testListOfReferences() {
        Property list = doc.getProperty("res:colorList");
        assertNull(list.getObjectResolver());
        list.addValue(PrimaryColor.RED.name());
        Property childProperty = list.get(0);
        PropertyObjectResolver objectResolver = childProperty.getObjectResolver();
        assertNotNull(objectResolver);
        assertEquals(objectResolver.fetch(), PrimaryColor.RED);
    }

    @Test
    public void testComplexReferenceList() {
        Property list = doc.getProperty("res:colorComplexList");
        assertNull(list.getObjectResolver());
        Map<String, String> element = new HashMap<String, String>();
        element.put("color1", PrimaryColor.RED.name());
        element.put("color2", SecondaryColor.ORANGE.name());
        list.addValue(element);
        Property childProperty = list.get(0);
        assertNull(childProperty.getObjectResolver());
        Property color1 = childProperty.get("color1");
        PropertyObjectResolver objectResolver1 = color1.getObjectResolver();
        assertNotNull(objectResolver1);
        assertEquals(objectResolver1.fetch(), PrimaryColor.RED);
        Property color2 = childProperty.get("color2");
        PropertyObjectResolver objectResolver2 = color2.getObjectResolver();
        assertNotNull(objectResolver2);
        assertEquals(objectResolver2.fetch(), SecondaryColor.ORANGE);
    }

    private Property prop() {
        return doc.getProperty(XPATH);
    }

}

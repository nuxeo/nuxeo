package org.nuxeo.ecm.core.schema.types.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.validation.DocumentValidationService.CTX_MAP_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.ConstraintViolation;
import org.nuxeo.ecm.core.schema.types.constraints.ExternalReferenceConstraint;
import org.nuxeo.ecm.core.schema.types.reference.TestingColorDummyReferenceResolver.Color;
import org.nuxeo.ecm.core.schema.types.reference.TestingColorDummyReferenceResolver.PrimaryColor;
import org.nuxeo.ecm.core.schema.types.reference.TestingColorDummyReferenceResolver.SecondaryColor;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-external-reference-service-contrib.xml" })
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestExternalReferenceProperties {

    private static final String XPATH = "ers:primaryColor";

    @Inject
    protected CoreSession session;

    @Inject
    protected ExternalReferenceService references;

    @Inject
    protected SchemaManager metamodel;

    @Inject
    protected DocumentValidationService validator;

    protected DocumentModel doc;

    @Before
    public void setUp() {
        doc = session.createDocumentModel("/", "doc1", "ExternalReferencer");
    }

    @Test
    public void testIsReference() {
        assertTrue(prop().isReference());
    }

    @Test
    public void testIsNotReference() {
        assertFalse(doc.getProperty("ers:isNotReference1").isReference());
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
        assertTrue(validator.validate(prop()).isEmpty());
    }

    @Test
    public void testPropertyValidationFailed() {
        prop().setValue("bob");
        List<ConstraintViolation> violations = validator.validate(prop());
        assertEquals(1, violations.size());
        Constraint constraint = violations.get(0).getConstraint();
        assertTrue(constraint instanceof ExternalReferenceConstraint);
        assertTrue(((ExternalReferenceConstraint) constraint).getResolver() instanceof TestingColorDummyReferenceResolver);
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
    public void testPropertyAccessorReference() {
        prop().setValue("RED");
        session.createDocument(doc);
        assertTrue(PrimaryColor.RED == prop().getReferencedEntity());
    }

    @Test
    public void testDocumentPropertyAccessorReference() {
        doc.setPropertyValue(XPATH, "RED");
        session.createDocument(doc);
        assertTrue(PrimaryColor.RED == prop().getReferencedEntity());
    }

    @Test
    public void testDocumentPropertyAccessorFetchingDefault() {
        doc.setPropertyValue(XPATH, "RED");
        session.createDocument(doc);
        assertEquals("RED", doc.getPropertyValue(XPATH));
    }

    @Test
    public void testPropertyMutatorReference1() {
        prop().setReferencedEntity(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        assertTrue(PrimaryColor.BLUE == prop().getReferencedEntity());
    }

    @Test
    public void testPropertyMutatorReference2() {
        prop().setValue(PrimaryColor.BLUE.name());
        doc = session.createDocument(doc);
        assertTrue(PrimaryColor.BLUE == prop().getReferencedEntity());
    }

    @Test
    public void testPropertyMutatorReference3() {
        doc.setPropertyValue(XPATH, PrimaryColor.BLUE.name());
        doc = session.createDocument(doc);
        assertTrue(PrimaryColor.BLUE == prop().getReferencedEntity());
    }

    @Test
    public void testPropertyMutatorReferenceSupportNull() {
        prop().setReferencedEntity(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        prop().setReferencedEntity(null);
        doc = session.saveDocument(doc);
        assertNull(prop().getValue());
        assertNull(prop().getReferencedEntity());
    }

    @Test
    public void testPropertyMutatorReferenceMakeNullStupidObjects() {
        prop().setReferencedEntity(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        prop().setReferencedEntity("romulus");
        doc = session.saveDocument(doc);
        assertNull(prop().getValue());
        assertNull(prop().getReferencedEntity());
    }

    @Test
    public void testPropertyMutatorReferenceMakeNullNonExistingValue() {
        prop().setReferencedEntity(PrimaryColor.BLUE);
        doc = session.createDocument(doc);
        prop().setReferencedEntity(SecondaryColor.ORANGE);
        doc = session.saveDocument(doc);
        assertNull(prop().getValue());
        assertNull(prop().getReferencedEntity());
    }

    @Test
    public void testPropertyCastAccessor1() {
        prop().setReferencedEntity(PrimaryColor.BLUE);
        Color color = prop().getValue(Color.class);
        assertNotNull(color);
        assertTrue(PrimaryColor.BLUE == color);
    }

    @Test
    public void testPropertyCastAccessor2() {
        prop().setReferencedEntity(PrimaryColor.BLUE);
        Color color = prop().getValue(PrimaryColor.class);
        assertNotNull(color);
        assertTrue(PrimaryColor.BLUE == color);
    }

    @Test
    public void testPropertyCastAccessorReturnNullWithNullValue() {
        prop().setValue(null);
        assertNull(prop().getValue(Color.class));
    }

    @Test(expected = PropertyConversionException.class)
    public void testPropertyCastAccessorReturnNullWithWrongType() {
        prop().setReferencedEntity(PrimaryColor.BLUE);
        Color color = prop().getValue(SecondaryColor.class);
        assertNull(color);
    }

    @Test(expected = PropertyConversionException.class)
    public void testPropertyCastAccessorWithInvalidValue() {
        prop().setValue("nimps");
        prop().getValue(Color.class);
    }

    @Test
    public void testListOfReferences() {
        Property list = doc.getProperty("ers:colorList");
        assertFalse(list.isReference());
        list.addValue(PrimaryColor.RED.name());
        Property childProperty = list.get(0);
        assertTrue(childProperty.isReference());
        assertTrue(childProperty.getReferencedEntity() == PrimaryColor.RED);
    }

    @Test
    public void testComplexReferenceList() {
        Property list = doc.getProperty("ers:colorComplexList");
        assertFalse(list.isReference());
        Map<String, String> element = new HashMap<String, String>();
        element.put("color1", PrimaryColor.RED.name());
        element.put("color2", SecondaryColor.ORANGE.name());
        list.addValue(element);
        Property childProperty = list.get(0);
        assertFalse(childProperty.isReference());
        Property color1 = childProperty.get("color1");
        assertTrue(color1.isReference());
        assertTrue(color1.getReferencedEntity() == PrimaryColor.RED);
        Property color2 = childProperty.get("color2");
        assertTrue(color2.isReference());
        assertTrue(color2.getReferencedEntity() == SecondaryColor.ORANGE);
    }

    private Property prop() {
        return doc.getProperty(XPATH);
    }

}

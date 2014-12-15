package org.nuxeo.ecm.core.schema.types.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.schema.types.reference.TestingColorDummyReferenceResolver.COLOR_MODE;
import static org.nuxeo.ecm.core.schema.types.reference.TestingColorDummyReferenceResolver.NAME;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint.Description;
import org.nuxeo.ecm.core.schema.types.constraints.ConstraintUtils;
import org.nuxeo.ecm.core.schema.types.constraints.ExternalReferenceConstraint;
import org.nuxeo.ecm.core.schema.types.reference.TestingColorDummyReferenceResolver.MODE;
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
public class TestExternalReferenceService {

    @Inject
    protected CoreSession session;

    @Inject
    protected ExternalReferenceService referenceService;

    @Inject
    protected SchemaManager metamodel;

    @Before
    public void setUp() {
    }

    @Test
    public void testServiceFetching() {
        assertNotNull(referenceService);
    }

    @Test
    public void testConfigurationOnRestrictionWorks() {
        Field field = metamodel.getField("ers:isReference1");
        checkResolver(field);
    }

    @Test
    public void testConfigurationOnRestrictionAndSimpleTypeWorks() {
        Field field = metamodel.getField("ers:isReference2");
        checkResolver(field);
    }

    @Test
    public void testConfigurationOnSimpleTypeWorks() {
        Field field = metamodel.getField("ers:isReference3");
        checkResolver(field);
    }

    @Test
    public void testSimpleTypeIsNotReference() {
        Field field = metamodel.getField("ers:isNotReference1");
        checkNoResolver(field);
    }

    @Test
    public void testSimpleTypeWithRestrictionIsNotReference() {
        Field field = metamodel.getField("ers:isNotReference2");
        checkNoResolver(field);
    }

    @Test
    public void testFieldSimpleTypeWithRestrictionAndParamButNoResolverIsNotReference() {
        Field field = metamodel.getField("ers:isNotReference3");
        checkNoResolver(field);
    }

    @Test
    public void testFieldFieldWithMissingParamIsNotReference() {
        Field field = metamodel.getField("ers:isReferenceButParamMissingFailed1");
        checkNoResolver(field);
    }

    @Test
    public void testFieldFieldWithWrongParamIsNotReference() {
        Field field = metamodel.getField("ers:isReferenceButWrongParamFailed1");
        checkNoResolver(field);
    }

    private void checkNoResolver(Field field) {
        assertFalse(field.getType().isReference());
        Set<Constraint> constraints = ((SimpleType) field.getType()).getConstraints();
        assertNull(ConstraintUtils.getConstraint(constraints, ExternalReferenceConstraint.class));
    }

    private void checkResolver(Field field) {
        assertTrue(field.getType().isReference());
        SimpleType simpleType = (SimpleType) field.getType();
        ExternalReferenceResolver resolver = simpleType.getResolver();
        assertNotNull(resolver);
        assertTrue(resolver instanceof TestingColorDummyReferenceResolver);
        Map<String, Serializable> parameters = resolver.getParameters();
        assertEquals(1, parameters.size());
        assertEquals(MODE.PRIMARY.name(), parameters.get(COLOR_MODE));
        Set<Constraint> constraints = simpleType.getConstraints();
        ExternalReferenceConstraint constraint = ConstraintUtils.getConstraint(constraints,
                ExternalReferenceConstraint.class);
        Description description = constraint.getDescription();
        assertEquals(NAME, description.getName());
        Map<String, Serializable> constraintParameters = description.getParameters();
        assertEquals(1, constraintParameters.size());
        assertEquals(MODE.PRIMARY.name(), parameters.get(COLOR_MODE));
    }

}

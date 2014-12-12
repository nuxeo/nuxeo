package org.nuxeo.ecm.core.schema.types.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    public void testResolverLoadingOnType() {
        Field field = metamodel.getField("ers:color");
        SimpleType type = (SimpleType) field.getType();
        checkResolver(type.getResolver());
    }

    @Test
    public void testResolverConstraintLoadingOnType() {
        Field field = metamodel.getField("ers:color");
        Set<Constraint> constraints = ((SimpleType) field.getType()).getConstraints();
        ExternalReferenceConstraint constraint = ConstraintUtils.getConstraint(constraints,
                ExternalReferenceConstraint.class);
        checkResolver(constraint.getResolver());
        Description description = constraint.getDescription();
        assertEquals(DummyPrimaryColorReferenceResolver.NAME, description.getName());
        Map<String, Serializable> parameters = description.getParameters();
        assertEquals(1, parameters.size());
        assertEquals("value1", parameters.get("param1"));
    }

    private void checkResolver(ExternalReferenceResolver<?> resolver) {
        assertNotNull(resolver);
        assertTrue(resolver instanceof DummyPrimaryColorReferenceResolver);
        Map<String, Serializable> parameters = resolver.getParameters();
        assertEquals(1, parameters.size());
        assertEquals("value1", parameters.get("param1"));
    }

}

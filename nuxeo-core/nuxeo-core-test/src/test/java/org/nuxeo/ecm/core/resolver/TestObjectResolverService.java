/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver.COLOR_MODE;
import static org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver.NAME;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

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
import org.nuxeo.ecm.core.schema.types.constraints.ObjectResolverConstraint;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolverService;
import org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver;
import org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver.MODE;
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
public class TestObjectResolverService {

    @Inject
    protected CoreSession session;

    @Inject
    protected ObjectResolverService referenceService;

    @Inject
    protected SchemaManager schemaManager;

    @Before
    public void setUp() {
    }

    @Test
    public void testServiceFetching() {
        assertNotNull(referenceService);
    }

    @Test
    public void testConfigurationOnRestrictionWorks() {
        Field field = schemaManager.getField("res:isReference1");
        checkResolver(field);
    }

    @Test
    public void testConfigurationOnRestrictionAndSimpleTypeWorks() {
        Field field = schemaManager.getField("res:isReference2");
        checkResolver(field);
    }

    @Test
    public void testConfigurationOnSimpleTypeWorks() {
        Field field = schemaManager.getField("res:isReference3");
        checkResolver(field);
    }

    @Test
    public void testSimpleTypeIsNotReference() {
        Field field = schemaManager.getField("res:isNotReference1");
        checkNoResolver(field);
    }

    @Test
    public void testSimpleTypeWithRestrictionIsNotReference() {
        Field field = schemaManager.getField("res:isNotReference2");
        checkNoResolver(field);
    }

    @Test
    public void testFieldSimpleTypeWithRestrictionAndParamButNoResolverIsNotReference() {
        Field field = schemaManager.getField("res:isNotReference3");
        checkNoResolver(field);
    }

    @Test
    public void testFieldFieldWithMissingParamIsNotReference() {
        Field field = schemaManager.getField("res:isReferenceButParamMissingFailed1");
        checkNoResolver(field);
    }

    @Test
    public void testFieldFieldWithWrongParamIsNotReference() {
        Field field = schemaManager.getField("res:isReferenceButWrongParamFailed1");
        checkNoResolver(field);
    }

    private void checkNoResolver(Field field) {
        assertNull(field.getType().getObjectResolver());
        Set<Constraint> constraints = field.getType().getConstraints();
        assertNull(ConstraintUtils.getConstraint(constraints, ObjectResolverConstraint.class));
    }

    private void checkResolver(Field field) {
        assertNotNull(field.getType().getObjectResolver());
        SimpleType simpleType = (SimpleType) field.getType();
        ObjectResolver resolver = simpleType.getObjectResolver();
        assertNotNull(resolver);
        assertTrue(resolver instanceof TestingColorResolver);
        Map<String, Serializable> parameters = resolver.getParameters();
        assertEquals(1, parameters.size());
        assertEquals(MODE.PRIMARY.name(), parameters.get(COLOR_MODE));
        Set<Constraint> constraints = simpleType.getConstraints();
        ObjectResolverConstraint constraint = ConstraintUtils.getConstraint(constraints,
                ObjectResolverConstraint.class);
        Description description = constraint.getDescription();
        assertEquals(NAME, description.getName());
        Map<String, Serializable> constraintParameters = description.getParameters();
        assertEquals(1, constraintParameters.size());
        assertEquals(MODE.PRIMARY.name(), parameters.get(COLOR_MODE));
    }

}

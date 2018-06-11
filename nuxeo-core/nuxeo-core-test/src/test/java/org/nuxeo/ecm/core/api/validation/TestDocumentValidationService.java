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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.validation;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.validation.ConstraintViolation.PathNode;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.NumericIntervalConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.PatternConstraint;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-validation-service-contrib.xml" })
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDocumentValidationService {

    // it comes from the message bundle messages_en.properties
    private static final String MESSAGE_FOR_USERS_FIRSTNAME = "message_for_users_firstname";

    private static final String SIMPLE_FIELD = "vs:groupCode";

    private static final String COMPLEX_FIELD = "vs:manager";

    private static final String LIST_FIELD = "vs:roles";

    private static final String COMPLEX_LIST_FIELD = "vs:users";

    public static final String COMPLEX_DUMMY_RESOLVER_1 = "vs:dummyComplex1";

    public static final String COMPLEX_DUMMY_RESOLVER_2 = "vs:dummyComplex2";

    private static final String SCHEMA = "validationSample";

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentValidationService validator;

    @Inject
    protected SchemaManager metamodel;

    private DocumentModel doc;

    @Before
    public void setUp() {
        doc = session.createDocumentModel("/", "doc1", "ValidatedUserGroup");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
    }

    @Test
    public void testServiceFetching() {
        assertNotNull(validator);
    }

    @Test
    public void testDocumentWithoutViolation() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        checkOk(validator.validate(doc));
    }

    @Test
    public void testDocumentWithViolation1() {
        checkNotNullOnGroupCode(validator.validate(doc));
    }

    @Test
    public void testDocumentWithViolation2() {
        doc.setPropertyValue(SIMPLE_FIELD, -12345);
        checkNumericIntervalOnGroupCode(validator.validate(doc));
    }

    @Test
    public void testDocumentDirtyWithViolation() {
        doc.setPropertyValue(SIMPLE_FIELD, 10);
        doc = session.saveDocument(doc);
        doc.setPropertyValue(SIMPLE_FIELD, null);
        checkNotNullOnGroupCode(validator.validate(doc, true));
    }

    @Test
    public void testDocumentNotDirtyWithViolation() {
        doc.setPropertyValue(SIMPLE_FIELD, null);
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkOk(validator.validate(doc, true));
    }

    @Test
    public void testFieldWithoutViolation() {
        Field field = metamodel.getField(SIMPLE_FIELD);
        checkOk(validator.validate(field, 12345));
    }

    @Test
    public void testFieldWithViolation1() {
        Field field = metamodel.getField(SIMPLE_FIELD);
        checkNotNullOnGroupCode(validator.validate(field, null));
    }

    @Test
    public void testFieldWithViolation2() {
        Field field = metamodel.getField(SIMPLE_FIELD);
        checkNumericIntervalOnGroupCode(validator.validate(field, -12345));
    }

    @Test
    public void testFieldComplexWithoutViolation() {
        Field field = metamodel.getField(COMPLEX_FIELD);
        checkOk(validator.validate(field, createUser("Bob", "Sponge")));
    }

    @Test
    public void testFieldComplexWithViolation1() {
        Field field = metamodel.getField(COMPLEX_FIELD);
        checkNotNullOnManagerFirstname(validator.validate(field, createUser(null, "Sponge")));
    }

    @Test
    public void testFieldComplexWithViolation2() {
        Field field = metamodel.getField(COMPLEX_FIELD);
        checkPatternOnManagerFirstname(validator.validate(field, createUser("   ", "Sponge")));
    }

    @Test
    public void testFieldListWithoutViolation() {
        Field field = metamodel.getField(LIST_FIELD);
        ArrayList<String> roles = createRoles("role1", "role2", "role3");
        checkOk(validator.validate(field, roles));
    }

    @Test
    public void testFieldListWithViolation1() {
        Field field = metamodel.getField(LIST_FIELD);
        ArrayList<String> roles = createRoles("role1", null, "role3");
        checkNotNullOnRoles(validator.validate(field, roles));
    }

    @Test
    public void testFieldListWithViolation2() {
        Field field = metamodel.getField(LIST_FIELD);
        ArrayList<String> roles = createRoles("role1", "role2", "invalid role3");
        checkPatternOnRoles(validator.validate(field, roles));
    }

    @Test
    public void testFieldComplexListWithoutViolation() {
        Field field = metamodel.getField(COMPLEX_LIST_FIELD);
        ArrayList<Map<String, String>> value = new ArrayList<Map<String, String>>();
        value.add(createUser("Bob", "Sponge"));
        value.add(createUser("Patrick", "Star"));
        value.add(createUser("Sandy", "Cheeks"));
        checkOk(validator.validate(field, value));
    }

    @Test
    public void testFieldComplexListWithViolation1() {
        Field field = metamodel.getField(COMPLEX_LIST_FIELD);
        ArrayList<Map<String, String>> value = new ArrayList<Map<String, String>>();
        value.add(createUser("Bob", "Sponge"));
        value.add(createUser("Patrick", "Star"));
        value.add(createUser(null, "Cheeks"));
        checkNotNullOnUsersFirstname(validator.validate(field, value));
    }

    @Test
    public void testFieldComplexListWithViolation2() {
        Field field = metamodel.getField(COMPLEX_LIST_FIELD);
        ArrayList<Map<String, String>> value = new ArrayList<Map<String, String>>();
        value.add(createUser("Bob", "Sponge"));
        value.add(createUser("Patrick", "Star"));
        value.add(createUser("   ", "Cheeks"));
        checkPatternOnUsersFirstname(validator.validate(field, value));
    }

    @Test
    public void testFieldXPathWithoutViolation() {
        checkOk(validator.validate(SIMPLE_FIELD, 12345));
    }

    @Test
    public void testFieldXPathWithViolation1() {
        checkNotNullOnGroupCode(validator.validate(SIMPLE_FIELD, null));
    }

    @Test
    public void testFieldXPathWithViolation2() {
        checkNumericIntervalOnGroupCode(validator.validate(SIMPLE_FIELD, -12345));
    }

    @Test
    public void testComplexFieldWithoutViolation() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        doc.setPropertyValue(COMPLEX_FIELD, createUser("Bob", "Sponge"));
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkOk(validator.validate(doc));
    }

    @Test
    public void testComplexFieldWithSubFieldViolation1() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        doc.setPropertyValue(COMPLEX_FIELD, createUser(null, "Sponge"));
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkNotNullOnManagerFirstname(validator.validate(doc));
    }

    @Test
    public void testComplexFieldWithSubFieldViolation2() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        doc.setPropertyValue(COMPLEX_FIELD, createUser("   ", "Sponge"));
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkPatternOnManagerFirstname(validator.validate(doc));
    }

    @Test
    public void testListFieldWithoutViolation() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        ArrayList<String> value = createRoles("role1", "role2", "role3");
        doc.setPropertyValue(LIST_FIELD, value);
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkOk(validator.validate(doc));
    }

    @Test
    public void testListFieldWithSubFieldNullViolation1() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        ArrayList<String> value = createRoles("role1", null, "role3");
        doc.setPropertyValue(LIST_FIELD, value);
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkNotNullOnRoles(validator.validate(doc));
    }

    @Test
    public void testListFieldWithSubFieldPatternViolation2() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        ArrayList<String> value = createRoles("role1", "role2", "invalid role3");
        doc.setPropertyValue(LIST_FIELD, value);
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkPatternOnRoles(validator.validate(doc));
    }

    @Test
    public void testComplexListFieldWithoutViolation() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        ArrayList<Map<String, String>> value = new ArrayList<Map<String, String>>();
        value.add(createUser("Bob", "Sponge"));
        value.add(createUser("Patrick", "Star"));
        value.add(createUser("Sandy", "Cheeks"));
        doc.setPropertyValue(COMPLEX_LIST_FIELD, value);
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkOk(validator.validate(doc));
    }

    @Test
    public void testComplexListFieldWithItemSubFieldNullViolation1() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        ArrayList<Map<String, String>> value = new ArrayList<Map<String, String>>();
        value.add(createUser("Bob", "Sponge"));
        value.add(createUser("Patrick", "Star"));
        value.add(createUser(null, "Sandy"));
        doc.setPropertyValue(COMPLEX_LIST_FIELD, value);
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkNotNullOnUsersFirstname(validator.validate(doc));
    }

    @Test
    public void testComplexListFieldWithSubFieldPatternViolation2() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        ArrayList<Map<String, String>> value = new ArrayList<Map<String, String>>();
        value.add(createUser("Bob", "Sponge"));
        value.add(createUser("Patrick", "Star"));
        value.add(createUser("   ", "Cheeks"));
        doc.setPropertyValue(COMPLEX_LIST_FIELD, value);
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        checkPatternOnUsersFirstname(validator.validate(doc));
    }

    @Test
    public void testComplexListFieldWithMultipleSubFieldViolations() {
        doc.setPropertyValue(SIMPLE_FIELD, 12345);
        ArrayList<Map<String, String>> value = new ArrayList<Map<String, String>>();
        value.add(createUser("   ", "Sponge"));
        value.add(createUser("Patrick", "Star"));
        value.add(createUser("   ", "Cheeks"));
        value.add(createUser("Bob", null));
        value.add(createUser("   ", null));
        doc.setPropertyValue(COMPLEX_LIST_FIELD, value);
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        List<ConstraintViolation> violations = validator.validate(doc).asList();
        assertEquals(5, violations.size());
        boolean found1 = false, found2 = false, found3 = false, found4 = false, found5 = false;
        for (ConstraintViolation violation : violations) {
            PathNode pathNode = violation.getPath().get(1);
            switch (pathNode.getIndex()) {
            case 0:
                checkPatternOnUsersFirstname(violation, 0);
                found1 = true;
                break;
            case 2:
                checkPatternOnUsersFirstname(violation, 2);
                found2 = true;
                break;
            case 3:
                checkNotNullOnUsersLastname(violation, 3);
                found3 = true;
                break;
            case 4:
                if (violation.getConstraint() instanceof NotNullConstraint) {
                    checkNotNullOnUsersLastname(violation, 4);
                    found5 = true;
                } else if (violation.getConstraint() instanceof PatternConstraint) {
                    checkPatternOnUsersFirstname(violation, 4);
                    found4 = true;
                }
                break;
            }
        }
        assertTrue(found1);
        assertTrue(found2);
        assertTrue(found3);
        assertTrue(found4);
        assertTrue(found5);
    }

    @Test
    public void testValidateDeepFieldUsingXpath() {
        DocumentValidationReport violations;
        violations = validator.validate("vs:users/0/firstname", "Bob");
        assertFalse(violations.hasError());
        violations = validator.validate("vs:users/firstname", "Bob");
        assertFalse(violations.hasError());
        violations = validator.validate("vs:users/user/firstname", "Bob");
        assertFalse(violations.hasError());
        violations = validator.validate("vs:users/0/firstname", null);
        assertEquals(1, violations.numberOfErrors());
        assertTrue(violations.asList().get(0).getConstraint() instanceof NotNullConstraint);
        violations = validator.validate("vs:users/0/firstname", "   ");
        assertEquals(1, violations.numberOfErrors());
        assertTrue(violations.asList().get(0).getConstraint() instanceof PatternConstraint);
    }

    @Test
    public void testValidateSimpleListField() {
        DocumentValidationReport violations;
        violations = validator.validate("vs:simpleList", new String[] {});
        assertFalse(violations.hasError());
        violations = validator.validate("vs:simpleList", new String[] { "", "123" });
        assertFalse(violations.hasError());
        violations = validator.validate("vs:simpleList", new String[] { "", "123", "ABC" });
        assertTrue(violations.hasError());
        assertEquals(1, violations.numberOfErrors());
        assertTrue(violations.asList().get(0).getConstraint() instanceof PatternConstraint);
    }

    @Test
    public void testValidateArrayPropsOnlyDirtyItem1() {
        doc.setPropertyValue("vs:simpleList", new Object[] { "123", "234", "345" });
        DocumentValidationReport violations = validator.validate(doc, true);
        assertFalse(violations.hasError());

    }

    @Test
    public void testValidateArrayPropsOnlyDirtyItem2() {
        doc.setPropertyValue("vs:simpleList", new Object[] { "123", "234", "345" });
        session.saveDocument(doc);
        doc.setPropertyValue("vs:simpleList", new Object[] { "abc", "234", "345" });
        DocumentValidationReport violations = validator.validate(doc, true);
        assertTrue(violations.hasError());
        assertEquals(1, violations.numberOfErrors());
        assertTrue(violations.asList().get(0).getConstraint() instanceof PatternConstraint);
    }

    @Test
    public void testValidateArrayPropsOnlyDirtyItem3() {
        doc.setPropertyValue("vs:simpleList", new Object[] { "123", "234", "345" });
        session.saveDocument(doc);
        doc.setPropertyValue("vs:simpleList", new Object[] { "123", "abc", "345" });
        DocumentValidationReport violations = validator.validate(doc, true);
        assertTrue(violations.hasError());
        assertEquals(1, violations.numberOfErrors());
        assertTrue(violations.asList().get(0).getConstraint() instanceof PatternConstraint);
    }

    @Test
    public void testValidateArrayPropertyWithConstraint() {
        DocumentValidationReport violations;
        doc.setPropertyValue("vs:groupCode", 123);
        doc.setPropertyValue("vs:simpleList", new String[] {});
        violations = validator.validate(doc);
        assertFalse(violations.hasError());
        doc.setPropertyValue("vs:simpleList", new String[] { "", "123" });
        violations = validator.validate(doc);
        assertFalse(violations.hasError());
        doc.setPropertyValue("vs:simpleList", new String[] { "", "123", "ABC" });
        violations = validator.validate(doc);
        assertTrue(violations.hasError());
        assertEquals(1, violations.numberOfErrors());
        assertTrue(violations.asList().get(0).getConstraint() instanceof PatternConstraint);
    }

    @Test
    public void testValidateDocumentPropertyViolationMessage() {
        DocumentValidationReport violations;
        doc.setPropertyValue("vs:groupCode", 123);
        HashMap<String, String> user = new HashMap<String, String>();
        user.put("lastname", "The kid");
        doc.getProperty("vs:users").addValue(0, user);
        violations = validator.validate(doc);
        assertTrue(violations.hasError());
        List<ConstraintViolation> violationList = violations.asList();
        assertEquals(1, violationList.size());
        ConstraintViolation violation = violationList.get(0);
        assertEquals(MESSAGE_FOR_USERS_FIRSTNAME, violation.getMessage(Locale.ENGLISH));
    }

    @Test
    public void testValidatePropertyViolationMessage() {
        DocumentValidationReport violations;
        doc.setPropertyValue("vs:groupCode", 123);
        HashMap<String, String> user = new HashMap<String, String>();
        user.put("lastname", "The kid");
        doc.getProperty("vs:users").addValue(0, user);
        Property userFirstnameProperty = doc.getProperty("vs:users").get(0).get("firstname");
        violations = validator.validate(userFirstnameProperty);
        assertTrue(violations.hasError());
        List<ConstraintViolation> violationList = violations.asList();
        assertEquals(1, violationList.size());
        ConstraintViolation violation = violationList.get(0);
        assertEquals(MESSAGE_FOR_USERS_FIRSTNAME, violation.getMessage(Locale.ENGLISH));
    }

    @Test
    public void testValidateXPathViolationMessage() {
        DocumentValidationReport violations;
        violations = validator.validate("vs:users/0/firstname", null);
        assertTrue(violations.hasError());
        List<ConstraintViolation> violationList = violations.asList();
        assertEquals(1, violationList.size());
        ConstraintViolation violation = violationList.get(0);
        assertEquals(MESSAGE_FOR_USERS_FIRSTNAME, violation.getMessage(Locale.ENGLISH));
        violations = validator.validate("vs:users/firstname", null);
        assertTrue(violations.hasError());
        violationList = violations.asList();
        assertEquals(1, violationList.size());
        violation = violationList.get(0);
        assertEquals(MESSAGE_FOR_USERS_FIRSTNAME, violation.getMessage(Locale.ENGLISH));
        violations = validator.validate("vs:users/user/firstname", null);
        assertTrue(violations.hasError());
        violationList = violations.asList();
        assertEquals(1, violationList.size());
        violation = violationList.get(0);
        assertEquals(MESSAGE_FOR_USERS_FIRSTNAME, violation.getMessage(Locale.ENGLISH));
    }

    // NXP-24660
    @Test
    public void testValidationOnSchemaWithTwoComplexHavingSameChild() {
        Property complexDummy1 = doc.getProperty(COMPLEX_DUMMY_RESOLVER_1);
        complexDummy1.setValue(singletonMap("value", "value1"));
        checkOk(validator.validate(complexDummy1));
        Property complexDummy2 = doc.getProperty(COMPLEX_DUMMY_RESOLVER_2);
        complexDummy2.setValue(singletonMap("value", "value2"));
        DocumentValidationReport report = validator.validate(complexDummy2);
        assertEquals(1, report.numberOfErrors());
    }

    // //////////////////////////////////////
    // End of the tests : Usefull methods //

    private ArrayList<String> createRoles(String... roles) {
        return new ArrayList<String>(Arrays.asList(roles));
    }

    private HashMap<String, String> createUser(String firstname, String lastname) {
        HashMap<String, String> user = new HashMap<String, String>();
        user.put("firstname", firstname);
        user.put("lastname", lastname);
        return user;
    }

    private void checkOk(DocumentValidationReport report) {
        List<ConstraintViolation> violations = report.asList();
        assertEquals(0, violations.size());
    }

    private void checkNotNullOnGroupCode(DocumentValidationReport report) {
        List<ConstraintViolation> violations = report.asList();
        assertEquals(1, violations.size());
        ConstraintViolation violation = violations.get(0);
        assertEquals(NotNullConstraint.get(), violation.getConstraint());
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(1, violation.getPath().size());
        String fieldName = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals(SIMPLE_FIELD, fieldName);
        assertNull(violation.getInvalidValue());
    }

    private void checkNumericIntervalOnGroupCode(DocumentValidationReport report) {
        List<ConstraintViolation> violations = report.asList();
        assertEquals(1, violations.size());
        ConstraintViolation violation = violations.get(0);
        Constraint constraint = violation.getConstraint();
        assertTrue(constraint instanceof NumericIntervalConstraint);
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(1, violation.getPath().size());
        String fieldName = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals(SIMPLE_FIELD, fieldName);
        assertEquals(-12345, ((Number) violation.getInvalidValue()).intValue());
    }

    private void checkNotNullOnManagerFirstname(DocumentValidationReport report) {
        List<ConstraintViolation> violations = report.asList();
        assertEquals(1, violations.size());
        ConstraintViolation violation = violations.get(0);
        assertEquals(NotNullConstraint.get(), violation.getConstraint());
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(2, violation.getPath().size());
        String fieldName1 = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals("vs:manager", fieldName1);
        String fieldName2 = violation.getPath().get(1).getField().getName().getPrefixedName();
        assertEquals("firstname", fieldName2);
        assertNull(violation.getInvalidValue());
    }

    private void checkPatternOnManagerFirstname(DocumentValidationReport report) {
        List<ConstraintViolation> violations = report.asList();
        assertEquals(1, violations.size());
        ConstraintViolation violation = violations.get(0);
        assertTrue(violation.getConstraint() instanceof PatternConstraint);
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(2, violation.getPath().size());
        String fieldName1 = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals("vs:manager", fieldName1);
        String fieldName2 = violation.getPath().get(1).getField().getName().getPrefixedName();
        assertEquals("firstname", fieldName2);
        assertEquals("   ", violation.getInvalidValue());
    }

    private void checkNotNullOnRoles(DocumentValidationReport report) {
        List<ConstraintViolation> violations = report.asList();
        assertEquals(1, violations.size());
        ConstraintViolation violation = violations.get(0);
        assertEquals(NotNullConstraint.get(), violation.getConstraint());
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(2, violation.getPath().size());
        String fieldName1 = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals("vs:roles", fieldName1);
        String fieldName2 = violation.getPath().get(1).getField().getName().getPrefixedName();
        assertEquals("role", fieldName2);
        assertNull(violation.getInvalidValue());
    }

    private void checkPatternOnRoles(DocumentValidationReport report) {
        List<ConstraintViolation> violations = report.asList();
        assertEquals(1, violations.size());
        ConstraintViolation violation = violations.get(0);
        assertTrue(violation.getConstraint() instanceof PatternConstraint);
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(2, violation.getPath().size());
        String fieldName1 = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals("vs:roles", fieldName1);
        String fieldName2 = violation.getPath().get(1).getField().getName().getPrefixedName();
        assertEquals("role", fieldName2);
        assertEquals("invalid role3", violation.getInvalidValue());
    }

    private void checkNotNullOnUsersFirstname(DocumentValidationReport report) {
        List<ConstraintViolation> violations = report.asList();
        assertEquals(1, violations.size());
        ConstraintViolation violation = violations.get(0);
        assertEquals(NotNullConstraint.get(), violation.getConstraint());
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(3, violation.getPath().size());
        String fieldName1 = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals("vs:users", fieldName1);
        String fieldName2 = violation.getPath().get(1).getField().getName().getPrefixedName();
        assertEquals("user", fieldName2);
        String fieldName3 = violation.getPath().get(2).getField().getName().getPrefixedName();
        assertEquals("firstname", fieldName3);
        assertNull(violation.getInvalidValue());
    }

    private void checkPatternOnUsersFirstname(DocumentValidationReport report) {
        List<ConstraintViolation> violations = report.asList();
        assertEquals(1, violations.size());
        ConstraintViolation violation = violations.get(0);
        assertTrue(violation.getConstraint() instanceof PatternConstraint);
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(3, violation.getPath().size());
        String fieldName1 = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals("vs:users", fieldName1);
        String fieldName2 = violation.getPath().get(1).getField().getName().getPrefixedName();
        assertEquals("user", fieldName2);
        String fieldName3 = violation.getPath().get(2).getField().getName().getPrefixedName();
        assertEquals("firstname", fieldName3);
        assertEquals("   ", violation.getInvalidValue());
    }

    private void checkNotNullOnUsersLastname(ConstraintViolation violation, int expectedIndex) {
        assertEquals(NotNullConstraint.get(), violation.getConstraint());
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(3, violation.getPath().size());
        String fieldName1 = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals("vs:users", fieldName1);
        String fieldName2 = violation.getPath().get(1).getField().getName().getPrefixedName();
        assertEquals("user", fieldName2);
        PathNode pathNode = violation.getPath().get(1);
        assertEquals(expectedIndex, pathNode.getIndex());
        String fieldName3 = violation.getPath().get(2).getField().getName().getPrefixedName();
        assertEquals("lastname", fieldName3);
        assertNull(violation.getInvalidValue());
    }

    private void checkPatternOnUsersFirstname(ConstraintViolation violation, int expectedIndex) {
        assertTrue(violation.getConstraint() instanceof PatternConstraint);
        assertEquals(SCHEMA, violation.getSchema().getName());
        assertEquals(3, violation.getPath().size());
        String fieldName1 = violation.getPath().get(0).getField().getName().getPrefixedName();
        assertEquals("vs:users", fieldName1);
        String fieldName2 = violation.getPath().get(1).getField().getName().getPrefixedName();
        assertEquals("user", fieldName2);
        PathNode pathNode = violation.getPath().get(1);
        assertEquals(expectedIndex, pathNode.getIndex());
        String fieldName3 = violation.getPath().get(2).getField().getName().getPrefixedName();
        assertEquals("firstname", fieldName3);
        assertEquals("   ", violation.getInvalidValue());
    }

}

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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.usermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.runtime.test.runner.Deploy;

@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanager-resolver.xml")
public class TestUserManagerResolver extends UserManagerTestCase {

    private static final String USER_XPATH = "umr:user";

    private static final String GROUP_XPATH = "umr:group";

    private static final String USER_GROUP_XPATH = "umr:userOrGroup";

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentValidationService validator;

    protected DocumentModel doc;

    @Before
    public void setUp() throws Exception {
        doc = session.createDocumentModel("/", "doc1", "TestResolver");
    }

    @Test
    public void defaultSupportedClasses() throws Exception {
        List<Class<?>> classes = new UserManagerResolver().getManagedClasses();
        assertEquals(2, classes.size());
        assertTrue(classes.contains(NuxeoPrincipal.class));
        assertTrue(classes.contains(NuxeoGroup.class));
    }

    @Test
    public void userAndGroupSupportedClasses() throws Exception {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        umr.configure(parameters);
        List<Class<?>> classes = umr.getManagedClasses();
        assertEquals(2, classes.size());
        assertTrue(classes.contains(NuxeoPrincipal.class));
        assertTrue(classes.contains(NuxeoGroup.class));
    }

    @Test
    public void userSupportedClasses() throws Exception {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        List<Class<?>> classes = umr.getManagedClasses();
        assertEquals(1, classes.size());
        assertTrue(classes.contains(NuxeoPrincipal.class));
    }

    @Test
    public void groupSupportedClasses() throws Exception {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        List<Class<?>> classes = umr.getManagedClasses();
        assertEquals(1, classes.size());
        assertTrue(classes.contains(NuxeoGroup.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetch() {
        new UserManagerResolver().fetch("usr:Administrator");
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetchCast() {
        new UserManagerResolver().fetch(NuxeoPrincipal.class, "usr:Administrator");
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetReference() {
        new UserManagerResolver().getReference(userManager.getPrincipal("Administrator"));
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetParameters() {
        new UserManagerResolver().getParameters();
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetConstraintErrorMessage() {
        new UserManagerResolver().getConstraintErrorMessage(null, Locale.ENGLISH);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleConfigurationTwice() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        umr.configure(parameters);
        umr.configure(parameters);
    }

    @Test
    public void testConfigurationDefaultUserOrGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertTrue(umr.isIncludingGroups());
        assertTrue(umr.isIncludingUsers());
        Map<String, Serializable> outputParameters = umr.getParameters();
        assertEquals(true, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_GROUPS));
        assertEquals(true, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_USERS));
    }

    @Test
    public void testConfigurationUser() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        assertFalse(umr.isIncludingGroups());
        assertTrue(umr.isIncludingUsers());
        Map<String, Serializable> outputParameters = umr.getParameters();
        assertEquals(false, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_GROUPS));
        assertEquals(true, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_USERS));
    }

    @Test
    public void testConfigurationGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        assertTrue(umr.isIncludingGroups());
        assertFalse(umr.isIncludingUsers());
        Map<String, Serializable> outputParameters = umr.getParameters();
        assertEquals(true, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_GROUPS));
        assertEquals(false, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_USERS));
    }

    @Test
    public void testName() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertEquals(UserManagerResolver.NAME, umr.getName());
    }

    @Test
    public void testValidateGoodUserWithDefaultConf() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertTrue(umr.validate("user:Administrator"));
    }

    @Test
    public void testValidateGoodUserWithFilterUser() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        assertTrue(umr.validate("user:Administrator"));
    }

    @Test
    public void testValidateUserFailedWithBadValue() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertFalse(umr.validate("toto"));
    }

    @Test
    public void testValidateUserSucceedWithoutPrefix() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertTrue(umr.validate("Administrator"));
    }

    @Test
    public void testValidateUserFailedWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        assertFalse(umr.validate("user:Administrator"));
    }

    @Test
    public void testValidateGoodGroupWithDefaultConf() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertTrue(umr.validate("group:members"));
    }

    @Test
    public void testValidateGoodGroupWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        assertTrue(umr.validate("group:members"));
    }

    @Test
    public void testValidateGroupFailedWithBadValue() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertFalse(umr.validate("toto"));
    }

    @Test
    public void testValidateGroupSucceedWithoutPrefix() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertTrue(umr.validate("members"));
    }

    @Test
    public void testValidateGroupFailedWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        assertFalse(umr.validate("group:members"));
    }

    @Test
    public void testFetchGoodUserWithDefaultConf() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        Object entity = umr.fetch("user:Administrator");
        assertTrue(entity instanceof NuxeoPrincipal);
        assertEquals("Administrator", ((NuxeoPrincipal) entity).getName());
    }

    @Test
    public void testFetchGoodUserWithFilterUser() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        Object entity = umr.fetch("user:Administrator");
        assertTrue(entity instanceof NuxeoPrincipal);
        assertEquals("Administrator", ((NuxeoPrincipal) entity).getName());
    }

    @Test
    public void testFetchUserFailedWithBadValue() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertNull(umr.fetch("user:toto"));
    }

    @Test
    public void testFetchUserSucceedWithoutPrefix() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertNotNull(umr.fetch("Administrator"));
    }

    @Test
    public void testFetchUserFailedWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        assertNull(umr.fetch("user:Administrator"));
    }

    @Test
    public void testFetchGoodGroupWithDefaultConf() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        Object entity = umr.fetch("group:members");
        assertTrue(entity instanceof NuxeoGroup);
        assertEquals("members", ((NuxeoGroup) entity).getName());
    }

    @Test
    public void testFetchGoodGroupWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        Object entity = umr.fetch("group:members");
        assertTrue(entity instanceof NuxeoGroup);
        assertEquals("members", ((NuxeoGroup) entity).getName());
    }

    @Test
    public void testFetchGroupFailedWithBadValue() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertNull(umr.fetch("group:toto"));
    }

    @Test
    public void testFetchGroupSucceedWithoutPrefix() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertNotNull(umr.fetch("members"));
    }

    @Test
    public void testFetchGroupFailedWithFilterUser() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        assertNull(umr.fetch("group:members"));
    }

    @Test
    public void testFetchCastNuxeoPrincipal() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        NuxeoPrincipal principal = umr.fetch(NuxeoPrincipal.class, "user:Administrator");
        assertNotNull(principal);
        assertEquals("Administrator", principal.getName());
        assertNotNull(umr.fetch(NuxeoPrincipal.class, "Administrator"));
        assertNull(umr.fetch(NuxeoPrincipal.class, "user:toto"));
        assertNull(umr.fetch(NuxeoPrincipal.class, "group:members"));
    }

    @Test
    public void testFetchCastNuxeoGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        NuxeoGroup group = umr.fetch(NuxeoGroup.class, "group:members");
        assertNotNull(group);
        assertEquals("members", group.getName());
        assertNotNull(umr.fetch(NuxeoGroup.class, "members"));
        assertNull(umr.fetch(NuxeoGroup.class, "group:toto"));
        assertNull(umr.fetch(NuxeoGroup.class, "user:Administrator"));
    }

    @Test
    public void testFetchCastDoesntSupportDocumentModel() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertNull(umr.fetch(DocumentModel.class, "user:Administrator"));
    }

    @Test
    public void testFetchCastDoesntSupportStupidTypes() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertNull(umr.fetch(List.class, "user:Administrator"));
    }

    @Test
    public void testGetReferenceUser() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        assertEquals("user:Administrator", umr.getReference(principal));
    }

    @Test
    public void testGetReferenceGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        NuxeoGroup group = userManager.getGroup("members");
        assertEquals("group:members", umr.getReference(group));
    }

    @Test
    public void testGetReferenceWithNonExistingUserWorks() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("chaps");
        assertEquals("user:chaps", umr.getReference(principal));
    }

    @Test
    public void testGetReferenceWithNonExistingGroupWorks() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        NuxeoGroup group = new NuxeoGroupImpl("chaps");
        assertEquals("group:chaps", umr.getReference(group));
    }

    @Test
    public void testGetReferenceInvalid() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<>());
        assertNull(umr.getReference("nothing"));
    }

    @Test
    public void testConfigurationIsLoaded() {
        UserManagerResolver userResolver = (UserManagerResolver) doc.getProperty(USER_XPATH)
                                                                    .getType()
                                                                    .getObjectResolver();
        assertTrue(userResolver.isIncludingUsers());
        assertFalse(userResolver.isIncludingGroups());
        UserManagerResolver groupResolver = (UserManagerResolver) doc.getProperty(GROUP_XPATH)
                                                                     .getType()
                                                                     .getObjectResolver();
        assertFalse(groupResolver.isIncludingUsers());
        assertTrue(groupResolver.isIncludingGroups());
        UserManagerResolver anyResolver = (UserManagerResolver) doc.getProperty(USER_GROUP_XPATH)
                                                                   .getType()
                                                                   .getObjectResolver();
        assertTrue(anyResolver.isIncludingUsers());
        assertTrue(anyResolver.isIncludingGroups());
    }

    @Test
    public void testTypeHasResolver() {
        ObjectResolver resolver;
        resolver = doc.getProperty(USER_XPATH).getType().getObjectResolver();
        assertNotNull(resolver);
        assertTrue(resolver instanceof UserManagerResolver);
        resolver = doc.getProperty(GROUP_XPATH).getType().getObjectResolver();
        assertNotNull(resolver);
        assertTrue(resolver instanceof UserManagerResolver);
        resolver = doc.getProperty(USER_GROUP_XPATH).getType().getObjectResolver();
        assertNotNull(resolver);
        assertTrue(resolver instanceof UserManagerResolver);
    }

    @Test
    public void testNullValueReturnNull() {
        assertNull(doc.getObjectResolver(USER_XPATH).fetch());
        assertNull(doc.getObjectResolver(USER_XPATH).fetch(DocumentModel.class));
        assertNull(doc.getProperty(USER_XPATH).getObjectResolver().fetch());
        assertNull(doc.getProperty(USER_XPATH).getObjectResolver().fetch(DocumentModel.class));
        assertNull(doc.getObjectResolver(GROUP_XPATH).fetch());
        assertNull(doc.getObjectResolver(GROUP_XPATH).fetch(DocumentModel.class));
        assertNull(doc.getProperty(GROUP_XPATH).getObjectResolver().fetch());
        assertNull(doc.getProperty(GROUP_XPATH).getObjectResolver().fetch(DocumentModel.class));
        assertNull(doc.getObjectResolver(USER_GROUP_XPATH).fetch());
        assertNull(doc.getObjectResolver(USER_GROUP_XPATH).fetch(DocumentModel.class));
        assertNull(doc.getProperty(USER_GROUP_XPATH).getObjectResolver().fetch());
        assertNull(doc.getProperty(USER_GROUP_XPATH).getObjectResolver().fetch(DocumentModel.class));
    }

    @Test
    public void testBadValuesValidationFailed() {
        doc.setPropertyValue(USER_XPATH, "totoDoesntExists");
        assertNull(doc.getProperty(USER_XPATH).getObjectResolver().fetch());
        assertFalse(doc.getProperty(USER_XPATH).getObjectResolver().validate());
        doc.setPropertyValue(GROUP_XPATH, "totoDoesntExists");
        assertNull(doc.getProperty(GROUP_XPATH).getObjectResolver().fetch());
        assertFalse(doc.getProperty(GROUP_XPATH).getObjectResolver().validate());
        doc.setPropertyValue(USER_GROUP_XPATH, "totoDoesntExists");
        assertNull(doc.getProperty(USER_GROUP_XPATH).getObjectResolver().fetch());
        assertFalse(doc.getProperty(USER_GROUP_XPATH).getObjectResolver().validate());
        assertEquals(3, validator.validate(doc).numberOfErrors());
    }

    @Test
    public void testUserCorrectValues() {
        doc.setPropertyValue(USER_XPATH, "user:Administrator");
        NuxeoPrincipal principal = (NuxeoPrincipal) doc.getProperty(USER_XPATH).getObjectResolver().fetch();
        assertNotNull(principal);
        assertEquals("Administrator", principal.getName());
        principal = doc.getProperty(USER_XPATH).getObjectResolver().fetch(NuxeoPrincipal.class);
        assertNotNull(principal);
        assertEquals("Administrator", principal.getName());
        principal = (NuxeoPrincipal) doc.getObjectResolver(USER_XPATH).fetch();
        assertNotNull(principal);
        assertEquals("Administrator", principal.getName());
        principal = doc.getObjectResolver(USER_XPATH).fetch(NuxeoPrincipal.class);
        assertNotNull(principal);
        assertEquals("Administrator", principal.getName());
    }

    @Test
    public void testUserDoesntSupportGroup() {
        doc.setPropertyValue(USER_XPATH, "group:members");
        assertNull(doc.getProperty(USER_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testUserNoPrefixWorks() {
        doc.setPropertyValue(USER_XPATH, "Administrator");
        assertNotNull(doc.getProperty(USER_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testUserOrGroupSupportsUser() {
        doc.setPropertyValue(USER_GROUP_XPATH, "user:Administrator");
        assertNotNull(doc.getProperty(USER_GROUP_XPATH).getObjectResolver());
    }

    @Test
    public void testGroupCorrectValues() {
        doc.setPropertyValue(GROUP_XPATH, "group:members");
        NuxeoGroup group = (NuxeoGroup) doc.getProperty(GROUP_XPATH).getObjectResolver().fetch();
        assertNotNull(group);
        assertEquals("members", group.getName());
        group = doc.getProperty(GROUP_XPATH).getObjectResolver().fetch(NuxeoGroup.class);
        assertNotNull(group);
        assertEquals("members", group.getName());
        group = (NuxeoGroup) doc.getObjectResolver(GROUP_XPATH).fetch();
        assertNotNull(group);
        assertEquals("members", group.getName());
        group = doc.getObjectResolver(GROUP_XPATH).fetch(NuxeoGroup.class);
        assertNotNull(group);
        assertEquals("members", group.getName());
    }

    @Test
    public void testGroupFieldDoesntSupportUser() {
        doc.setPropertyValue(GROUP_XPATH, "user:Administrator");
        assertNull(doc.getProperty(GROUP_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testGroupNoPrefixWorks() {
        doc.setPropertyValue(GROUP_XPATH, "members");
        assertNotNull(doc.getProperty(GROUP_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testUserOrGroupSupportsGroup() {
        doc.setPropertyValue(USER_GROUP_XPATH, "group:members");
        assertNotNull(doc.getProperty(USER_GROUP_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testTranslation() {
        UserManagerResolver allUMR = new UserManagerResolver();
        allUMR.configure(new HashMap<>());
        checkMessage(allUMR);
        UserManagerResolver userUMR = new UserManagerResolver();
        Map<String, String> userParams = new HashMap<>();
        userParams.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        userUMR.configure(userParams);
        checkMessage(userUMR);
        UserManagerResolver groupUMR = new UserManagerResolver();
        Map<String, String> groupParams = new HashMap<>();
        groupParams.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        groupUMR.configure(groupParams);
        checkMessage(groupUMR);

    }

    @Test
    public void testSerialization() throws Exception {
        // create it
        UserManagerResolver resolver = new UserManagerResolver();
        resolver.configure(new HashMap<>());
        // write it
        byte[] buffer = SerializationUtils.serialize(resolver);
        // forget the resolver
        resolver = null;
        // read it
        Object readObject = SerializationUtils.deserialize(buffer);
        // check it's a dir resolver
        assertTrue(readObject instanceof UserManagerResolver);
        UserManagerResolver readResolver = (UserManagerResolver) readObject;
        // check the configuration
        assertTrue(readResolver.isIncludingGroups());
        assertTrue(readResolver.isIncludingUsers());
        Map<String, Serializable> outputParameters = readResolver.getParameters();
        assertEquals(true, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_GROUPS));
        assertEquals(true, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_USERS));
        // test it works: validate
        assertTrue(readResolver.validate("user:Administrator"));
        // test it works: fetch
        Object entity = readResolver.fetch("user:Administrator");
        assertTrue(entity instanceof NuxeoPrincipal);
        assertEquals("Administrator", ((NuxeoPrincipal) entity).getName());
        // test it works: getReference
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        assertEquals("user:Administrator", readResolver.getReference(principal));
    }

    private void checkMessage(UserManagerResolver umr) {
        for (Locale locale : Arrays.asList(Locale.FRENCH, Locale.ENGLISH)) {
            String message = umr.getConstraintErrorMessage("abc123", locale);
            assertNotNull(message);
            assertFalse(message.trim().isEmpty());
            System.out.println(message);
        }
    }

}

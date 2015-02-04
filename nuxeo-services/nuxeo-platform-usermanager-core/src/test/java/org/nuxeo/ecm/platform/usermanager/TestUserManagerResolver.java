/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.runtime.api.Framework;

public class TestUserManagerResolver extends UserManagerTestCase {

    private static final String USER_XPATH = "umr:user";

    private static final String GROUP_XPATH = "umr:group";

    private static final String USER_GROUP_XPATH = "umr:userOrGroup";

    protected CoreSession session;

    protected DocumentValidationService validator;

    protected UserManager userManager;

    protected DocumentModel doc;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests", "test-usermanagerimpl/directory-config.xml");
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests", "test-usermanager-resolver.xml");
        session = Framework.getService(CoreSession.class);
        validator = Framework.getService(DocumentValidationService.class);
        userManager = Framework.getService(UserManager.class);
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
        Map<String, String> parameters = new HashMap<String, String>();
        umr.configure(parameters);
        List<Class<?>> classes = umr.getManagedClasses();
        assertEquals(2, classes.size());
        assertTrue(classes.contains(NuxeoPrincipal.class));
        assertTrue(classes.contains(NuxeoGroup.class));
    }

    @Test
    public void userSupportedClasses() throws Exception {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        List<Class<?>> classes = umr.getManagedClasses();
        assertEquals(1, classes.size());
        assertTrue(classes.contains(NuxeoPrincipal.class));
    }

    @Test
    public void groupSupportedClasses() throws Exception {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
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
        Map<String, String> parameters = new HashMap<String, String>();
        umr.configure(parameters);
        umr.configure(parameters);
    }

    @Test
    public void testConfigurationDefaultUserOrGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertTrue(umr.isIncludingGroups());
        assertTrue(umr.isIncludingUsers());
        Map<String, Serializable> outputParameters = umr.getParameters();
        assertEquals(true, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_GROUPS));
        assertEquals(true, outputParameters.get(UserManagerResolver.PARAM_INCLUDE_USERS));
    }

    @Test
    public void testConfigurationUser() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
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
        Map<String, String> parameters = new HashMap<String, String>();
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
        umr.configure(new HashMap<String, String>());
        assertEquals(UserManagerResolver.NAME, umr.getName());
    }

    @Test
    public void testValidateGoodUserWithDefaultConf() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertTrue(umr.validate("user:Administrator"));
    }

    @Test
    public void testValidateGoodUserWithFilterUser() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        assertTrue(umr.validate("user:Administrator"));
    }

    @Test
    public void testValidateUserFailedWithBadValue() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertFalse(umr.validate("toto"));
    }

    @Test
    public void testValidateUserFailedWithoutPrefix() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertFalse(umr.validate("Administrator"));
    }

    @Test
    public void testValidateUserFailedWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        assertFalse(umr.validate("user:Administrator"));
    }

    @Test
    public void testValidateGoodGroupWithDefaultConf() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertTrue(umr.validate("group:members"));
    }

    @Test
    public void testValidateGoodGroupWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        assertTrue(umr.validate("group:members"));
    }

    @Test
    public void testValidateGroupFailedWithBadValue() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertFalse(umr.validate("toto"));
    }

    @Test
    public void testValidateGroupFailedWithoutPrefix() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertFalse(umr.validate("members"));
    }

    @Test
    public void testValidateGroupFailedWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        assertFalse(umr.validate("group:members"));
    }

    @Test
    public void testFetchGoodUserWithDefaultConf() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        Object entity = umr.fetch("user:Administrator");
        assertTrue(entity instanceof NuxeoPrincipal);
        assertEquals("Administrator", ((NuxeoPrincipal) entity).getName());
    }

    @Test
    public void testFetchGoodUserWithFilterUser() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        Object entity = umr.fetch("user:Administrator");
        assertTrue(entity instanceof NuxeoPrincipal);
        assertEquals("Administrator", ((NuxeoPrincipal) entity).getName());
    }

    @Test
    public void testFetchUserFailedWithBadValue() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertNull(umr.fetch("user:toto"));
    }

    @Test
    public void testFetchUserFailedWithoutPrefix() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertNull(umr.fetch("Administrator"));
    }

    @Test
    public void testFetchUserFailedWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        assertNull(umr.fetch("user:Administrator"));
    }

    @Test
    public void testFetchGoodGroupWithDefaultConf() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        Object entity = umr.fetch("group:members");
        assertTrue(entity instanceof NuxeoGroup);
        assertEquals("members", ((NuxeoGroup) entity).getName());
    }

    @Test
    public void testFetchGoodGroupWithFilterGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        umr.configure(parameters);
        Object entity = umr.fetch("group:members");
        assertTrue(entity instanceof NuxeoGroup);
        assertEquals("members", ((NuxeoGroup) entity).getName());
    }

    @Test
    public void testFetchGroupFailedWithBadValue() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertNull(umr.fetch("group:toto"));
    }

    @Test
    public void testFetchGroupFailedWithoutPrefix() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertNull(umr.fetch("members"));
    }

    @Test
    public void testFetchGroupFailedWithFilterUser() {
        UserManagerResolver umr = new UserManagerResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        umr.configure(parameters);
        assertNull(umr.fetch("group:members"));
    }

    @Test
    public void testFetchCastNuxeoPrincipal() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        NuxeoPrincipal principal = umr.fetch(NuxeoPrincipal.class, "user:Administrator");
        assertNotNull(principal);
        assertEquals("Administrator", principal.getName());
        assertNull(umr.fetch(NuxeoPrincipal.class, "Administrator"));
        assertNull(umr.fetch(NuxeoPrincipal.class, "user:toto"));
        assertNull(umr.fetch(NuxeoPrincipal.class, "group:members"));
    }

    @Test
    public void testFetchCastNuxeoGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        NuxeoGroup group = umr.fetch(NuxeoGroup.class, "group:members");
        assertNotNull(group);
        assertEquals("members", group.getName());
        assertNull(umr.fetch(NuxeoGroup.class, "members"));
        assertNull(umr.fetch(NuxeoGroup.class, "group:toto"));
        assertNull(umr.fetch(NuxeoGroup.class, "user:Administrator"));
    }

    @Test
    public void testFetchCastDoesntSupportDocumentModel() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertNull(umr.fetch(DocumentModel.class, "user:Administrator"));
    }

    @Test
    public void testFetchCastDoesntSupportStupidTypes() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertNull(umr.fetch(List.class, "user:Administrator"));
    }

    @Test
    public void testGetReferenceUser() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        NuxeoPrincipal principal = userManager.getPrincipal("Administrator");
        assertEquals("user:Administrator", umr.getReference(principal));
    }

    @Test
    public void testGetReferenceGroup() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        NuxeoGroup group = userManager.getGroup("members");
        assertEquals("group:members", umr.getReference(group));
    }

    @Test
    public void testGetReferenceWithNonExistingUserWorks() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("chaps");
        assertEquals("user:chaps", umr.getReference(principal));
    }

    @Test
    public void testGetReferenceWithNonExistingGroupWorks() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        NuxeoGroup group = new NuxeoGroupImpl("chaps");
        assertEquals("group:chaps", umr.getReference(group));
    }

    @Test
    public void testGetReferenceInvalid() {
        UserManagerResolver umr = new UserManagerResolver();
        umr.configure(new HashMap<String, String>());
        assertNull(umr.getReference("nothing"));
    }

    @Test
    public void testConfigurationIsLoaded() {
        UserManagerResolver userResolver = (UserManagerResolver) ((SimpleType) doc.getProperty(USER_XPATH).getType()).getObjectResolver();
        assertTrue(userResolver.isIncludingUsers());
        assertFalse(userResolver.isIncludingGroups());
        UserManagerResolver groupResolver = (UserManagerResolver) ((SimpleType) doc.getProperty(GROUP_XPATH).getType()).getObjectResolver();
        assertFalse(groupResolver.isIncludingUsers());
        assertTrue(groupResolver.isIncludingGroups());
        UserManagerResolver anyResolver = (UserManagerResolver) ((SimpleType) doc.getProperty(USER_GROUP_XPATH).getType()).getObjectResolver();
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
    public void testUserWrongPrefixReturnNull() {
        doc.setPropertyValue(USER_XPATH, "Administrator");
        assertNull(doc.getProperty(USER_XPATH).getObjectResolver().fetch());
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
    public void testGroupWrongPrefixReturnNull() {
        doc.setPropertyValue(GROUP_XPATH, "members");
        assertNull(doc.getProperty(GROUP_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testUserOrGroupSupportsGroup() {
        doc.setPropertyValue(USER_GROUP_XPATH, "group:members");
        assertNotNull(doc.getProperty(USER_GROUP_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testTranslation() {
        UserManagerResolver allUMR = new UserManagerResolver();
        allUMR.configure(new HashMap<String, String>());
        checkMessage(allUMR);
        UserManagerResolver userUMR = new UserManagerResolver();
        Map<String, String> userParams = new HashMap<String, String>();
        userParams.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_USER);
        userUMR.configure(userParams);
        checkMessage(userUMR);
        UserManagerResolver groupUMR = new UserManagerResolver();
        Map<String, String> groupParams = new HashMap<String, String>();
        groupParams.put(UserManagerResolver.INPUT_PARAM_FILTER, UserManagerResolver.FILTER_GROUP);
        groupUMR.configure(groupParams);
        checkMessage(groupUMR);

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

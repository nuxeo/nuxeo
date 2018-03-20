/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.user.invite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.user.invite.UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class TestUserRegistration extends AbstractUserRegistration {

    private UserRegistrationConfiguration configuration;

    @Before
    public void init() {
        initializeRegistrations();
        configuration = ((UserInvitationComponent) userRegistrationService).configurations.get(
                DEFAULT_CONFIGURATION_NAME);
        DocumentModel group = userManager.getBareGroupModel();
        group.setPropertyValue(userManager.getGroupIdField(), "samplegroup");
        group.setPropertyValue(userManager.getGroupLabelField(), "Sample Group");
        userManager.createGroup(group);
    }

    @Test
    public void testTestContribution() {
        DocumentModel doc = session.createDocumentModel("TestRegistration");
        assertTrue(doc.hasFacet("UserInvitation"));

        assertNotNull(userRegistrationService);
        UserRegistrationConfiguration config = userRegistrationService.getConfiguration();
        assertEquals("Workspace", config.getContainerDocType());
    }

    @Test
    public void testBasicUserRegistration() {
        DocumentModelList users = userManager.searchUsers("jolivier");
        assertEquals(0, users.size());

        String requestId = userRegistrationService.submitRegistrationRequest(buildSampleUserInfo(),
                buildAdditionalInfo(), UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        users = userManager.searchUsers("jolivier");
        assertEquals(1, users.size());
        DocumentModel jolivier = users.get(0);
        assertEquals("tenant-a", jolivier.getPropertyValue("tenantId"));
    }

    @Test
    public void testBasicUserRegistrationWithLoginChanged() {
        // User info
        DocumentModel userInfo = buildSampleUserInfo();
        String templogin = "templogin";
        String newUser = "newUser";

        userInfo.setPropertyValue("userinfo:login", templogin);
        userInfo.setPropertyValue("userinfo:email", templogin + "@dummy.com");

        assertEquals(0, userManager.searchUsers(templogin).size());
        assertEquals(0, userManager.searchUsers(newUser).size());

        String requestId = userRegistrationService.submitRegistrationRequest(userInfo, buildAdditionalInfo(),
                UserInvitationService.ValidationMethod.NONE, true);
        Map<String, Serializable> additionnalInfos = new HashMap<>();
        additionnalInfos.put("userinfo:login", newUser);
        userRegistrationService.validateRegistration(requestId, additionnalInfos);

        assertEquals(0, userManager.searchUsers(templogin).size());
        assertEquals(1, userManager.searchUsers(newUser).size());
    }

    @Test(expected = UserAlreadyExistsException.class)
    public void testMultipleRegistrationWithSameEmail() {
        // User info
        DocumentModel userInfo = buildSampleUserInfo();
        String templogin = "templogin";
        String newUser = "newUser";

        userInfo.setPropertyValue("userinfo:login", templogin);
        userInfo.setPropertyValue("userinfo:email", templogin + "@dummy.com");

        String requestId = userRegistrationService.submitRegistrationRequest(userInfo, buildAdditionalInfo(),
                UserInvitationService.ValidationMethod.NONE, true);
        Map<String, Serializable> additionnalInfos = new HashMap<>();
        additionnalInfos.put("userinfo:login", newUser);
        userRegistrationService.validateRegistration(requestId, additionnalInfos);

        userInfo.setPropertyValue("userinfo:login", templogin + '1');
        // Must throw a UserAlreadyExistsException
        userRegistrationService.submitRegistrationRequest(userInfo, buildAdditionalInfo(),
                UserInvitationService.ValidationMethod.NONE, true);
    }

    @Test(expected = UserAlreadyExistsException.class)
    public void testMultipleRegistrationWithSameLogin() {
        // User info
        DocumentModel userInfo = buildSampleUserInfo();

        userInfo.setPropertyValue("userinfo:login", "jdoe");
        userInfo.setPropertyValue("userinfo:firstName", "John");
        userInfo.setPropertyValue("userinfo:lastName", "Doe");
        userInfo.setPropertyValue("userinfo:email", "johndoe@dummy.com");

        String requestId = userRegistrationService.submitRegistrationRequest(userInfo, buildAdditionalInfo(),
                UserInvitationService.ValidationMethod.NONE, true);
        Map<String, Serializable> additionnalInfos = new HashMap<>();
        userRegistrationService.validateRegistration(requestId, additionnalInfos);

        userInfo = buildSampleUserInfo();
        userInfo.setPropertyValue("userinfo:login", "jdoe");
        userInfo.setPropertyValue("userinfo:firstName", "Jane");
        userInfo.setPropertyValue("userinfo:email", "janeolivier@dummy.com");
        // Must throw a UserAlreadyExistsException
        userRegistrationService.submitRegistrationRequest(userInfo, buildAdditionalInfo(),
                UserInvitationService.ValidationMethod.NONE, true);
    }

    @Test
    public void testSingleUserCantInviteIntoOtherGroups() throws Exception {

        // Given a user in a sample group
        DocumentModel userInfo = buildSampleUserInfo();

        userInfo.setPropertyValue(configuration.getUserInfoGroupsField(), new String[] { "samplegroup" });

        String requestId = userRegistrationService.submitRegistrationRequest(userInfo, buildAdditionalInfo(),
                UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        assertFalse(userManager.getPrincipal("jolivier").isAdministrator());
        assertTrue(userManager.getPrincipal("jolivier").getAllGroups().contains("samplegroup"));

        // When that user invite another user and add the administrators group
        userInfo = buildSampleUserInfo();

        // Force the tenantId to null as the MultiTenant plugin is not deployed
        userInfo.setPropertyValue("userinfo:tenantId", null);
        userInfo.setPropertyValue("userinfo:login", "jolivier2");
        userInfo.setPropertyValue("userinfo:email", "oolivier2@dummy.com");
        userInfo.setPropertyValue(configuration.getUserInfoGroupsField(),
                new String[] { "administrators", "samplegroup" });

        requestId = userRegistrationService.submitRegistrationRequest(userInfo, buildAdditionalInfo("jolivier"),
                UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        // Then only the intersection of the group is added to the new user
        NuxeoPrincipal jolivier2 = userManager.getPrincipal("jolivier2");
        assertFalse(jolivier2.isAdministrator());
        assertTrue(jolivier2.getAllGroups().contains("samplegroup"));

    }

    @Test(expected = UserRegistrationException.class)
    public void testSingleUserCantInviteIntoOtherTenant() throws Exception {

        // Given a user in a tenant-a
        DocumentModel userInfo = buildSampleUserInfo();
        userInfo.setPropertyValue(configuration.getUserInfoGroupsField(), new String[] { "samplegroup" });

        String requestId = userRegistrationService.submitRegistrationRequest(userInfo, buildAdditionalInfo(),
                UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        assertFalse(userManager.getPrincipal("jolivier").isAdministrator());
        assertTrue(userManager.getPrincipal("jolivier").getAllGroups().contains("samplegroup"));

        // When that user invite another user in tenant-b
        userInfo = buildSampleUserInfo();
        userInfo.setPropertyValue("userinfo:login", "jolivier2");
        userInfo.setPropertyValue("userinfo:email", "oolivier2@dummy.com");
        userInfo.setPropertyValue("userinfo:tenantId", "tenant-b");

        requestId = userRegistrationService.submitRegistrationRequest(userInfo, buildAdditionalInfo("jolivier"),
                UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

    }

    private DocumentModel buildSampleUserInfo() {
        DocumentModel userInfo = session.createDocumentModel(configuration.getRequestDocType());
        userInfo.setPropertyValue("userinfo:login", "jolivier");
        userInfo.setPropertyValue("userinfo:firstName", "John");
        userInfo.setPropertyValue("userinfo:lastName", "Olivier");
        userInfo.setPropertyValue("userinfo:email", "oolivier@dummy.com");
        userInfo.setPropertyValue("userinfo:tenantId", "tenant-a");
        return userInfo;
    }

}

/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.usermanager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.exceptions.InvalidPasswordException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Tests the various way where the password validity is checked. In that setup, the password is valid if : - it has only
 * word characters : [a-zA-Z0-9_] - it's at least 8 long - it has at least one digit - it has at least two lowercase
 * letter - it has at least one uppercase letter
 *
 * @since 8.4
 */
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class TestUserManagerPasswordValidity extends UserManagerTestCase {

    private static final String BAD_PASSWORD = "a";

    private static final String GOOD_PASSWORD = "ALONGpassword123";

    @Test
    public void passwordValidityShouldBeCheckedIfNonEmpty() throws Exception {
        DocumentModel user = getUser("testUser");
        user.setProperty(userManager.getUserSchemaName(), getPasswordField(), BAD_PASSWORD);
        try {
            userManager.createUser(user);
            fail("Should throw an InvalidPasswordException");
        } catch (InvalidPasswordException e) {
        }

        user.setProperty(userManager.getUserSchemaName(), getPasswordField(), GOOD_PASSWORD);
        userManager.createUser(user);

        assertNotNull(userManager.getPrincipal("testUser"));
    }

    @Test
    public void passwordValidityShouldNotBeCheckedIfPasswordEmpty() throws Exception {
        DocumentModel user = getUser("testUser");
        userManager.createUser(user);
        assertNotNull(userManager.getPrincipal("testUser"));

    }

    @Test
    public void passwordShouldBeCheckedOnUpdate() throws Exception {
        DocumentModel user = getUser("testUser");
        user.setProperty(userManager.getUserSchemaName(), getPasswordField(), GOOD_PASSWORD);
        user = userManager.createUser(user);

        user.setProperty(userManager.getUserSchemaName(), getPasswordField(), BAD_PASSWORD);
        try {
            userManager.updateUser(user);
            fail("Should throw an InvalidPasswordException");
        } catch (InvalidPasswordException e) {

        }

    }

    @Test
    public void anInvalidExistingPasswordShouldNotPreventUserUpdate() throws Exception {
        // Given a user with a forced bad password
        DocumentModel user = getUser("testUser");
        user = userManager.createUser(user);
        DirectoryService dirService = Framework.getService(DirectoryService.class);
        try (Session userDir = dirService.open(userManager.getUserDirectoryName(), null)) {
            // We update the user's password via directory to prevent check from UserManager
            DocumentModel userentry = userDir.getEntry("testUser");
            userentry.setProperty(userManager.getUserSchemaName(), getPasswordField(), BAD_PASSWORD);
            userDir.updateEntry(userentry);
        }

        // When i update on property
        user.setProperty(userManager.getUserSchemaName(), userManager.getUserEmailField(), "jdoe@test.com");
        userManager.updateUser(user);

        // Then i doesn't throw a InvalidPasswordException
        assertNotNull(userManager.getPrincipal("testUser"));
    }

    private String getPasswordField() {
        String userDirectoryName = userManager.getUserDirectoryName();
        return Framework.getService(DirectoryService.class).getDirectory(userDirectoryName).getPasswordField();
    }

    private DocumentModel getUser(String userId) throws Exception {
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userId);
        return newUser;
    }

}

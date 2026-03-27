/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <thomas.roger@hyland.com>
 */
package org.nuxeo.ecm.platform.usermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Tests that the {@code user_password_changed} event is fired when a user's password is changed, and not fired when
 * other user fields are updated.
 *
 * @since 2025.18
 */
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class TestUserPasswordChangedEvent extends UserManagerTestCase {

    @Test
    public void testPasswordChangedEventFiredOnPasswordUpdate() {
        // Create a user
        var user = userManager.getBareUserModel();
        user.setProperty("user", "username", "testUser");
        user.setProperty("user", "password", "ALONGpassword123");
        userManager.createUser(user);

        try (var listener = new CapturingEventListener(UserManagerImpl.USER_PASSWORD_CHANGED_EVENT_ID)) {
            // Update the password
            var userModel = userManager.getUserModel("testUser");
            userModel.setProperty("user", "password", "NEWlongpassword456");
            userManager.updateUser(userModel);

            // The event should have been fired
            assertTrue("user_password_changed event should be fired on password change",
                    listener.hasBeenFired(UserManagerImpl.USER_PASSWORD_CHANGED_EVENT_ID));
        }
    }

    @Test
    public void testPasswordChangedEventNotFiredOnOtherFieldUpdate() {
        // Create a user
        var user = userManager.getBareUserModel();
        user.setProperty("user", "username", "testUser2");
        user.setProperty("user", "password", "ALONGpassword789");
        userManager.createUser(user);

        try (var listener = new CapturingEventListener(UserManagerImpl.USER_PASSWORD_CHANGED_EVENT_ID)) {
            // Update a non-password field
            var userModel = userManager.getUserModel("testUser2");
            userModel.setProperty("user", "firstName", "Updated");
            userManager.updateUser(userModel);

            // The event should NOT have been fired
            assertFalse("user_password_changed event should not be fired on non-password update",
                    listener.hasBeenFired(UserManagerImpl.USER_PASSWORD_CHANGED_EVENT_ID));
        }
    }

    @Test
    public void testContextDataPropagatedToEvent() {
        // Create a user
        var user = userManager.getBareUserModel();
        user.setProperty("user", "username", "testUser3");
        user.setProperty("user", "password", "ALONGpassword000");
        userManager.createUser(user);

        try (var listener = new CapturingEventListener(UserManagerImpl.USER_PASSWORD_CHANGED_EVENT_ID)) {
            // Update the password with context data carrying a session ID
            var userModel = userManager.getUserModel("testUser3");
            userModel.setProperty("user", "password", "NEWlongpassword111");
            userModel.putContextData(UserManager.USER_HTTP_SESSION_ID_KEY, "test-session-42");
            userManager.updateUser(userModel);

            // The event should have been fired with the context data propagated
            var event = listener.findFirstCapturedEventOrElseThrow(UserManagerImpl.USER_PASSWORD_CHANGED_EVENT_ID);
            var sessionId = (String) event.getContext().getProperty(UserManager.USER_HTTP_SESSION_ID_KEY);
            assertEquals("session ID should be propagated through event context", "test-session-42", sessionId);
        }
    }
}

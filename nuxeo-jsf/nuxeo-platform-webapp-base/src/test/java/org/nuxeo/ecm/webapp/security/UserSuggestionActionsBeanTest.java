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
 *     egiuly
 */
package org.nuxeo.ecm.webapp.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * @author egiuly
 */
public class UserSuggestionActionsBeanTest extends BaseUserGroupMock {

    private UserSuggestionActionsBean userSuggestionActionsBean = new UserSuggestionActionsBean();

    private Map<String, Object> userInfo;

    @Before
    public void doBefore() {
        userSuggestionActionsBean.userManager = mockUserManager();
    }

    @Test
    public void getUserInfoShouldReturnUserInfo() {
        String id = "user";
        userInfo = userSuggestionActionsBean.getUserInfo(id);
        assertTrue(userInfo.containsKey("id"));
        assertEquals(userInfo.get("id"), id);
        assertTrue(userInfo.containsKey("type"));
        assertEquals(userInfo.get("type"), "USER_TYPE");
        assertTrue(userInfo.containsKey("entry"));
    }

    @Test
    public void getUserInfoShouldReturnGroupInfo() {
        String id = "administrators";
        userInfo = userSuggestionActionsBean.getUserInfo(id);
        assertTrue(userInfo.containsKey("id"));
        assertEquals(userInfo.get("id"), id);
        assertTrue(userInfo.containsKey("type"));
        assertEquals(userInfo.get("type"), "GROUP_TYPE");
        assertTrue(userInfo.containsKey("entry"));
    }

    @Test
    public void getUserInfoShouldNotReturnInfoWhenBlankId() {
        userInfo = userSuggestionActionsBean.getUserInfo("");
        assertTrue(userInfo.containsKey("id"));
        assertFalse(userInfo.containsKey("type"));
        assertFalse(userInfo.containsKey("entry"));

        userInfo = userSuggestionActionsBean.getUserInfo(null);
        assertTrue(userInfo.containsKey("id"));
        assertFalse(userInfo.containsKey("type"));
        assertFalse(userInfo.containsKey("entry"));
    }

}

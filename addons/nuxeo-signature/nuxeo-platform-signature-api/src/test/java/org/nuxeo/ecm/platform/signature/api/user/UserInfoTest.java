/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.api.user;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public class UserInfoTest {

    /**
     * @throws java.lang.Exception
     */
    /**
     * Check that the returned field sequence is always the same
     */
    @Test
    public void testGetDN() throws Exception {

        Map<CNField, String> userFields;
        userFields = new HashMap<>();
        userFields.put(CNField.C, "US");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.OU, "IT");
        userFields.put(CNField.CN, "Wojciech Sulejman");
        userFields.put(CNField.Email, "wsulejman@nuxeo.com");
        userFields.put(CNField.UserID, "wsulejman");

        UserInfo ui = new UserInfo(userFields);
        assertEquals(ui.getDN(userFields), ("C=US, O=Nuxeo, OU=IT, CN=Wojciech Sulejman"));

        Map<CNField, String> userFields2;
        userFields2 = new HashMap<>();
        userFields2.put(CNField.OU, "IT");
        userFields2.put(CNField.Email, "wsulejman@nuxeo.com");
        userFields2.put(CNField.O, "Nuxeo");
        userFields2.put(CNField.C, "US");
        userFields2.put(CNField.CN, "Wojciech Sulejman");
        userFields2.put(CNField.UserID, "wsulejman");

        UserInfo ui2 = new UserInfo(userFields2);
        assertEquals(ui2.getDN(userFields2), ("C=US, O=Nuxeo, OU=IT, CN=Wojciech Sulejman"));

    }
}

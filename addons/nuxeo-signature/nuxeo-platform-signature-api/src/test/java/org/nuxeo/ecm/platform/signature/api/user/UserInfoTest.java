/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.C, "US");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.OU, "IT");
        userFields.put(CNField.CN, "Wojciech Sulejman");
        userFields.put(CNField.Email, "wsulejman@nuxeo.com");
        userFields.put(CNField.UserID, "wsulejman");

        UserInfo ui = new UserInfo(userFields);
        assertEquals(ui.getDN(userFields), ("C=US, O=Nuxeo, OU=IT, CN=Wojciech Sulejman"));

        Map<CNField, String> userFields2;
        userFields2 = new HashMap<CNField, String>();
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

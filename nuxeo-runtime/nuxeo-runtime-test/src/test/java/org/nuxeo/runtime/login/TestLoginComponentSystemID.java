/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.login;

import java.security.Principal;

import org.nuxeo.runtime.api.login.LoginComponent;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 */
public class TestLoginComponentSystemID extends NXRuntimeTestCase {

    public void testSystemIDEquals() {
        Principal user1 = new LoginComponent.SystemID(
                LoginComponent.SYSTEM_USERNAME);
        Principal user2 = new LoginComponent.SystemID(
                LoginComponent.SYSTEM_USERNAME);
        assertNotNull(user1);
        assertEquals(user1, user2);

        Principal otherUser = new LoginComponent.SystemID("toto");
        assertFalse(user1.equals(otherUser));

        Principal nullUser = new LoginComponent.SystemID();
        assertFalse(user1.equals(nullUser));
    }

}

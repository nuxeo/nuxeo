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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestPasswordHelper {

    @Test
    public void testVerify() {
        assertTrue(PasswordHelper.verifyPassword("abcd", "abcd"));
        assertTrue(PasswordHelper.verifyPassword("abcd",
                "{SSHA}WPvqVeSt0Mr2llICYmAX9+pjtPH271eznDHvrw=="));
        assertTrue(PasswordHelper.verifyPassword("abcd",
                "{SMD5}/wZ7JQUARlCBq4JFHI57AfbvV7OcMe+v"));
        assertFalse(PasswordHelper.verifyPassword("1234", "abcd"));
        assertFalse(PasswordHelper.verifyPassword("1234",
                "{SSHA}WPvqVeSt0Mr2llICYmAX9+pjtPH271eznDHvrw=="));
        assertFalse(PasswordHelper.verifyPassword("1234",
                "{SMD5}/wZ7JQUARlCBq4JFHI57AfbvV7OcMe+v"));
        assertFalse(PasswordHelper.verifyPassword(" abcd", "abcd"));
        assertFalse(PasswordHelper.verifyPassword("abcd", "{SSHA}WPvqVeS"));
        assertFalse(PasswordHelper.verifyPassword("abcd", "{SSHA}/wZ7JQUAR"));
        assertFalse(PasswordHelper.verifyPassword("abcd", "{SSHA}WXYZ"));
        assertFalse(PasswordHelper.verifyPassword("abcd",
                "{SSHA}WPvqVeSt0Mr2llICYmAX9+pjtPH271eznDHvrwfghijkl"));
        assertFalse(PasswordHelper.verifyPassword("abcd", "{SSHA}/wZ7JQUARlC*"));
    }

    @Test
    public void testHash() {
        String password = "abcdéf";
        String hashed;
        hashed = PasswordHelper.hashPassword(password, null);
        assertEquals(password, hashed);
        assertTrue(PasswordHelper.verifyPassword(password, hashed));
        hashed = PasswordHelper.hashPassword(password, PasswordHelper.SSHA);
        assertTrue(hashed.startsWith("{SSHA}"));
        assertTrue(PasswordHelper.verifyPassword(password, hashed));
        hashed = PasswordHelper.hashPassword(password, PasswordHelper.SMD5);
        assertTrue(hashed.startsWith("{SMD5}"));
        assertTrue(PasswordHelper.verifyPassword(password, hashed));
    }

    @Test
    public void testIsHashed() {
        assertTrue(PasswordHelper.isHashed("{SSHA}foo"));
        assertTrue(PasswordHelper.isHashed("{SMD5}foo"));
        assertFalse(PasswordHelper.isHashed("{foo}bar"));
        assertFalse(PasswordHelper.isHashed("foo"));
    }

}

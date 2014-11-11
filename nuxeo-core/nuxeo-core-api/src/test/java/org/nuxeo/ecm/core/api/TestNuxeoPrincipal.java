/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.impl.UserPrincipal;

public class TestNuxeoPrincipal extends TestCase {

    public void test() {
        NuxeoPrincipal principal = new UserPrincipal("john");

        assertEquals("john", principal.getName());

        principal.setFirstName("john");
        assertEquals("john", principal.getFirstName());

        principal.setLastName("paul");
        assertEquals("paul", principal.getLastName());

        principal.setCompany("nuxeo");
        assertEquals("nuxeo", principal.getCompany());
    }

    public void testEquals() {
        NuxeoPrincipal john1 = new UserPrincipal("john");
        NuxeoPrincipal john2 = new UserPrincipal("john");
        NuxeoPrincipal jim = new UserPrincipal("jim");

        assertEquals(john1, john2);
        assertEquals(john1.hashCode(), john2.hashCode());
        assertFalse(john1.equals(jim));
        assertFalse(jim.equals(john1));

        jim.setName("john");
        assertEquals(john1, jim);
    }

}

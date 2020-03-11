/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.impl.UserPrincipal;

public class TestNuxeoPrincipal {

    @Test
    public void test() {
        NuxeoPrincipal principal = new UserPrincipal("john", new ArrayList<String>(), false, false);

        assertEquals("john", principal.getName());

        principal.setFirstName("john");
        assertEquals("john", principal.getFirstName());

        principal.setLastName("paul");
        assertEquals("paul", principal.getLastName());

        principal.setCompany("nuxeo");
        assertEquals("nuxeo", principal.getCompany());
    }

    @Test
    public void testEquals() {
        NuxeoPrincipal john1 = new UserPrincipal("john", new ArrayList<String>(), false, false);
        NuxeoPrincipal john2 = new UserPrincipal("john", new ArrayList<String>(), false, false);
        NuxeoPrincipal jim = new UserPrincipal("jim", new ArrayList<String>(), false, false);

        assertEquals(john1, john2);
        assertEquals(john1.hashCode(), john2.hashCode());
        assertFalse(john1.equals(jim));
        assertFalse(jim.equals(john1));

        jim.setName("john");
        assertEquals(john1, jim);
    }
}

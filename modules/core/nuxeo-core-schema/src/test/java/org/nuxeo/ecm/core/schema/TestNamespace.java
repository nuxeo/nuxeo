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

package org.nuxeo.ecm.core.schema;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestNamespace {

    @Test
    public void testNameSpace() {
        Namespace ns1 = new Namespace("uri", "prefix");
        Namespace ns2 = new Namespace("uri", "prefix");
        Namespace ns3 = new Namespace("uri", null);
        Namespace ns4 = new Namespace("uri2", "prefix");

        assertTrue(ns1.hasPrefix());
        assertFalse(ns3.hasPrefix());

        assertEquals(ns1, ns1);
        assertEquals(ns1, ns2);
        assertEquals(ns1.hashCode(), ns2.hashCode());
        assertEquals(ns1.toString(), ns2.toString());

        assertNotNull(ns1);
        assertFalse(ns1.equals(ns3));
        assertFalse(ns3.equals(ns1));
        assertFalse(ns1.equals(ns4));
        assertFalse(ns4.equals(ns1));

        assertFalse(ns1.toString().equals(ns3.toString()));
        assertFalse(ns1.toString().equals(ns4.toString()));
    }

}

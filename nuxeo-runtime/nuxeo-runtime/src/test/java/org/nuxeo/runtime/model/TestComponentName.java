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

package org.nuxeo.runtime.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestComponentName {

    @Test
    public void test() {
        ComponentName cn1 = new ComponentName("foo:bar");
        ComponentName cn2 = new ComponentName("foo", "bar");
        ComponentName cn3 = new ComponentName("fu:baz");

        assertEquals("foo", cn1.getType());
        assertEquals("bar", cn1.getName());
        assertEquals("foo:bar", cn1.getRawName());

        assertEquals(cn1, cn2);
        assertEquals(cn1.hashCode(), cn2.hashCode());
        assertEquals(cn1.toString(), cn2.toString());

        assertNotNull(cn1);
        assertFalse(cn1.equals(cn3));
        assertFalse(cn3.equals(cn1));
    }

}

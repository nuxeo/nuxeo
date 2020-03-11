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
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.apidoc.documentation.DocumentationHelper;

public class TestSecureXML {

    @Test
    public void testSecureXML() throws Exception {
        String xml = "foo <password>p1</password> " //
                + " <myPassword>p2</myPassword>" //
                + " <yo password=\"p3\" name=\"bla\">" //
                + " <yo otherPassword=\"p4\" name=\"bla\">" //
                + " <prop name=\"password\">p5</prop>" //
                + " <prop name=\"realPassword\">p6</prop>";
        String expected = "foo <password>********</password> " //
                + " <myPassword>********</myPassword>" //
                + " <yo password=\"********\" name=\"bla\">" //
                + " <yo otherPassword=\"********\" name=\"bla\">" //
                + " <prop name=\"password\">********</prop>" //
                + " <prop name=\"realPassword\">********</prop>";
        String res = DocumentationHelper.secureXML(xml);
        assertEquals(expected, res);
    }

}

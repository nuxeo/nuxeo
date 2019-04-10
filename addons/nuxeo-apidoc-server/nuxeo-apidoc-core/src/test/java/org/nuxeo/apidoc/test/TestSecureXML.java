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
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.test;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

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

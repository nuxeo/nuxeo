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
package org.nuxeo.ecm.core.storage.sql;

import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestFulltextWordSplit extends NXRuntimeTestCase {

    protected void check(String expected, String s) {
        FulltextParser parser = new FulltextParser();
        parser.strings = new ArrayList<String>();
        parser.parse(s, "fakepath");
        assertEquals(expected, StringUtils.join(parser.strings, "|"));
    }

    @Test
    public void testDefaultParser() throws Exception {
        check("abc", "abc");
        check("abc|def", "abc def");
        check("abc|def", " abc    def  ");
        check("abc|def", "  -,abc DEF?? !");
        // accents left alone
        check("hot|caf\u00e9", "hot CAF\u00c9");
    }

}

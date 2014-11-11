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
package org.nuxeo.ecm.core.storage;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDefaultFulltextParser extends NXRuntimeTestCase {

    protected void check(String expected, String s) {
        FulltextParser parser = new DefaultFulltextParser();
        List<String> strings = new ArrayList<String>();
        parser.parse(s, "fakepath", strings);
        assertEquals(expected, StringUtils.join(strings, "|"));
    }

    @Test
    public void testDefaultParser() throws Exception {
        check("abc", "abc");
        check("abc|def", "abc def");
        check("abc|def", " abc    def  ");
        check("abc|def", "  -,abc DEF?? !");
        // accents left alone
        check("hot|caf\u00e9", "hot CAF\u00c9");
        // check html removal and entities unescape
        check("test|é|test", "test &eacute; test");
        check("test|é|test", "test <p style=\"something\">&eacute;</p> test");
    }

}

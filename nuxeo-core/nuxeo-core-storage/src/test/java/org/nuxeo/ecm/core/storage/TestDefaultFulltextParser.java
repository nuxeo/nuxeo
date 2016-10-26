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
package org.nuxeo.ecm.core.storage;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDefaultFulltextParser extends NXRuntimeTestCase {

    protected void check(String expected, String s, String mimeType) {
        FulltextParser parser = new DefaultFulltextParser();
        List<String> strings = new ArrayList<String>();
        parser.parse(s, "fakepath", mimeType, null, strings);
        assertEquals(expected, StringUtils.join(strings, "|"));
    }

    @Test
    public void testDefaultParser() throws Exception {
        check("abc", "abc", null);
        check("abc|def", "abc def", null);
        check("abc|def", " abc    def  ", null);
        check("abc|def", "  -,abc DEF?? !", null);
        // accents left alone
        check("hot|caf\u00e9", "hot CAF\u00c9", null);
        // check html removal and entities unescape
        check("test|é|test", "test &eacute; test", null);
        check("test|é|test", "test &eacute; test", "text/html");
        check("test|é|test", "<html>test &eacute; test</html>", null);

        check("test|p|style|something|é|p|test", "test <p style=\"something\">&eacute;</p> test", null);
        check("test|é|test", "test <p style=\"something\">&eacute;</p> test", "text/html");
        check("test|é|test", "<html>test <p style=\"something\">&eacute;</p> test</html>", null);
    }

}

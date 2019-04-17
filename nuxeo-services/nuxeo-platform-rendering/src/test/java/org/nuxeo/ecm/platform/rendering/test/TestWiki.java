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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializer;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;
import org.wikimodel.wem.WikiParserException;

/** @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> */
public class TestWiki {

    @Test
    public void test() throws Exception {
        InputStream in = TestWiki.class.getResourceAsStream("/testdata/test.wiki");
        try (Reader reader = new InputStreamReader(in)) {

            WikiSerializer engine = new WikiSerializer();
            engine.addFilter(new PatternFilter("_([-A-Za-z0-9]+)_", "<i>$1</i>"));
            engine.addFilter(new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>"));
            engine.addFilter(new PatternFilter("NXP-[0-9]+", "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));

            StringWriter writer = new StringWriter();
            engine.serialize(reader, writer);
        }
    }

    @Test
    public void test2() throws IOException, WikiParserException {
        InputStream in = TestWiki.class.getResourceAsStream("/testdata/test2.wiki");
        try (Reader reader = new InputStreamReader(in)) {

            WikiSerializer engine = new WikiSerializer();
            engine.addFilter(new PatternFilter("_([-A-Za-z0-9]+)_", "<i>$1</i>"));
            engine.addFilter(new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>"));
            engine.addFilter(new PatternFilter("NXP-[0-9]+", "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));

            StringWriter writer = new StringWriter();
            engine.serialize(reader, writer);
        }

        // System.out.println(writer.getBuffer());
    }

    @Test
    public void test3() throws Exception {
        InputStream in = TestWiki.class.getResourceAsStream("/testdata/test3.wiki");
        try (Reader reader = new InputStreamReader(in)) {

            WikiSerializer engine = new WikiSerializer();
            engine.addFilter(new PatternFilter("_([-A-Za-z0-9]+)_", "<i>$1</i>"));
            engine.addFilter(new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>"));
            engine.addFilter(new PatternFilter("NXP-[0-9]+", "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));

            StringWriter writer = new StringWriter();
            engine.serialize(reader, writer);

            String out = writer.toString();

            assertTrue(out.contains("<img src="));
            assertFalse(out.contains("<script"));

            System.out.println(out);
        }
    }

}

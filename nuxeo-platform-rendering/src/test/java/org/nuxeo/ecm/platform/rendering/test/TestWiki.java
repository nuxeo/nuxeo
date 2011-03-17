/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializer;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;
import org.wikimodel.wem.WikiParserException;

/** @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> */
public class TestWiki extends TestCase {

    public void test() throws IOException, WikiParserException {
        double s = System.currentTimeMillis();
        InputStream in = TestWiki.class.getResourceAsStream("/testdata/test.wiki");
        Reader reader = new InputStreamReader(in);

        WikiSerializer engine = new WikiSerializer();
        engine.addFilter(new PatternFilter("_([-A-Za-z0-9]+)_", "<i>$1</i>"));
        engine.addFilter(new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>"));
        engine.addFilter(new PatternFilter("NXP-[0-9]+",
                "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));

        StringWriter writer = new StringWriter();
        engine.serialize(reader, writer);

        // System.out.println(writer.getBuffer());
        // System.out.println(">>>>>>> " + ((System.currentTimeMillis() - s) / 1000));
    }

    public void test2() throws IOException, WikiParserException {
        InputStream in = TestWiki.class.getResourceAsStream("/testdata/test2.wiki");
        Reader reader = new InputStreamReader(in);

        WikiSerializer engine = new WikiSerializer();
        engine.addFilter(new PatternFilter("_([-A-Za-z0-9]+)_", "<i>$1</i>"));
        engine.addFilter(new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>"));
        engine.addFilter(new PatternFilter("NXP-[0-9]+",
                "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));

        StringWriter writer = new StringWriter();
        engine.serialize(reader, writer);

        // System.out.println(writer.getBuffer());
    }

    public void test3() throws IOException, WikiParserException {
        InputStream in = TestWiki.class.getResourceAsStream("/testdata/test3.wiki");
        Reader reader = new InputStreamReader(in);

        WikiSerializer engine = new WikiSerializer();
        engine.addFilter(new PatternFilter("_([-A-Za-z0-9]+)_", "<i>$1</i>"));
        engine.addFilter(new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>"));
        engine.addFilter(new PatternFilter("NXP-[0-9]+",
                "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));

        StringWriter writer = new StringWriter();
        engine.serialize(reader, writer);

        String out = writer.toString();

        assertTrue(out.contains("<img src="));
        assertFalse(out.contains("<script"));

        System.out.println(out);
    }


}

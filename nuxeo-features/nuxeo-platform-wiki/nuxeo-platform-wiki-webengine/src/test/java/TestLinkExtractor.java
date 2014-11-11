/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.wiki.listener.WikiHelper;
import org.nuxeo.ecm.wiki.listener.WordExtractor;
import org.wikimodel.wem.WikiParserException;
import org.wikimodel.wem.common.CommonWikiParser;

public class TestLinkExtractor {

    @Test
    public void testOne() throws WikiParserException{
        InputStream in = TestLinkExtractor.class.getClassLoader().getResourceAsStream("test.txt");
        CommonWikiParser parser = new CommonWikiParser();
        StringBuffer sb = new StringBuffer();
        parser.parse(new InputStreamReader(in), new WordExtractor(sb));
        // System.out.println(sb.toString());

        List<String> workLinks = WikiHelper.getWordLinks(sb.toString());
        assertEquals(2, workLinks.size());
        assertEquals("WikiName.PageName", workLinks.get(0));
        assertEquals("PageParsing", workLinks.get(1));
    }

}

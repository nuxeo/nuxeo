/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.wiki.listener;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.wiki.WikiTypes;
import org.wikimodel.wem.WikiParserException;
import org.wikimodel.wem.common.CommonWikiParser;

public class WikiHelper {

    // TODO make this configurable
    static final String PATTERN = "([A-Z]+[a-z]+[A-Z][A-Za-z]*.)?([A-Z]+[a-z]+[A-Z][A-Za-z]*)";
    static final Pattern WORD_LINKS_PATTERN = Pattern.compile(PATTERN);

    public static List<String> getWordLinks(DocumentModel doc){
        try {
            String content = (String) doc.getPart(WikiTypes.SCHEMA_WIKIPAGE).get(WikiTypes.FIELD_CONTENT).getValue();
            StringBuffer collector = new StringBuffer();
            WordExtractor extractor= new WordExtractor(collector);
            CommonWikiParser parser = new CommonWikiParser();
            try {
                parser.parse(new StringReader(content), extractor);
            } catch (WikiParserException e) {
                e.printStackTrace();
            }
            return getWordLinks(collector.toString());
        } catch (PropertyNotFoundException e) {
            e.printStackTrace();
        } catch (PropertyException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static List<String> getWordLinks(String text){
        List<String> wordLinks = new ArrayList<String>();
        Matcher matcher = WORD_LINKS_PATTERN.matcher(text);
        while (matcher.find()) {
            String s = matcher.group(0);
            if ( !wordLinks.contains(s) ){
                wordLinks.add(s);
            }
        }
        return wordLinks;
    }

}

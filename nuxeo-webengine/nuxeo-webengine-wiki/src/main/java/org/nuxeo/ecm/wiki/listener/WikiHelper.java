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

import static org.nuxeo.ecm.wiki.rendering.WikiPageLinkResolver.PAGE_LINK_PATTERN;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.util.RelationConstants;
import org.nuxeo.ecm.webengine.util.RelationHelper;
import org.nuxeo.ecm.wiki.WikiTypes;
import org.wikimodel.wem.WikiParserException;
import org.wikimodel.wem.common.CommonWikiParser;

public class WikiHelper {

    public static final Resource HAS_LINK_TO = new ResourceImpl("http://www.nuxeo.org/wiki/hasLinkTo");

    // TODO fix this hardcoded path
    public static final String WIKI_ROOT_PATH = "/default-domain/workspaces/wikis";

    // FIXME: properly handle exceptions
    public static List<String> getWordLinks(DocumentModel doc) {
        try {
            String content = (String) doc.getPart(WikiTypes.SCHEMA_WIKIPAGE).get(WikiTypes.FIELD_CONTENT).getValue();
            StringBuffer collector = new StringBuffer();
            WordExtractor extractor = new WordExtractor(collector);
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
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    // this will update links graph
    // TODO optimize this!
    // keep old statements
    public static void updateRelations(DocumentModel doc) {
        List<String> list = getWordLinks(doc);
        List<Statement> stmts = RelationHelper.getStatements(doc, HAS_LINK_TO);
        try {
            // remove old links
            RelationManager rm = RelationHelper.getRelationManager();
            if (stmts != null) {
                rm.remove(RelationConstants.GRAPH_NAME, stmts);
                stmts.clear();
            } else {
                stmts = new ArrayList<Statement>();
            }

            // add new links
            if (list != null) {
                QNameResource docResource = RelationHelper.getDocumentResource(doc);
                for (String word : list) {
                    if (!word.startsWith(".")) {
                        word = getAbsolutePageLink(doc, word);
                    }
                    Statement stmt = new StatementImpl(
                            docResource, HAS_LINK_TO, new LiteralImpl(word));
                    stmts.add(stmt);
                }
                rm.add(RelationConstants.GRAPH_NAME, stmts);
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getWordLinks(String text) {
        List<String> wordLinks = new ArrayList<String>();
        Matcher matcher = PAGE_LINK_PATTERN.matcher(text);
        while (matcher.find()) {
            String s = matcher.group(0);
            if (!wordLinks.contains(s)) {
                wordLinks.add(s);
            }
        }
        return wordLinks;
    }

    public static String getAbsolutePageLink(DocumentModel doc, String relativeLink) {
        if (relativeLink.startsWith(".")) {
            return relativeLink;
        }
        Path path = doc.getPath().removeFirstSegments(3);
        path = path.removeLastSegments(1);
        return "." + path.toString().replace("/", ".") + "." + relativeLink;
    }

    public static List<Map<String, String>> getLinks(WebContext ctx) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        DocumentModel doc = null;
        org.nuxeo.ecm.webengine.model.Resource resource = ctx.getTargetObject();
        if (resource instanceof DocumentObject) {
            DocumentObject docObj = (DocumentObject) resource;
            doc = docObj.getDocument();
        }
        if (doc == null) {
            return list;
        }

        DocumentModelList l = RelationHelper.getSubjectDocuments(
                HAS_LINK_TO, getPageAbsoluteLink(doc), doc.getSessionId());

        String prefix = ctx.getModulePath();

        for (DocumentModel d : l) {
            Map<String, String> map = new HashMap<String, String>();
            try {
                map.put("title", d.getTitle());
                map.put("href", prefix + "/" + d.getPath().removeFirstSegments(3).toString());
                list.add(map);
            } catch (ClientException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static String getPageAbsoluteLink(DocumentModel doc) {
        String s = doc.getPath().removeFirstSegments(3).toString();
        return "." + s.replace("/", ".");
    }

}

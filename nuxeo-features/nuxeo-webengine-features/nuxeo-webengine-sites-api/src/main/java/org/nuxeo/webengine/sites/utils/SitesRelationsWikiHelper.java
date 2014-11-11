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

package org.nuxeo.webengine.sites.utils;

import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_CONTENT;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.wikimodel.wem.WikiParserException;
import org.wikimodel.wem.common.CommonWikiParser;

/**
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
public class SitesRelationsWikiHelper {

    public static final Resource HAS_LINK_TO = new ResourceImpl(
            "http://www.nuxeo.org/sitesWiki/hasLinkTo");

    // Utility class.
    private SitesRelationsWikiHelper() {
    }

    public static void updateRelations(DocumentModel doc, List<String> linksList) throws ClientException {
        List<String> list = getWordLinks(doc);

        List<Statement> stmts = RelationHelper.getStatements(doc, HAS_LINK_TO);

        // remove old links
        RelationManager rm = RelationHelper.getRelationManager();
        if (stmts != null) {
            rm.remove(RelationConstants.GRAPH_NAME, stmts);
            stmts.clear();
        } else {
            stmts = new ArrayList<Statement>();
        }

        QNameResource docResource = RelationHelper.getDocumentResource(doc);
        String basePath = (String) doc.getContextData("basePath");
        String siteName = (String) doc.getContextData("siteName");
        String targetBasePath = (String) doc.getContextData("targetObjectPath");

        for (String word : list) {
            if (word.startsWith(".")) {
                word = basePath + "/" + siteName + word.replace(".", "/");
            } else {
                word = targetBasePath + "/" + word;
            }
            Statement stmt = new StatementImpl(docResource,
                    HAS_LINK_TO, new LiteralImpl(word));
            stmts.add(stmt);
        }

        // Add additional links
        for (String word : linksList) {
            Statement stmt = new StatementImpl(docResource,
                    HAS_LINK_TO, new LiteralImpl(word));
            stmts.add(stmt);
        }
        rm.add(RelationConstants.GRAPH_NAME, stmts);
    }

    public static List<String> getWordLinks(DocumentModel doc) {
        try {
            String content = (String) doc.getPropertyValue(WEBPAGE_CONTENT);
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
        return new ArrayList<String>();
    }

    public static List<String> getWordLinks(String text) {
        List<String> wordLinks = new ArrayList<String>();
        Matcher matcher = SiteConstants.PAGE_LINK_PATTERN.matcher(text);
        while (matcher.find()) {
            String s = matcher.group(0);
            if (!wordLinks.contains(s)) {
                wordLinks.add(s);
            }
        }
        return wordLinks;
    }

}

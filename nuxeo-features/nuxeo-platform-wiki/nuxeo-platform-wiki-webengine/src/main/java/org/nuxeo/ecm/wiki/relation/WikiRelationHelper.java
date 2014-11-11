/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.wiki.relation;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.api.util.RelationHelper;
import org.nuxeo.ecm.wiki.listener.WikiHelper;

public class WikiRelationHelper {

    private WikiRelationHelper() {
    }

    // this will update links graph
    // TODO optimize this!
    // keep old statements
    public static void updateRelations(DocumentModel doc) {
        List<String> list = WikiHelper.getWordLinks(doc);
        List<Statement> stmts = RelationHelper.getStatements(doc, WikiRelationConstants.HAS_LINK_TO);
        try {
            // remove old links
            RelationManager rm = RelationHelper.getRelationManager();
            Graph graph = rm.getGraphByName(RelationConstants.GRAPH_NAME);
            if (stmts != null) {
                graph.remove(stmts);
                stmts.clear();
            } else {
                stmts = new ArrayList<Statement>();
            }

            // add new links
            if (list != null) {
                QNameResource docResource = RelationHelper.getDocumentResource(doc);
                for (String word : list) {
                    Statement stmt = new StatementImpl(docResource,
                            WikiRelationConstants.HAS_LINK_TO, new LiteralImpl(word));
                    stmts.add(stmt);
                }
                graph.add(stmts);
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }

}

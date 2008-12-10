package org.nuxeo.ecm.wiki.relation;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.webengine.util.RelationConstants;
import org.nuxeo.ecm.webengine.util.RelationHelper;
import org.nuxeo.ecm.wiki.listener.WikiHelper;

public class WikiRelationHelper implements WikiRelationConstants{
    // this will update links graph
    // TODO optimize this!
    // keep old statements
    public static void updateRelations(DocumentModel doc) {
        List<String> list = WikiHelper.getWordLinks(doc);
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



}

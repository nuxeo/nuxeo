package org.nuxeo.ecm.wiki.relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.wiki.listener.WikiHelper;
import org.nuxeo.runtime.api.Framework;

public class RelationHelper {

    static RelationManager relationManager = null;

    public static RelationManager getRelationManager(){
        if ( relationManager == null) {
            try {
                relationManager = Framework.getService(RelationManager.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return relationManager;
    }


    /**
     * Returns the relation node corresponding to a document model
     */
    public static QNameResource getDocumentResource(DocumentModel document)
            throws ClientException {
        QNameResource documentResource = null;
        RelationManager rm = getRelationManager();
        if (document != null && rm != null) {
            documentResource = (QNameResource) rm.getResource(RelationConstants.DOCUMENT_NAMESPACE, document, null);
        }
        return documentResource;
    }

    /**
     * Returns the document model corresponding to a relation node
     */
    public static DocumentModel getDocumentModel(Node node, String coreSessionId)
            throws ClientException {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put(ResourceAdapter.CORE_SESSION_ID_CONTEXT_KEY,coreSessionId);
            Object o = relationManager.getResourceRepresentation(resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }


    public static DocumentModelList getDocumentsWithLinksTo(String pageName, String sessionId){
        try {
            Literal literal = new LiteralImpl(pageName);
            Statement pattern = new StatementImpl(null,RelationConstants.HAS_LINK_TO, literal );
            List<Statement> stmts = relationManager.getStatements(RelationConstants.GRAPH_NAME, pattern);
            if ( stmts != null ){
                DocumentModelList docs = new DocumentModelListImpl();
                DocumentModel d;
                for( Statement stmt : stmts) {
                    d = getDocumentModel(stmt.getSubject(), sessionId);
                    if ( d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static DocumentModelList getDocumentsWithLinksTo(DocumentModel doc){
        try {
            QNameResource docResource = getDocumentResource(doc);
            Statement pattern = new StatementImpl(null,RelationConstants.HAS_LINK_TO, docResource);
            List<Statement> stmts = relationManager.getStatements(RelationConstants.GRAPH_NAME, pattern);
            if ( stmts != null ){
                DocumentModelList docs = new DocumentModelListImpl();
                DocumentModel d;
                for( Statement stmt : stmts) {
                    d = getDocumentModel(stmt.getSubject(), doc.getSessionId());
                    if ( d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DocumentModelList getDocumentsWithLinksFrom(DocumentModel doc){
        try {
            List<Statement> stmts = getStatementsWithLinksFrom(doc);
            if ( stmts != null ){
                DocumentModelList docs = new DocumentModelListImpl();
                DocumentModel d;
                for( Statement stmt : stmts) {
                    d = getDocumentModel(stmt.getObject(), doc.getSessionId());
                    if ( d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Statement> getStatementsWithLinksFrom(DocumentModel doc){
        try {
            QNameResource docResource = getDocumentResource(doc);
            Statement pattern = new StatementImpl(docResource,RelationConstants.HAS_LINK_TO, null);
            List<Statement> stmts = relationManager.getStatements(RelationConstants.GRAPH_NAME, pattern);
            return stmts;
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    // this will update links graph
    // TODO optimize this!
    // keep old statements
    public static void updateRelations(DocumentModel doc) {
        List<String> list = WikiHelper.getWordLinks(doc);
        List<Statement> stmts = getStatementsWithLinksFrom(doc);
        try {
            // remove old links
            RelationManager rm = getRelationManager();
            if ( stmts != null) {
                rm.remove(RelationConstants.GRAPH_NAME, stmts);
                stmts.clear();
            } else {
                stmts = new ArrayList<Statement>();
            }

            // add new links
            if ( list != null ){
                QNameResource docResource = getDocumentResource(doc);
                for ( String word : list) {
                    Statement stmt = new StatementImpl(docResource, RelationConstants.HAS_LINK_TO, new LiteralImpl(word));
                    stmts.add(stmt);
                }
                rm.add(RelationConstants.GRAPH_NAME, stmts);
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }


}

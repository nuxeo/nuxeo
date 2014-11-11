/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.io.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Will help create relations between given documents.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class DummyRelationsHelper {

    public static final String DOCUMENT_NAMESPACE = "http://www.nuxeo.org/document/uid/";

    private static final Log log = LogFactory.getLog(DummyRelationsHelper.class);

    private static RelationManager relationManager;

    public static DocumentModel createSampleComment(
            CoreSession documentManager, DocumentModel docModel)
            throws Exception {
        DocumentModel myComment = documentManager.createDocumentModel("Comment");

        myComment.setProperty("comment", "author", "Autor");
        myComment.setProperty("comment", "text", "sample text");
        myComment.setProperty("comment", "creationDate", new Date());

        return createComment(docModel, myComment);
    }

    public static DocumentModel createComment(DocumentModel docModel,
            DocumentModel comment) throws Exception {
        return getCommentManager().createComment(docModel, comment);
    }

    public static CommentManager getCommentManager() throws Exception {
        return Framework.getService(CommentManager.class);
    }

    public static void createRelation(DocumentModel doc1, DocumentModel doc2)
            throws Exception {

        relationManager = Framework.getService(RelationManager.class);

        if (relationManager == null) {
            log.error("Unable to get Relations service");
            return;
        }

        // create ressources based on the documents
        Resource docAResource = getDocumentResource(doc1);
        Resource docBResource = getDocumentResource(doc2);

        // create new relation
        Statement newRelation = new StatementImpl(docAResource,
                new ResourceImpl("http://depends-on"), docBResource);
        List<Statement> statementsToAdd = new ArrayList<Statement>();
        statementsToAdd.add(newRelation);
        relationManager.add("default", statementsToAdd);

        // search for incomming relations in docB
        Statement pattern = new StatementImpl(null, null, docBResource);
        List<Statement> incomingStatements = relationManager.getStatements(
                "default", pattern);
        for (Statement st : incomingStatements) {
            log.info("Relations on B :" + st.getPredicate().toString());
        }

        // search for outgoing relations in docA
        pattern = new StatementImpl(docAResource, null, null);
        List<Statement> outgoingStatements = relationManager.getStatements(
                "default", pattern);
        for (Statement st : outgoingStatements) {
            log.info("Relations on A :" + st.getPredicate().toString());
        }
    }

    public static void checkRelation() {
    }

    private static QNameResource getDocumentResource(DocumentModel document) throws Exception {
        QNameResource documentResource = null;
        if (document != null) {
            documentResource = (QNameResource) relationManager.getResource(
                    DOCUMENT_NAMESPACE, document, null);
        }
        return documentResource;
    }

}

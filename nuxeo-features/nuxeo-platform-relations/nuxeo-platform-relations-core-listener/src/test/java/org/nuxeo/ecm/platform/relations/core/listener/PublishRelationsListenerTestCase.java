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

package org.nuxeo.ecm.platform.relations.core.listener;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.api.Framework;

public class PublishRelationsListenerTestCase extends SQLRepositoryTestCase {

    protected static final Resource conformsTo = new ResourceImpl(
            "http://purl.org/dc/terms/ConformsTo");

    protected static final String COMMENTS_GRAPH_NAME = "documentComments";

    protected DocumentModel doc1;

    protected DocumentModel doc2;

    protected DocumentModel workspace;

    protected DocumentModel section;

    protected RelationManager relationManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.relations.api");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployBundle("org.nuxeo.ecm.relations.core.listener");
        deployBundle("org.nuxeo.ecm.platform.comment.api");
        deployBundle("org.nuxeo.ecm.platform.comment.core");
        deployContrib("org.nuxeo.ecm.platform.comment",
                "OSGI-INF/CommentService.xml");
        deployBundle("org.nuxeo.ecm.platform.relations.core.listener.tests");
        openSession();

        relationManager = Framework.getService(RelationManager.class);

        workspace = session.createDocumentModel("Folder");
        workspace.setProperty("dublincore", "title", "Workspace");
        workspace.setPathInfo("/", "workspace");
        workspace = session.createDocument(workspace);

        section = session.createDocumentModel("Folder");
        section.setProperty("dublincore", "title", "Section");
        section.setPathInfo("/", "section");
        section = session.createDocument(section);

        doc1 = session.createDocumentModel("File");
        doc1.setProperty("dublincore", "title", "Some file");
        doc1.setPathInfo("/workspace/", "file-1");
        doc1 = session.createDocument(doc1);

        doc2 = session.createDocumentModel("File");
        doc2.setProperty("dublincore", "title", "Some other file");
        doc2.setPathInfo("/workspace/", "file-2");
        doc2 = session.createDocument(doc2);

        session.save();
        closeSession();
        openSession();
    }

    protected void addSomeComments(DocumentModel docToComment) throws Exception {
        // Create a first commentary
        CommentableDocument cDoc = docToComment.getAdapter(CommentableDocument.class);
        DocumentModel comment = session.createDocumentModel("Comment");
        comment.setProperty("comment", "text", "This is my comment for "
                + docToComment.getTitle());
        comment.setProperty("comment", "author", "Me");
        comment = cDoc.addComment(comment);

        // Create a second commentary
        DocumentModel comment2 = session.createDocumentModel("Comment");
        comment2.setProperty("comment", "text", "This is another  comment for "
                + docToComment.getTitle());
        comment.setProperty("comment", "author", "the other author");
        comment2 = cDoc.addComment(comment);
    }

    protected void addSomeRelations(Resource documentResource)
            throws ClientException {
        Resource otherDocResource = relationManager.getResource(
                RelationConstants.DOCUMENT_NAMESPACE, doc2, null);

        List<Statement> originalStatments = new ArrayList<Statement>();
        originalStatments.add(new StatementImpl(documentResource, conformsTo,
                new LiteralImpl("some conformance")));
        originalStatments.add(new StatementImpl(otherDocResource, conformsTo,
                documentResource));
        relationManager.add(RelationConstants.GRAPH_NAME, originalStatments);
    }

    public void testCopyRelationsFromWork() throws Exception {
        // add relations of type comment
        addSomeComments(doc1);

        // add some real document relations (like those from the Relations tab
        // in DM
        Resource docResource = relationManager.getResource(
                RelationConstants.DOCUMENT_NAMESPACE, doc1, null);
        addSomeRelations(docResource);

        // publish the document
        DocumentModel publishedProxy = session.publishDocument(doc1, section);
        session.save();
        closeSession();

        // read the relations carried by the proxy, comments should not be
        // copied on the proxy, just ordinary relations
        Resource publishedResource = relationManager.getResource(
                RelationConstants.DOCUMENT_NAMESPACE, publishedProxy, null);
        List<Statement> statements = relationManager.getStatements(
                RelationConstants.GRAPH_NAME, new StatementImpl(
                        publishedResource, null, null));
        assertNotNull(statements);
        assertEquals(1, statements.size());

        statements = relationManager.getStatements(
                RelationConstants.GRAPH_NAME, new StatementImpl(null, null,
                        publishedResource));
        assertNotNull(statements);
        assertEquals(1, statements.size());

        // no comments where copied
        List<Statement> comments = relationManager.getStatements(
                COMMENTS_GRAPH_NAME, new StatementImpl(null, null,
                        publishedResource));
        assertNotNull(comments);
        assertEquals(0, comments.size());
    }

    public void testKeepCommentsAndRelationsFromOldProxy() throws Exception {
        // add relations of type comment on the original document
        addSomeComments(doc1);

        // publish it
        DocumentModel publishedProxy = session.publishDocument(doc1, section);
        session.save();

        // add some comments on the proxy
        addSomeComments(publishedProxy);

        // add some real document relations (like those from the Relations tab
        // in DM
        Resource publishedResource = relationManager.getResource(
                RelationConstants.DOCUMENT_NAMESPACE, publishedProxy, null);
        addSomeRelations(publishedResource);

        // publish again
        publishedProxy = session.publishDocument(doc1, section);
        session.save();

        // check that the old relations are still there
        List<Statement> statements = relationManager.getStatements(
                RelationConstants.GRAPH_NAME, new StatementImpl(
                        publishedResource, null, null));
        assertNotNull(statements);
        assertEquals(1, statements.size());

        statements = relationManager.getStatements(
                RelationConstants.GRAPH_NAME, new StatementImpl(null, null,
                        publishedResource));
        assertNotNull(statements);
        assertEquals(1, statements.size());

        // previous comments are still there, but not the comments from the
        // source document
        List<Statement> comments = relationManager.getStatements(
                COMMENTS_GRAPH_NAME, new StatementImpl(null, null,
                        publishedResource));
        assertNotNull(comments);
        assertEquals(2, comments.size());
    }

}

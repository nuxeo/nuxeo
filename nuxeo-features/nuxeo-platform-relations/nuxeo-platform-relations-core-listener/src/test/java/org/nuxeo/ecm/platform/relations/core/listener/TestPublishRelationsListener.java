/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.relations.core.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.relations.api", //
        "org.nuxeo.ecm.relations", //
        "org.nuxeo.ecm.relations.jena", //
        "org.nuxeo.ecm.relations.core.listener", //
        "org.nuxeo.ecm.platform.comment.api", //
        "org.nuxeo.ecm.platform.comment", //
        "org.nuxeo.ecm.platform.relations.core.listener.tests", //
})
public class TestPublishRelationsListener {

    protected static final Resource conformsTo = new ResourceImpl("http://purl.org/dc/terms/ConformsTo");

    protected static final String COMMENTS_GRAPH_NAME = "documentComments";

    protected static final String DOCUMENT_NAMESPACE_NOSLASH = "http://www.nuxeo.org/document/uid";

    protected DocumentModel doc1;

    protected DocumentModel doc2;

    protected DocumentModel workspace;

    protected DocumentModel section;

    @Inject
    protected RelationManager relationManager;

    @Inject
    protected CoreSession session;

    @Before
    public void setUp() throws Exception {

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
    }

    protected void addSomeComments(DocumentModel docToComment) throws Exception {
        // Create a first commentary
        CommentableDocument cDoc = docToComment.getAdapter(CommentableDocument.class);
        DocumentModel comment = session.createDocumentModel("Comment");
        comment.setProperty("comment", "text", "This is my comment for " + docToComment.getTitle());
        comment.setProperty("comment", "author", "Me");
        comment = cDoc.addComment(comment);

        // Create a second commentary
        DocumentModel comment2 = session.createDocumentModel("Comment");
        comment2.setProperty("comment", "text", "This is another  comment for " + docToComment.getTitle());
        comment2.setProperty("comment", "author", "the other author");
        comment2 = cDoc.addComment(comment2);
    }

    protected void addSomeRelations(Resource documentResource) {
        Resource otherDocResource = relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE, doc2, null);

        List<Statement> originalStatements = new ArrayList<Statement>();
        originalStatements.add(new StatementImpl(documentResource, conformsTo, new LiteralImpl("some conformance")));
        originalStatements.add(new StatementImpl(otherDocResource, conformsTo, documentResource));
        relationManager.getGraphByName(RelationConstants.GRAPH_NAME).add(originalStatements);
    }

    @Test
    public void testCopyRelationsFromWork() throws Exception {
        // add relations of type comment
        addSomeComments(doc1);

        // add some real document relations (like those from the Relations tab
        // in DM
        Resource docResource = relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE, doc1, null);
        addSomeRelations(docResource);

        // publish the document
        DocumentModel publishedProxy = session.publishDocument(doc1, section);
        session.save();

        // read the relations carried by the proxy, comments should not be
        // copied on the proxy, just ordinary relations
        Resource publishedResource = relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE, publishedProxy,
                null);
        Graph graph = relationManager.getGraphByName(RelationConstants.GRAPH_NAME);
        List<Statement> statements = graph.getStatements(new StatementImpl(publishedResource, null, null));
        assertNotNull(statements);
        assertEquals(1, statements.size());

        statements = graph.getStatements(new StatementImpl(null, null, publishedResource));
        assertNotNull(statements);
        assertEquals(1, statements.size());

        // no comments where copied
        List<Statement> comments = relationManager.getGraphByName(COMMENTS_GRAPH_NAME).getStatements(
                new StatementImpl(null, null, publishedResource));
        assertNotNull(comments);
        assertEquals(0, comments.size());
    }

    @Test
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
        Resource publishedResource = relationManager.getResource(DOCUMENT_NAMESPACE_NOSLASH, publishedProxy, null);
        addSomeRelations(publishedResource);

        // publish again
        publishedProxy = session.publishDocument(doc1, section);
        session.save();

        // check that the old relations are still there
        Graph defaultGraph = relationManager.getGraphByName(RelationConstants.GRAPH_NAME);
        List<Statement> statements = defaultGraph.getStatements(publishedResource, null, null);
        assertNotNull(statements);
        assertEquals(1, statements.size());

        statements = defaultGraph.getStatements(null, null, publishedResource);
        assertNotNull(statements);
        assertEquals(1, statements.size());

        // previous comments are still there, but not the comments from the
        // source document
        Graph commentGraph = relationManager.getGraphByName(COMMENTS_GRAPH_NAME);
        List<Statement> comments = commentGraph.getStatements(null, null, publishedResource);
        assertNotNull(comments);
        assertEquals(2, comments.size());
    }

    @Test
    public void testDeleteRelationsOnOriginalDoc() throws Exception {
        DocumentModel docToDelete = createDocTestDoc("docToDelete");
        // add some real document relations (like those from the Relations tab
        // in DM between 2 resources :

        Resource docToDeleteResource = relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE, docToDelete,
                null);
        Resource docResource2 = relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE, doc2, null);
        addSomeRelations(docToDeleteResource);

        // now check that the relations were added

        List<Statement> statementsOnDeleted = getRelations(docResource2, docToDeleteResource);
        assertEquals(1, statementsOnDeleted.size());

        // publish the document
        DocumentModel publishedProxy = session.publishDocument(docToDelete, section);
        session.save();
        Resource publishedResource = relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE, publishedProxy,
                null);

        // now delete the document and check that the relations on the original
        // doc are deleted
        session.removeDocument(docToDelete.getRef());
        session.save();

        // check that relations are still there on the published doc after the
        // doc was deleted
        List<Statement> statementsOnPublishedResource = getRelations(publishedResource, null);
        assertNotNull(statementsOnPublishedResource);
        assertEquals(2, statementsOnPublishedResource.size());

        // check that all relations the deleted docs was part of are deleted

        statementsOnDeleted = getRelations(docResource2, docToDeleteResource);
        assertEquals(0, statementsOnDeleted.size());

    }

    private DocumentModel createDocTestDoc(String title) {
        DocumentModel doc = session.createDocumentModel("File");
        doc.setPropertyValue("dc:title", title);
        doc.setPathInfo("/workspace/", "file-1");
        doc = session.createDocument(doc1);
        session.save();
        return doc;
    }

    private List<Statement> getRelations(Resource resource1, Resource resource2) {
        Graph graph = relationManager.getGraphByName(RelationConstants.GRAPH_NAME);
        List<Statement> statements = graph.getStatements(new StatementImpl(resource1, null, resource2));
        if (statements != null) {
            statements.addAll(graph.getStatements(new StatementImpl(resource2, null, resource1)));
        }
        return statements;

    }

}

package org.nuxeo.ecm.platform.relations.core.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.jms.AsyncProcessorConfig;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.api.Framework;

public class PublishRelationsListenerTestCase extends SQLRepositoryTestCase {

    protected DocumentModel doc;

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
        deployContrib("org.nuxeo.ecm.platform.comment",
                "OSGI-INF/comment-listener-contrib.xml");
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

        doc = session.createDocumentModel("File");
        doc.setProperty("dublincore", "title", "Some file");
        doc.setPathInfo("/workspace/", "file-1");

        doc = session.createDocument(doc);
        session.save();
        AsyncProcessorConfig.setForceJMSUsage(false);
        closeSession();
        openSession();
    }

    protected void addSomeComments(DocumentModel docToComment) throws Exception {
        // Create a first commentary
        CommentableDocument cDoc = docToComment.getAdapter(CommentableDocument.class);
        DocumentModel comment = session.createDocumentModel("Comment");
        comment.setProperty("comment", "text", "This is my comment");
        comment = cDoc.addComment(comment);

        // Create a second commentary
        DocumentModel comment2 = session.createDocumentModel("Comment");
        comment2.setProperty("comment", "text", "This is another  comment");
        comment2 = cDoc.addComment(comment);
    }

    public void testCopyRelationsFromWork() throws Exception {
        // add relations of type comment
        //addSomeComments(doc);

        // add some real document relations (like those from the Relations tab
        // in DM
        Resource docResource = relationManager.getResource(
                RelationConstants.DOCUMENT_NAMESPACE, doc, null);

        List<Statement> originalStatments = new ArrayList<Statement>();
        originalStatments.add(new StatementImpl(docResource, new ResourceImpl(
                "urn:someproperty"), new ResourceImpl("urn:somevalue")));
        originalStatments.add(new StatementImpl(new ResourceImpl(
                "urn:someresource"), new ResourceImpl("urn:someproperty"),
                docResource));
        relationManager.add(RelationConstants.GRAPH_NAME, originalStatments);

        // publish the document
        DocumentModel publishedProxy = session.publishDocument(doc, section);

        // read the relations carried by the proxy
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
    }

    public void testKeepRelationsFromOldProxy() throws Exception {
        // TODO
    }

    public void testKeepCommentsFromOldProxy() throws Exception {
        // TODO
    }

}

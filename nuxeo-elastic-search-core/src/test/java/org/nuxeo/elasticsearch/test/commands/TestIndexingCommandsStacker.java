package org.nuxeo.elasticsearch.test.commands;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommands;
import org.nuxeo.elasticsearch.commands.IndexingCommandsStacker;

/**
 *
 * Test that the logic for transforming CoreEvents on ElasticSearch commands
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class TestIndexingCommandsStacker extends IndexingCommandsStacker {

    Map<String, IndexingCommands> commands  = new HashMap<String, IndexingCommands>();

    @Override
    protected Map<String, IndexingCommands> getAllCommands() {
        return commands;
    }


    public final class MockDocumentModel extends DocumentModelImpl {

        protected String uid;
        protected boolean folder = false;

        public MockDocumentModel( String uid) {
            super();
            this.uid = uid;

        }

        @Override
        public String getId() {
            return uid;
        }

        @Override
        public boolean isFolder() {
            return folder;
        }

    }

    @Test
    public void shouldStoreRemoveDuplicatedEvents() {

        DocumentModel doc1 = new MockDocumentModel( "1");
        DocumentModel doc2 = new MockDocumentModel( "2");
        DocumentModel doc3 = new MockDocumentModel( "3");
        DocumentModel doc4 = new MockDocumentModel( "4");

        stackCommand(doc1, DocumentEventTypes.DOCUMENT_CREATED, false);
        stackCommand(doc1, DocumentEventTypes.DOCUMENT_UPDATED, false);

        stackCommand(doc2, DocumentEventTypes.BEFORE_DOC_UPDATE, false);
        stackCommand(doc2, DocumentEventTypes.BEFORE_DOC_UPDATE, false);
        stackCommand(doc2, DocumentEventTypes.BEFORE_DOC_UPDATE, false);

        stackCommand(doc3, DocumentEventTypes.DOCUMENT_CREATED, false);
        stackCommand(doc3, DocumentEventTypes.BEFORE_DOC_UPDATE, false);
        stackCommand(doc3, DocumentEventTypes.DOCUMENT_REMOVED, false);

        Assert.assertEquals(3, commands.size());

        IndexingCommands ic1 = getCommands(doc1);
        Assert.assertEquals(1, ic1.getMergedCommands().size());
        ic1.contains(IndexingCommand.INDEX);

        IndexingCommands ic2 = getCommands(doc2);
        Assert.assertEquals(1, ic2.getMergedCommands().size());
        ic2.contains(IndexingCommand.UPDATE);

        IndexingCommands ic3 = getCommands(doc3);
        Assert.assertEquals(0, ic3.getMergedCommands().size());

    }

}

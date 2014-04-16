package org.nuxeo.elasticsearch.test.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommands;
import org.nuxeo.elasticsearch.commands.IndexingCommandsStacker;

/**
 *
 * Test that the logic for transforming CoreEvents in ElasticSearch commands
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class TestIndexingCommandsStacker extends IndexingCommandsStacker {

    protected Map<String, IndexingCommands> commands = new HashMap<String, IndexingCommands>();

    protected List<IndexingCommand> flushedSyncCommands;

    protected List<IndexingCommand> flushedAsyncCommands;

    @Override
    protected Map<String, IndexingCommands> getAllCommands() {
        return commands;
    }

    @Before
    public void reset() {
        flushedSyncCommands = new ArrayList<>();
        flushedAsyncCommands = new ArrayList<>();
    }

    @Override
    protected void fireSyncIndexing(List<IndexingCommand> syncCommands)
            throws ClientException {
        flushedSyncCommands.addAll(syncCommands);
    }

    @Override
    protected void fireAsyncIndexing(List<IndexingCommand> asyncCommands)
            throws ClientException {
        flushedAsyncCommands.addAll(asyncCommands);
    }

    public final class MockDocumentModel extends DocumentModelImpl {

        private static final long serialVersionUID = 1L;

        protected String uid;

        protected boolean folder = false;

        public MockDocumentModel(String uid) {
            this(uid, false);
        }

        public MockDocumentModel(String uid, boolean folder) {
            super();
            this.uid = uid;
            this.folder = folder;
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
    public void shouldRemoveDuplicatedEvents() throws Exception {

        DocumentModel doc1 = new MockDocumentModel("1");
        DocumentModel doc2 = new MockDocumentModel("2");
        DocumentModel doc3 = new MockDocumentModel("3");

        stackCommand(doc1, DocumentEventTypes.DOCUMENT_CREATED, false);
        stackCommand(doc1, DocumentEventTypes.BEFORE_DOC_UPDATE, false);

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
        Assert.assertEquals(IndexingCommand.INDEX,
                ic1.getMergedCommands().get(0).getName());

        IndexingCommands ic2 = getCommands(doc2);
        Assert.assertEquals(1, ic2.getMergedCommands().size());
        ic2.contains(IndexingCommand.UPDATE);
        Assert.assertEquals(IndexingCommand.UPDATE,
                ic2.getMergedCommands().get(0).getName());

        IndexingCommands ic3 = getCommands(doc3);
        Assert.assertEquals(0, ic3.getMergedCommands().size());

        flushCommands();
        Assert.assertEquals(0, flushedSyncCommands.size());
        Assert.assertEquals(2, flushedAsyncCommands.size());

    }

    @Test
    public void shouldMergeDuplicatedEventsAndSwitchToSync() throws Exception {

        DocumentModel doc1 = new MockDocumentModel("1");
        DocumentModel doc2 = new MockDocumentModel("2");

        stackCommand(doc1, DocumentEventTypes.BEFORE_DOC_UPDATE, false);
        stackCommand(doc1, DocumentEventTypes.BEFORE_DOC_UPDATE, true);

        stackCommand(doc2, DocumentEventTypes.DOCUMENT_CREATED, false);
        stackCommand(doc2, DocumentEventTypes.BEFORE_DOC_UPDATE, false);
        stackCommand(doc2, DocumentEventTypes.BEFORE_DOC_UPDATE, true);

        Assert.assertEquals(2, commands.size());

        IndexingCommands ic1 = getCommands(doc1);
        Assert.assertEquals(1, ic1.getMergedCommands().size());
        ic1.contains(IndexingCommand.UPDATE);
        Assert.assertEquals(IndexingCommand.UPDATE,
                ic1.getMergedCommands().get(0).getName());
        Assert.assertTrue(ic1.getMergedCommands().get(0).isSync());

        IndexingCommands ic2 = getCommands(doc2);
        Assert.assertEquals(1, ic2.getMergedCommands().size());
        ic2.contains(IndexingCommand.INDEX);
        Assert.assertEquals(IndexingCommand.INDEX,
                ic2.getMergedCommands().get(0).getName());
        Assert.assertTrue(ic2.getMergedCommands().get(0).isSync());

        flushCommands();
        Assert.assertEquals(2, flushedSyncCommands.size());
        Assert.assertEquals(0, flushedAsyncCommands.size());

    }

    @Test
    public void shouldRecurseReindex() throws Exception {

        DocumentModel doc1 = new MockDocumentModel("1", true);
        DocumentModel doc2 = new MockDocumentModel("2", true);

        stackCommand(doc1, DocumentEventTypes.DOCUMENT_MOVED, false);

        stackCommand(doc2, DocumentEventTypes.DOCUMENT_SECURITY_UPDATED, false);

        IndexingCommands ic1 = getCommands(doc1);
        Assert.assertEquals(1, ic1.getMergedCommands().size());
        ic1.contains(IndexingCommand.UPDATE);
        Assert.assertEquals(IndexingCommand.UPDATE,
                ic1.getMergedCommands().get(0).getName());
        Assert.assertTrue(ic1.getMergedCommands().get(0).isRecurse());

        IndexingCommands ic2 = getCommands(doc2);
        Assert.assertEquals(1, ic2.getMergedCommands().size());
        ic2.contains(IndexingCommand.UPDATE_SECURITY);
        Assert.assertEquals(IndexingCommand.UPDATE_SECURITY,
                ic2.getMergedCommands().get(0).getName());
        Assert.assertTrue(ic2.getMergedCommands().get(0).isRecurse());

        flushCommands();
        Assert.assertEquals(0, flushedSyncCommands.size());
        Assert.assertEquals(2, flushedAsyncCommands.size());

    }

}

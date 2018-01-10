/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.elasticsearch.test.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.elasticsearch.commands.IndexingCommands;
import org.nuxeo.elasticsearch.commands.IndexingCommandsStacker;

/**
 * Test that the logic for transforming CoreEvents in ElasticSearch commands
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class TestIndexingCommandsStacker extends IndexingCommandsStacker {

    protected Map<String, IndexingCommands> commands = new HashMap<>();

    protected List<IndexingCommand> flushedSyncCommands;

    protected List<IndexingCommand> flushedAsyncCommands;

    @Override
    protected Map<String, IndexingCommands> getAllCommands() {
        return commands;
    }

    @Override
    protected boolean isSyncIndexingByDefault() {
        return false;
    }

    @Before
    public void reset() {
        flushedSyncCommands = new ArrayList<>();
        flushedAsyncCommands = new ArrayList<>();
    }

    protected void fireSyncIndexing(List<IndexingCommand> syncCommands) {
        flushedSyncCommands.addAll(syncCommands);
    }

    protected void fireAsyncIndexing(List<IndexingCommand> asyncCommands) {
        flushedAsyncCommands.addAll(asyncCommands);
    }

    protected void flushCommands() {
        Map<String, IndexingCommands> allCmds = getAllCommands();

        List<IndexingCommand> syncCommands = new ArrayList<>();
        List<IndexingCommand> asyncCommands = new ArrayList<>();

        for (IndexingCommands cmds : allCmds.values()) {
            for (IndexingCommand cmd : cmds.getCommands()) {
                if (cmd.isSync()) {
                    syncCommands.add(cmd);
                } else {
                    asyncCommands.add(cmd);
                }
            }
        }
        getAllCommands().clear();

        if (syncCommands.size() > 0) {
            fireSyncIndexing(syncCommands);
        }
        if (asyncCommands.size() > 0) {
            fireAsyncIndexing(asyncCommands);
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
        Assert.assertEquals(1, ic1.getCommands().size());
        Assert.assertTrue(ic1.contains(Type.INSERT));
        Assert.assertEquals(Type.INSERT, ic1.getCommands().get(0).getType());

        IndexingCommands ic2 = getCommands(doc2);
        Assert.assertEquals(1, ic2.getCommands().size());
        Assert.assertTrue(ic2.contains(Type.UPDATE));
        Assert.assertEquals(Type.UPDATE, ic2.getCommands().get(0).getType());

        IndexingCommands ic3 = getCommands(doc3);
        Assert.assertEquals(0, ic3.getCommands().size());

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
        Assert.assertEquals(1, ic1.getCommands().size());
        Assert.assertTrue(ic1.contains(Type.UPDATE));
        Assert.assertEquals(Type.UPDATE, ic1.getCommands().get(0).getType());
        Assert.assertTrue(ic1.getCommands().get(0).isSync());

        IndexingCommands ic2 = getCommands(doc2);
        Assert.assertEquals(1, ic2.getCommands().size());
        Assert.assertTrue(ic2.contains(Type.INSERT));
        Assert.assertEquals(Type.INSERT, ic2.getCommands().get(0).getType());
        Assert.assertTrue(ic2.getCommands().get(0).isSync());

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
        Assert.assertEquals(1, ic1.getCommands().size());
        Assert.assertTrue(ic1.contains(Type.UPDATE));
        Assert.assertEquals(Type.UPDATE, ic1.getCommands().get(0).getType());
        Assert.assertTrue(ic1.getCommands().get(0).isRecurse());

        IndexingCommands ic2 = getCommands(doc2);
        Assert.assertEquals(1, ic2.getCommands().size());
        Assert.assertTrue(ic2.contains(Type.UPDATE_SECURITY));
        Assert.assertEquals(Type.UPDATE_SECURITY, ic2.getCommands().get(0).getType());
        Assert.assertTrue(ic2.getCommands().get(0).isRecurse());

        flushCommands();
        Assert.assertEquals(0, flushedSyncCommands.size());
        Assert.assertEquals(2, flushedAsyncCommands.size());

    }

    @Test
    public void shouldRecurseReindexInSync() throws Exception {
        DocumentModel doc1 = new MockDocumentModel("1", true);

        stackCommand(doc1, DocumentEventTypes.DOCUMENT_MOVED, true);

        IndexingCommands ic1 = getCommands(doc1);
        // We should have 2 commands 1 sync + 1 async and recursive
        Assert.assertEquals(2, ic1.getCommands().size());
        Assert.assertTrue(ic1.contains(Type.UPDATE));
        Assert.assertEquals(Type.UPDATE, ic1.getCommands().get(0).getType());
        Assert.assertFalse(ic1.getCommands().get(0).isRecurse());
        Assert.assertTrue(ic1.getCommands().get(1).isRecurse());

        flushCommands();
        Assert.assertEquals(1, flushedSyncCommands.size());
        Assert.assertEquals(1, flushedAsyncCommands.size());
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
}

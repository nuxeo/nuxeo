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
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.test.commands;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;

public class TestIndexingCommand {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testConstructorOk() throws Exception {
        DocumentModel doc = new MockDocumentModel("foo");
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, false);
        cmd = new IndexingCommand(doc, Type.INSERT, true, false);
        Assert.assertTrue(cmd.isSync());
        Assert.assertFalse(cmd.isRecurse());
        cmd = new IndexingCommand(doc, Type.INSERT, false, true);
        // delete recurse and sync is accepted
        cmd = new IndexingCommand(doc, Type.DELETE, true, true);
        Assert.assertTrue(cmd.isSync());
        Assert.assertTrue(cmd.isRecurse());

    }

    @Test
    public void testConstructorWithRecurseSync() throws Exception {
        DocumentModel doc = new MockDocumentModel("foo");
        exception.expect(IllegalArgumentException.class);
        new IndexingCommand(doc, Type.INSERT, true, true);
    }

    @Test
    public void testConstructorWithNullDoc() throws Exception {
        exception.expect(IllegalArgumentException.class);
        new IndexingCommand(null, Type.INSERT, true, false);
    }

    @Test
    public void testConstructorWithNullDocId() throws Exception {
        DocumentModel doc = new MockDocumentModel(null);
        exception.expect(IllegalArgumentException.class);
        new IndexingCommand(doc, Type.INSERT, true, false);
    }

    @Test
    public void testAddSchemas() throws Exception {
        DocumentModel doc = new MockDocumentModel("foo");
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, true, false);
        Assert.assertNull(cmd.getSchemas());
        cmd.addSchemas("mySchema");
        Assert.assertEquals(1, cmd.getSchemas().length);
    }

    @Test
    public void testMakeSync() throws Exception {
        DocumentModel doc = new MockDocumentModel("foo");
        // ok for non recursive command
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, false);
        cmd.makeSync();
        Assert.assertTrue(cmd.isSync());
        // recursive command cannot be turned into sync
        cmd = new IndexingCommand(doc, Type.INSERT, false, true);
        cmd.makeSync();
        Assert.assertFalse(cmd.isSync());
        // except for deletion
        cmd = new IndexingCommand(doc, Type.DELETE, false, true);
        cmd.makeSync();
        Assert.assertTrue(cmd.isSync());
    }

    @Test
    public void testJson() throws Exception {
        DocumentModel doc = new MockDocumentModel("foo");
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, true);
        String json = cmd.toJSON();
        IndexingCommand cmd2 = IndexingCommand.fromJSON(json);
        String json2 = cmd2.toJSON();
        Assert.assertEquals(json, json2);
        Assert.assertTrue(cmd2.isRecurse());
    }

    @Test
    public void testInvalidJson() throws Exception {
        DocumentModel doc = new MockDocumentModel("foo");
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, true);
        exception.expect(IllegalArgumentException.class);
        String json = "{" + cmd.toJSON();
        IndexingCommand.fromJSON(json);
    }

    @Test
    public void testInvalidJsonDocIdNull() throws Exception {
        String json = "{\"id\": \"124\", \"type\": \"INSERT\"}";
        exception.expect(IllegalArgumentException.class);
        IndexingCommand cmd = IndexingCommand.fromJSON(json);
    }

    public final class MockDocumentModel extends DocumentModelImpl {
        private static final long serialVersionUID = 1L;

        protected String uid;

        public MockDocumentModel(String uid) {
            this.uid = uid;
        }

        @Override
        public String getId() {
            return uid;
        }

        @Override
        public String toString() {
            return String.format("MockDoc(uid=%s)", uid);
        }
    }

}

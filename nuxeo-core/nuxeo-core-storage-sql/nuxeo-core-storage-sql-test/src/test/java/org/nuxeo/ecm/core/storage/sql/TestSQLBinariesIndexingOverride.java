/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestSQLBinariesIndexingOverride extends TXSQLRepositoryTestCase {

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                "OSGI-INF/test-override-indexing-contrib.xml");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
    }

    @Test
    public void testTwoBinaryIndexes() throws Exception {
        DocumentModelList res;
        DocumentModel doc = session.createDocumentModel("/", "source", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("test"));
        doc = session.createDocument(doc);
        session.save();
        closeSession();
        TransactionHelper.commitOrRollbackTransaction();
        waitForFulltextIndexing();
        TransactionHelper.startTransaction();
        openSession();

        // main index
        res = session.query("SELECT * FROM Document WHERE ecm:fulltext = 'test'");
        assertEquals(1, res.size());

        // other index
        res = session.query("SELECT * FROM Document WHERE ecm:fulltext_binaries = 'test'");
        assertEquals(1, res.size());
    }

    @Test
    public void testGetBinaryFulltext() throws Exception {
        DocumentModelList res;
        DocumentModel doc = session.createDocumentModel("/", "source", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("test"));
        doc = session.createDocument(doc);
        session.save();
        closeSession();
        TransactionHelper.commitOrRollbackTransaction();
        waitForFulltextIndexing();
        TransactionHelper.startTransaction();
        openSession();
        // main index
        res = session.query("SELECT * FROM Document WHERE ecm:fulltext = 'test'");
        assertEquals(1, res.size());
        Map<String, String> map = session.getBinaryFulltext(res.get(0).getRef());
        // we have 2 binaries field
        assertTrue(map.containsKey("binarytext"));
        assertTrue(map.containsKey("binarytext_binaries"));
        assertEquals("test", map.get("binarytext"));
        assertEquals("test", map.get("binarytext_binaries"));
    }

}

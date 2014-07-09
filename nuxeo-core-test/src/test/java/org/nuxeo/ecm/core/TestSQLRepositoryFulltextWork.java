/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.storage.sql.TXSQLRepositoryTestCase;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests fulltext extractor work and updater work.
 */
public class TestSQLRepositoryFulltextWork extends TXSQLRepositoryTestCase {

    protected static final Log log = LogFactory.getLog(TestSQLRepositoryFulltextWork.class);

    protected void nextTX() throws ClientException {
        closeSession();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        openSession();
    }

    private void createFolder() throws PropertyException, ClientException {
        DocumentModel folder = new DocumentModelImpl("/", "testfolder",
                "Folder");
        folder.setPropertyValue("dc:title", "folder Title");
        folder = session.createDocument(folder);
    }

    @Test
    public void testFulltext() throws Exception {
        createFolder();
        createAndDeleteFile("testfile");
    }

    private void createAndDeleteFile(String name) throws PropertyException,
            ClientException {
        DocumentModel file = new DocumentModelImpl("/testfolder", name, "File");
        file.setPropertyValue("dc:title", "testfile Title");
        file = session.createDocument(file);
        session.save();
        nextTX();

        // at this point fulltext update is triggered async

        session.removeDocument(new IdRef(file.getId()));
        session.save();
        nextTX();

        waitForAsyncCompletion();
    }

    @Test
    public void testFulltextWithConcurrentDelete() throws Exception {
        createFolder();
        for (int i = 0; i < 50; i++) {
            createAndDeleteFile("testfile" + i);
        }
    }

}

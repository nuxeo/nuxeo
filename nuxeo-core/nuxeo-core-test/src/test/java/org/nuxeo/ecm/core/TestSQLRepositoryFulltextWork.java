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

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests fulltext extractor work and updater work.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestSQLRepositoryFulltextWork {

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected void waitForAsyncCompletion() {
        nextTransaction();
        eventService.waitForAsyncCompletion();
    }

    private void createFolder() throws PropertyException {
        DocumentModel folder = session.createDocumentModel("/", "testfolder", "Folder");
        folder.setPropertyValue("dc:title", "folder Title");
        folder = session.createDocument(folder);
    }

    @Test
    public void testFulltext() throws Exception {
        createFolder();
        createAndDeleteFile("testfile");
    }

    private void createAndDeleteFile(String name) throws PropertyException {
        DocumentModel file = session.createDocumentModel("/testfolder", name, "File");
        file.setPropertyValue("dc:title", "testfile Title");
        file = session.createDocument(file);
        session.save();
        nextTransaction();

        // at this point fulltext update is triggered async

        session.removeDocument(new IdRef(file.getId()));
        session.save();

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

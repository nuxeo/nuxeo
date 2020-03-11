/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RandomBug;
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
    @RandomBug.Repeat(issue = "NXP-28711: randomly failing in postgresql mode", onFailure = 10, onSuccess = 30)
    public void testFulltextWithConcurrentDelete() throws Exception {
        createFolder();
        for (int i = 0; i < 50; i++) {
            createAndDeleteFile("testfile" + i);
        }
    }

}

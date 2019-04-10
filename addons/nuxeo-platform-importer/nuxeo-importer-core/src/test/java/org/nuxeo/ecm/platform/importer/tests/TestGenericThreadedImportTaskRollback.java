/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import javax.inject.Inject;
import javax.transaction.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.base.GenericThreadedImportTask;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestGenericThreadedImportTaskRollback {

    @Inject
    protected CoreSession coreSession;

    /**
     * Test that we properly rollback the transaction when an exception occurs during GenericThreadedImportTask
     * processing.
     */
    @Test
    public void testTaskRollback() throws Exception {
        // we have no document named "/doc" yet
        assertFalse(coreSession.exists(new PathRef("/doc")));
        // get out of the transaction
        TransactionHelper.commitOrRollbackTransaction();

        // prepare a pseudo-task that creates /doc then throws an exception
        Runnable task = new MyTask(coreSession.getRepositoryName());
        task.run();

        // check that the task run rolled back its transaction on error
        assertEquals(Status.STATUS_NO_TRANSACTION, TransactionHelper.lookupUserTransaction().getStatus());

        // start back the transaction
        TransactionHelper.startTransaction();
        // make sure we still have no document named "/doc"
        assertFalse(coreSession.exists(new PathRef("/doc")));
    }

    /**
     * Subclass of GenericThreadedImportTask where we have custom behavior for the creation to test things.
     */
    protected static class MyTask extends GenericThreadedImportTask {

        protected MyTask(String repositoryName) {
            super(repositoryName, new FileSourceNode("/tmp"), null, false, null, 10, null, null, "job");
        }

        @Override
        protected void recursiveCreateDocumentFromNode(DocumentModel parent, SourceNode node) throws IOException {
            // create /doc
            DocumentModel doc = session.createDocumentModel("/", "doc", "File");
            doc = session.createDocument(doc);
            session.save();
            // now throw
            throw new NuxeoException();
        }

    }

}

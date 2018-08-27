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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.listener.CollectionAsynchrnonousQuery;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features(CollectionFeature.class)
public class CollectionTestCase {

    @Inject
    CollectionManager collectionManager;

    @Inject
    CoreSession session;

    @Inject
    WorkManager workManager;

    @Inject
    TrashService trashService;

    protected static final String TEST_FILE_NAME = "testFile";

    protected static final String COLLECTION_NAME = "testCollection";

    protected static final String COLLECTION_DESCRIPTION = "dummy";

    protected static final String COLLECTION_FOLDER_PATH = "/Administrator/"
            + CollectionConstants.DEFAULT_COLLECTIONS_NAME;

    protected static final int MAX_CARDINALITY = (int) ((2 * CollectionAsynchrnonousQuery.MAX_RESULT) + 1);

    protected static final int WORK_TIMEOUT_S = 30;

    protected DocumentModel testWorkspace;

    public List<DocumentModel> createTestFiles(CoreSession session, final int nbFile) {
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        for (int i = 1; i <= nbFile; i++) {
            DocumentModel testFile = session.createDocumentModel(testWorkspace.getPath().toString(), TEST_FILE_NAME + i,
                    "File");
            testFile = session.createDocument(testFile);
            result.add(testFile);
        }
        return result;
    }

    protected void awaitCollectionWorks() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertTrue(workManager.awaitCompletion(CollectionConstants.COLLECTION_QUEUE_ID, WORK_TIMEOUT_S, TimeUnit.SECONDS));
    }

}

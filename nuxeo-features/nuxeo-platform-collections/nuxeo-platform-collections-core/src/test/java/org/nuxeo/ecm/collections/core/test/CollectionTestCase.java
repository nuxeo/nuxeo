/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, PlatformFeature.class})
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.collections.core",
        "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.web.common"})
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

    protected static final String COLLECTION_FOLDER_PATH = "/default-domain/UserWorkspaces/Administrator/"
            + CollectionConstants.DEFAULT_COLLECTIONS_NAME;

    protected static final int MAX_CARDINALITY = 60;

    protected static final int WORK_TIME_OUT_MS = 5000;

    public static List<DocumentModel> createTestFiles(CoreSession session, final int nbFile)
            throws ClientException {
        DocumentModel testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        for (int i = 1; i <= nbFile; i++) {
            DocumentModel testFile = session.createDocumentModel(
                    testWorkspace.getPath().toString(), TEST_FILE_NAME + i,
                    "File");
            testFile = session.createDocument(testFile);
            result.add(testFile);
        }
        return result;
    }

    protected void awaitCollectionWorks() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        workManager.awaitCompletion(CollectionConstants.COLLECTION_QUEUE_ID,
                WORK_TIME_OUT_MS, TimeUnit.MILLISECONDS);

        assertEquals(0, workManager.getQueueSize(
                CollectionConstants.COLLECTION_QUEUE_ID, null));
    }

}

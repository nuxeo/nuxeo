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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.automation.core.test.collections;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.core.test.CollectionFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.9.4
 */
@RunWith(FeaturesRunner.class)
@Features(CollectionFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
public class CollectionOperationsTestCase {

    protected static final String TEST_FILE_NAME = "testFile";

    protected static final String COLLECTION_NAME = "testCollection";

    protected static final String COLLECTION_DESCRIPTION = "dummy";

    protected static final String COLLECTION_FOLDER_PATH = "/default-domain/UserWorkspaces/Administrator/"
            + CollectionConstants.DEFAULT_COLLECTIONS_NAME;

    protected DocumentModel testWorkspace;

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    OperationChain chain;

    public static List<DocumentModel> createTestFiles(CoreSession session, final int nbFile) {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        for (int i = 1; i <= nbFile; i++) {
            DocumentModel testFile = session.createDocumentModel(testWorkspace.getPath().toString(),
                    TEST_FILE_NAME + i, "File");
            testFile = session.createDocument(testFile);
            result.add(testFile);
        }
        return result;
    }
}

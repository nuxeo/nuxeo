/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.tag;

import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test class for tag service based on SQL relations
 *
 * @since 9.3
 */
@Deploy("org.nuxeo.ecm.platform.tag:relation-tag-service-override.xml")
public class TestRelationTagService extends AbstractTestTagService {

    protected static final String TAG_DOCUMENT_TYPE = "Tag";

    protected static final String TAG_LABEL_FIELD = "tag:label";

    protected static final String TAGGING_DOCUMENT_TYPE = "Tagging";

    protected static final String TAGGING_SOURCE_FIELD = "relation:source";

    protected static final String TAGGING_TARGET_FIELD = "relation:target";

    @Before
    public void setUp() {
        assumeTrue("DBS does not support tags based on SQL relations", !coreFeature.getStorageConfiguration().isDBS());
    }

    @Override
    protected boolean supportsIsNull() {
        return false;
    }

    @Override
    protected void createTags() {
        DocumentModel file1 = session.getDocument(new PathRef("/file1"));
        DocumentModel file2 = session.getDocument(new PathRef("/file2"));

        String label1 = "tag1";
        DocumentModel tag1 = session.createDocumentModel(null, label1, TAG_DOCUMENT_TYPE);
        tag1.setPropertyValue(TAG_LABEL_FIELD, label1);
        tag1 = session.createDocument(tag1);

        String label2 = "tag2";
        DocumentModel tag2 = session.createDocumentModel(null, label2, TAG_DOCUMENT_TYPE);
        tag2.setPropertyValue(TAG_LABEL_FIELD, label2);
        tag2 = session.createDocument(tag2);

        DocumentModel tagging1to1 = session.createDocumentModel(null, label1, TAGGING_DOCUMENT_TYPE);
        tagging1to1.setPropertyValue(TAGGING_SOURCE_FIELD, file1.getId());
        tagging1to1.setPropertyValue(TAGGING_TARGET_FIELD, tag1.getId());
        tagging1to1 = session.createDocument(tagging1to1);

        DocumentModel tagging1to2 = session.createDocumentModel(null, label2, TAGGING_DOCUMENT_TYPE);
        tagging1to2.setPropertyValue(TAGGING_SOURCE_FIELD, file1.getId());
        tagging1to2.setPropertyValue(TAGGING_TARGET_FIELD, tag2.getId());
        tagging1to2 = session.createDocument(tagging1to2);

        DocumentModel tagging2to1 = session.createDocumentModel(null, label1, TAGGING_DOCUMENT_TYPE);
        tagging2to1.setPropertyValue(TAGGING_SOURCE_FIELD, file2.getId());
        tagging2to1.setPropertyValue(TAGGING_TARGET_FIELD, tag1.getId());
        tagging2to1 = session.createDocument(tagging2to1);

        // create a relation that isn't a Tagging
        DocumentModel rel = session.createDocumentModel(null, label1, "Relation");
        rel.setPropertyValue(TAGGING_SOURCE_FIELD, file1.getId());
        rel.setPropertyValue(TAGGING_TARGET_FIELD, tag1.getId());
        rel = session.createDocument(rel);

        session.save();

        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();
    }
}

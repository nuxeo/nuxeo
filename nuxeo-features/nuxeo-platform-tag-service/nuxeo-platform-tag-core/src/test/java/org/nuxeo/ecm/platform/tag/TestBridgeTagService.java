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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.tag;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test class for tag service bridging relations and facets during migration.
 *
 * @since 9.3
 */
@Features(TestBridgeTagService.BridgeTagServiceFeature.class)
public class TestBridgeTagService extends AbstractTestTagService {

    protected static final String TAG_DOCUMENT_TYPE = "Tag";

    protected static final String TAG_LABEL_FIELD = "tag:label";

    protected static final String TAGGING_DOCUMENT_TYPE = "Tagging";

    protected static final String TAGGING_SOURCE_FIELD = "relation:source";

    protected static final String TAGGING_TARGET_FIELD = "relation:target";

    public static class BridgeTagServiceFeature implements RunnerFeature {

        @Override
        public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) {
            TagService first = new RelationTagService();
            TagService second = new FacetedTagService();
            ((TestBridgeTagService) test).tagService = new BridgeTagService(first, second);
        }
    }

    @Override
    protected void createTags() {

        // relations for tag1+tag2 on file1

        DocumentModel file1 = session.getDocument(new PathRef("/file1"));

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

        // facet for tag1 on file2

        DocumentModel file2 = session.getDocument(new PathRef("/file2"));

        Map<String, Serializable> map = new HashMap<>();
        map.put("label", "tag1");
        map.put("username", "Administrator");
        file2.setPropertyValue("nxtag:tags", (Serializable) Collections.singletonList(map));

        session.saveDocument(file2);

        // save

        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();
    }

    @Override
    protected void testQueriesOnTags() {
        throw new AssumptionViolatedException("NXQL queries cannot use the bridge");
    }

}

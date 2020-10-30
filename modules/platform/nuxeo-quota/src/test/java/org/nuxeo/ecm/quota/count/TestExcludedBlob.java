/*
 * (C) Copyright 2011-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.quota.count;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.nuxeo.ecm.quota.count.QuotaFeature.assertQuota;
import static org.nuxeo.ecm.quota.count.QuotaFeature.createFakeBlob;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.quota.size.QuotaSizeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * NXP-11558 : Test that blob can be exclude by their path
 *
 * @author dmetzler
 */
@RunWith(FeaturesRunner.class)
@Features(QuotaFeature.class)
@Deploy("org.nuxeo.ecm.quota.test:exclude-blob-contrib.xml")
public class TestExcludedBlob {

    @Inject
    protected QuotaSizeService sus;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void quotaServiceCanGiveTheListOfXpathPropsExcludedFromQuotaComputation() {
        Collection<String> paths = sus.getExcludedPathList();
        assertThat(paths, is(notNullValue()));
        assertThat(paths.size(), is(1));
        assertThat(paths.contains("files/*/file"), is(true));
    }

    @Test
    public void quotaComputationDontTakeFilesSchemaIntoAccount() {

        // Given a document with a blob in the file schema
        DocumentModel doc = session.createDocumentModel("/", "file1", "File");
        doc.setPropertyValue("file:content", createFakeBlob(100));
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        assertQuota(doc, 100, 100);

        // When I add some Blob in the files content
        List<Map<String, Serializable>> files = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            Map<String, Serializable> files_entry = new HashMap<>();
            files_entry.put("file", createFakeBlob(70));
            files.add(files_entry);
        }
        doc.setPropertyValue("files:files", (Serializable) files);
        session.saveDocument(doc);
        txFeature.nextTransaction();

        // Then quota should not change
        doc = session.getDocument(doc.getRef());
        assertQuota(doc, 100, 100);
    }

}

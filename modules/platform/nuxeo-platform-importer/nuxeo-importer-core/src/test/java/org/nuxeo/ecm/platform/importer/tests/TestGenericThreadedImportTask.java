/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.nuxeo.runtime.api.Framework.NUXEO_TESTING_SYSTEM_PROP;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.base.GenericThreadedImportTask;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.importer.core.test:test-document-resolver-contrib.xml" })
public class TestGenericThreadedImportTask {

    protected static final String PATH_REF = "test:/test";

    protected static class TestTask extends GenericThreadedImportTask {

        protected TestTask(String repositoryName) {
            super(repositoryName, new FileSourceNode("/tmp"), null, false, null, 10, null, null, "job");
        }

        @Override
        protected void recursiveCreateDocumentFromNode(DocumentModel parent, SourceNode node) {
            DocumentModel doc = session.getDocument(new PathRef("/test"));
            doc.setPropertyValue("dr:docPathRefSimpleList", new String[] { PATH_REF, PATH_REF });
            session.saveDocument(doc);
            session.save();
        }
    }

    @Inject
    protected CoreSession session;

    @Before
    public void setup() {
        Framework.getRuntime().setProperty(NUXEO_TESTING_SYSTEM_PROP, Boolean.FALSE.toString());
    }

    @After
    public void teardown() {
        Framework.getRuntime().setProperty(NUXEO_TESTING_SYSTEM_PROP, Boolean.TRUE.toString());
    }

    @Test
    public void testTask() {

        DocumentModel doc = session.createDocumentModel("/", "test", "TestResolver");
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();

        Runnable task = new TestTask(session.getRepositoryName());
        task.run();

        TransactionHelper.startTransaction();

        doc = session.getDocument(new PathRef("/test"));
        assertArrayEquals(new String[] { PATH_REF, PATH_REF },
                (String[]) doc.getPropertyValue("dr:docPathRefSimpleList"));
    }

}

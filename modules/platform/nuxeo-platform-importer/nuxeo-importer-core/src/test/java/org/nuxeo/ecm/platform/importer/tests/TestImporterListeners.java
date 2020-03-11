/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.executor.DefaultImporterExecutor;
import org.nuxeo.ecm.platform.importer.listener.ImporterListener;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
public class TestImporterListeners {

    @Test
    public void testImportWithListeners() throws Exception {
        SourceNode src = RandomTextSourceNode.init(10);
        String targetPath = "/default-domain/workspaces/";

        DummyListener listener1 = new DummyListener();
        DummyListener listener2 = new DummyListener();
        ImporterExecutorWithListeners executor = new ImporterExecutorWithListeners(listener1, listener2);
        executor.run(src, targetPath, false, 10, 5, true);

        long createdDocs = executor.getCreatedDocsCounter();
        assertTrue(createdDocs > 0);

        assertTrue(listener1.importStarted);
        assertTrue(listener2.importStarted);
        assertTrue(listener1.importFinished);
        assertTrue(listener2.importFinished);
    }

}

class ImporterExecutorWithListeners extends DefaultImporterExecutor {

    protected ImporterListener[] listeners;

    public ImporterExecutorWithListeners(ImporterListener... listeners) {
        this.listeners = listeners;
    }

    @Override
    public String run(SourceNode source, String targetPath, Boolean skipRootContainerCreation, Integer batchSize,
            Integer nbTheards, Boolean interactive) {
        importer = new GenericMultiThreadedImporter(source, targetPath, skipRootContainerCreation, batchSize,
                nbTheards, getLogger());
        importer.setFactory(getFactory());
        importer.setThreadPolicy(getThreadPolicy());

        for (ImporterListener listener : listeners) {
            importer.addListeners(listener);
        }
        return doRun(importer, interactive);
    }
}

class DummyListener implements ImporterListener {

    public boolean importStarted = false;

    public boolean importFinished = false;

    @Override
    public void beforeImport() {
        importStarted = true;
    }

    @Override
    public void afterImport() {
        importFinished = true;
    }

    @Override
    public void importError() {
    }

}

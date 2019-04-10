/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.tests;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.executor.DefaultImporterExecutor;
import org.nuxeo.ecm.platform.importer.listener.ImporterListener;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TestImporterListeners extends SQLRepositoryTestCase {

    public TestImporterListeners() {
        super();
    }

    protected TestImporterListeners(String name) {
        super(name);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        fireFrameworkStarted();
        openSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

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

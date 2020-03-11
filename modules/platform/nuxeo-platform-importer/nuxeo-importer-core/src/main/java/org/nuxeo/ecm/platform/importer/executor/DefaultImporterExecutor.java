/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunner;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Default importer
 *
 * @author Thierry Delprat
 */
public class DefaultImporterExecutor extends AbstractImporterExecutor {

    private static final Log log = LogFactory.getLog(DefaultImporterExecutor.class);

    protected GenericMultiThreadedImporter importer = null;

    protected String repositoryName;

    public DefaultImporterExecutor() {
    }

    public DefaultImporterExecutor(String repositoryName) {
        this.repositoryName=repositoryName;
    }

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    public long getCreatedDocsCounter() {
        return GenericMultiThreadedImporter.getCreatedDocsCounter();
    }

    public String run(String inputPath, String targetPath, Boolean skipRootContainerCreation, Integer batchSize,
            Integer nbTheards, Boolean interactive) {
        SourceNode source = new FileSourceNode(inputPath);
        return run(source, targetPath, skipRootContainerCreation, batchSize, nbTheards, interactive);
    }

    public String run(SourceNode source, String targetPath, Boolean skipRootContainerCreation, Integer batchSize,
            Integer nbTheards, Boolean interactive) {
        importer = new GenericMultiThreadedImporter(source, targetPath, skipRootContainerCreation, batchSize,
                nbTheards, getLogger());
        importer.setFactory(getFactory());
        importer.setThreadPolicy(getThreadPolicy());
        importer.setTransactionTimeout(getTransactionTimeout());
        return doRun(importer, interactive);
    }

    @Override
    public String run(ImporterRunner runner, Boolean interactive) {
        return doRun(runner, interactive);
    }

}

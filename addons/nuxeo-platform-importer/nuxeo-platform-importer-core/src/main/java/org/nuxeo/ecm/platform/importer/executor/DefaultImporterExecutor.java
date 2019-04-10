/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 *
 * Default importer
 *
 * @author Thierry Delprat
 *
 */
public class DefaultImporterExecutor extends AbstractImporterExecutor {

    private static final Log log = LogFactory.getLog(DefaultImporterExecutor.class);

    protected GenericMultiThreadedImporter importer = null;

    public DefaultImporterExecutor() {

    }

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    public long getCreatedDocsCounter() {
        return importer.getCreatedDocsCounter();
    }

    public String run(String inputPath, String targetPath,
            Boolean skipRootContainerCreation, Integer batchSize,
            Integer nbTheards, Boolean interactive) throws Exception {
        SourceNode source = new FileSourceNode(inputPath);
        return run(source, targetPath, skipRootContainerCreation, batchSize,
                nbTheards, interactive);
    }

    public String run(SourceNode source, String targetPath,
            Boolean skipRootContainerCreation, Integer batchSize,
            Integer nbTheards, Boolean interactive) throws Exception {
        importer = new GenericMultiThreadedImporter(source, targetPath,
                skipRootContainerCreation, batchSize, nbTheards, getLogger());
        importer.setFactory(getFactory());
        importer.setThreadPolicy(getThreadPolicy());
        return doRun(importer, interactive);
    }

}

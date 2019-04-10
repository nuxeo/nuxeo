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

package org.nuxeo.dam.importer.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.dam.DamService;
import org.nuxeo.dam.importer.core.filter.DamImporterFilter;
import org.nuxeo.dam.importer.core.filter.DamImportingDocumentFilter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.executor.AbstractImporterExecutor;
import org.nuxeo.ecm.platform.importer.factories.FileManagerDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.source.FileWithMetadataSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DamImporterExecutor extends AbstractImporterExecutor {

    private static final Log log = LogFactory.getLog(DamImporterExecutor.class);

    protected String inputPath;

    protected String importFolderPath;

    protected String importSetTitle;

    protected boolean interactiveMode;

    protected boolean removeImportedFolder;

    public DamImporterExecutor(String inputPath, String importFolderPath,
            String importSetTitle, boolean interactiveMode,
            boolean removeImportedFolder) {
        this.inputPath = inputPath;
        this.importFolderPath = importFolderPath;
        this.importSetTitle = importSetTitle;
        this.interactiveMode = interactiveMode;
        this.removeImportedFolder = removeImportedFolder;
    }

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    public String run() throws Exception {
        java.io.File srcFile = new java.io.File(inputPath);
        SourceNode source = new FileWithMetadataSourceNode(srcFile);

        DamService damService = Framework.getLocalService(DamService.class);
        ImporterRunnerConfiguration configuration = new ImporterRunnerConfiguration.Builder(
                source, damService.getAssetLibraryPath(), getLogger()).skipRootContainerCreation(
                true).build();
        DamMultiThreadedImporter runner = DamMultiThreadedImporter.createWithImportFolderPath(
                configuration, importFolderPath, importSetTitle,
                removeImportedFolder);
        runner.setFactory(new FileManagerDocumentModelFactory());

        ImporterFilter filter = new EventServiceConfiguratorFilter(false,
                false, false, true);
        runner.addFilter(filter);
        runner.addFilter(new DamImporterFilter());

        runner.addImportingDocumentFilters(new DamImportingDocumentFilter());

        return doRun(runner, interactiveMode);
    }

}

/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.importer.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.executor.AbstractImporterExecutor;
import org.nuxeo.ecm.platform.importer.executor.DefaultImporterExecutor;
import org.nuxeo.ecm.platform.importer.factories.AbstractDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class DefaultImporterServiceImpl implements DefaultImporterService {

    private static Log log = LogFactory.getLog(DefaultImporterServiceImpl.class);

    private Class<? extends AbstractDocumentModelFactory> docModelFactoryClass;

    private Class<? extends SourceNode> sourceNodeClass;

    protected SourceNode sourceNode;

    protected AbstractDocumentModelFactory documentModelFactory;

    protected String folderishDocType;

    protected String leafDocType;

    protected ImporterLogger importerLogger;

    @Override
    public void importDocuments(String destinationPath, String sourcePath,
            boolean skipRootContainerCreation, int batchSize,
            int noImportingThreads) throws ClientException {

        if (sourceNodeClass != null
                && FileSourceNode.class.isAssignableFrom(sourceNodeClass)) {
            try {
                setSourceNode(sourceNodeClass.getConstructor(String.class).newInstance(
                        sourcePath));
            } catch (Exception e) {
                log.error(e);
            }
        }

        if (docModelFactoryClass != null
                && DefaultDocumentModelFactory.class.isAssignableFrom(docModelFactoryClass)) {
            try {
                setDocumentModelFactory(docModelFactoryClass.getConstructor(
                        String.class, String.class).newInstance(
                        getFolderishDocType(), getLeafDocType()));
            } catch (Exception e) {
                log.error(e);
            }
        }

        if (sourceNode == null) {
            log.error("Need to set a sourceNode to be used by this importer");
            return;
        }
        if (documentModelFactory == null) {
            log.error("Need to set a documentModelFactory to be used by this importer");
        }

        DefaultImporterExecutor executor = new DefaultImporterExecutor();
        executor.setFactory(getDocumentModelFactory());
        try {
            executor.run(getSourceNode(), destinationPath,
                    skipRootContainerCreation, batchSize, noImportingThreads,
                    true);
        } catch (Exception e) {
            log.error("Import error:", e);
            throw new ClientException(e);
        }

    }

    public String importDocuments(AbstractImporterExecutor executor,
            String destinationPath, String sourcePath,
            boolean skipRootContainerCreation, int batchSize,
            int noImportingThreads, boolean interactive) throws ClientException {

        if (sourceNodeClass != null
                && FileSourceNode.class.isAssignableFrom(sourceNodeClass)) {
            try {
                setSourceNode(sourceNodeClass.getConstructor(String.class).newInstance(
                        sourcePath));
            } catch (Exception e) {
                log.error(e);
            }
        }

        if (docModelFactoryClass != null
                && DefaultDocumentModelFactory.class.isAssignableFrom(docModelFactoryClass)) {
            try {
                setDocumentModelFactory(docModelFactoryClass.getConstructor(
                        String.class, String.class).newInstance(
                        getFolderishDocType(), getLeafDocType()));
            } catch (Exception e) {
                log.error(e);
            }
        }

        if (sourceNode == null) {
            log.error("Need to set a sourceNode to be used by this importer");
            return "Can not import";
        }
        if (documentModelFactory == null) {
            log.error("Need to set a documentModelFactory to be used by this importer");
        }

        ImporterRunnerConfiguration configuration = new ImporterRunnerConfiguration.Builder(
                getSourceNode(), destinationPath, executor.getLogger()).skipRootContainerCreation(
                skipRootContainerCreation).batchSize(batchSize).nbThreads(
                noImportingThreads).build();
        GenericMultiThreadedImporter runner;
        try {
            runner = new GenericMultiThreadedImporter(configuration);
        } catch (Exception e1) {
            log.error(e1);
            throw new ClientException(e1);
        }
        ImporterFilter filter = new EventServiceConfiguratorFilter(false,
                false, false, true);
        runner.addFilter(filter);
        runner.setFactory(getDocumentModelFactory());
        try {
            return executor.run(runner, interactive);
        } catch (Exception e) {
            log.error("Import error:", e);
            throw new ClientException(e);
        }
    }

    @Override
    public void setDocModelFactoryClass(
            Class<? extends AbstractDocumentModelFactory> docModelFactoryClass) {
        this.docModelFactoryClass = docModelFactoryClass;
    }

    @Override
    public void setSourceNodeClass(Class<? extends SourceNode> sourceNodeClass) {
        this.sourceNodeClass = sourceNodeClass;
    }

    public SourceNode getSourceNode() {
        return sourceNode;
    }

    public void setSourceNode(SourceNode sourceNode) {
        this.sourceNode = sourceNode;
    }

    public AbstractDocumentModelFactory getDocumentModelFactory() {
        return documentModelFactory;
    }

    public void setDocumentModelFactory(
            AbstractDocumentModelFactory documentModelFactory) {
        this.documentModelFactory = documentModelFactory;
    }

    public String getFolderishDocType() {
        return folderishDocType;
    }

    @Override
    public void setFolderishDocType(String folderishDocType) {
        this.folderishDocType = folderishDocType;
    }

    public String getLeafDocType() {
        return leafDocType;
    }

    @Override
    public void setLeafDocType(String fileDocType) {
        this.leafDocType = fileDocType;
    }

    public ImporterLogger getImporterLogger() {
        return importerLogger;
    }

    public void setImporterLogger(ImporterLogger importerLogger) {
        this.importerLogger = importerLogger;
    }

}

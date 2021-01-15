/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.importer.service;

import static java.util.Objects.requireNonNullElse;
import static org.nuxeo.ecm.platform.importer.service.DefaultImporterComponent.DEFAULT_FOLDERISH_DOC_TYPE;
import static org.nuxeo.ecm.platform.importer.service.DefaultImporterComponent.DEFAULT_LEAF_DOC_TYPE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.executor.AbstractImporterExecutor;
import org.nuxeo.ecm.platform.importer.executor.DefaultImporterExecutor;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class DefaultImporterServiceImpl implements DefaultImporterService {

    private static final Logger log = LogManager.getLogger(DefaultImporterServiceImpl.class);

    private Class<? extends ImporterDocumentModelFactory> docModelFactoryClass;

    private Class<? extends SourceNode> sourceNodeClass;

    private String folderishDocType;

    private String leafDocType;

    private ImporterDocumentModelFactory documentModelFactory;

    private ImporterLogger importerLogger;

    private String repositoryName;

    private boolean bulkMode = true;

    protected boolean enablePerfLogging = true;

    /**
     * @since 11.5
     */
    public void configure(ImporterConfigurationDescriptor descriptor) throws ReflectiveOperationException {
        this.sourceNodeClass = requireNonNullElse(descriptor.getSourceNodeClass(), FileSourceNode.class);
        checkSourceNode(sourceNodeClass);

        ImporterConfigurationDescriptor.DocumentModelFactory factory = descriptor.getDocumentModelFactory();
        this.docModelFactoryClass = requireNonNullElse(factory.getDocumentModelFactoryClass(),
                DefaultDocumentModelFactory.class);
        this.folderishDocType = requireNonNullElse(factory.getFolderishType(), DEFAULT_FOLDERISH_DOC_TYPE);
        this.leafDocType = requireNonNullElse(factory.getLeafType(), DEFAULT_LEAF_DOC_TYPE);
        documentModelFactory = createDocumentModelFactory(docModelFactoryClass, folderishDocType, leafDocType);

        Class<? extends ImporterLogger> logClass = descriptor.getImporterLog();
        if (logClass == null) {
            log.info("No specific ImporterLogger configured for this importer");
        } else {
            importerLogger = logClass.getDeclaredConstructor().newInstance();
        }

        this.repositoryName = descriptor.getRepository();
        this.bulkMode = requireNonNullElse(descriptor.getBulkMode(), true);
        this.enablePerfLogging = requireNonNullElse(descriptor.getEnablePerfLogging(), true);
    }

    protected void checkSourceNode(Class<? extends SourceNode> sourceNodeClass) {
        if (sourceNodeClass == null || !FileSourceNode.class.isAssignableFrom(sourceNodeClass)) {
            throw new IllegalArgumentException("Invalid source node");
        }
    }

    protected SourceNode createNewSourceNodeInstanceForSourcePath(String sourcePath) {
        checkSourceNode(sourceNodeClass);
        try {
            return sourceNodeClass.getConstructor(String.class).newInstance(sourcePath);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected ImporterDocumentModelFactory createDocumentModelFactory(
            Class<? extends ImporterDocumentModelFactory> docModelFactoryClass, String folderishDocType,
            String leafDocType) {
        try {
            if (DefaultDocumentModelFactory.class.isAssignableFrom(docModelFactoryClass)) {
                return docModelFactoryClass.getConstructor(String.class, String.class)
                                           .newInstance(folderishDocType, leafDocType);
            } else {
                return docModelFactoryClass.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void importDocuments(String destinationPath, String sourcePath, boolean skipRootContainerCreation,
            int batchSize, int noImportingThreads) {
        SourceNode sourceNode = createNewSourceNodeInstanceForSourcePath(sourcePath);
        DefaultImporterExecutor executor = new DefaultImporterExecutor(repositoryName);
        executor.setFactory(documentModelFactory);
        executor.setTransactionTimeout(0);
        executor.run(sourceNode, destinationPath, skipRootContainerCreation, batchSize, noImportingThreads, true);
    }

    protected String doImport(AbstractImporterExecutor executor, String destinationPath, String sourcePath,
            boolean skipRootContainerCreation, int batchSize, int noImportingThreads, boolean interactive,
            ImporterDocumentModelFactory factory) {
        SourceNode sourceNode = createNewSourceNodeInstanceForSourcePath(sourcePath);
        ImporterRunnerConfiguration configuration = new ImporterRunnerConfiguration.Builder(sourceNode, destinationPath,
                executor.getLogger()).skipRootContainerCreation(skipRootContainerCreation)
                                     .batchSize(batchSize)
                                     .nbThreads(noImportingThreads)
                                     .repository(repositoryName)
                                     .build();
        GenericMultiThreadedImporter runner = new GenericMultiThreadedImporter(configuration);
        runner.setEnablePerfLogging(enablePerfLogging);
        runner.setTransactionTimeout(executor.getTransactionTimeout());
        ImporterFilter filter = new EventServiceConfiguratorFilter(false, false, false, false, bulkMode);
        runner.addFilter(filter);
        runner.setFactory(factory);
        return executor.run(runner, interactive);
    }

    @Override
    public String importDocuments(AbstractImporterExecutor executor, String destinationPath, String sourcePath,
            boolean skipRootContainerCreation, int batchSize, int noImportingThreads, boolean interactive) {
        return doImport(executor, destinationPath, sourcePath, skipRootContainerCreation, batchSize, noImportingThreads,
                interactive, documentModelFactory);
    }

    @Override
    public String importDocuments(AbstractImporterExecutor executor, String leafType, String folderishType,
            String destinationPath, String sourcePath, boolean skipRootContainerCreation, int batchSize,
            int noImportingThreads, boolean interactive) {
        ImporterDocumentModelFactory factory = createDocumentModelFactory(docModelFactoryClass,
                requireNonNullElse(folderishType, this.folderishDocType),
                requireNonNullElse(leafType, this.leafDocType));
        return doImport(executor, destinationPath, sourcePath, skipRootContainerCreation, batchSize, noImportingThreads,
                interactive, factory);
    }

    public String getFolderishDocType() {
        return folderishDocType;
    }

    public String getLeafDocType() {
        return leafDocType;
    }

    public ImporterLogger getImporterLogger() {
        return importerLogger;
    }

    @Override
    public boolean getEnablePerfLogging() {
        return this.enablePerfLogging;
    }

}

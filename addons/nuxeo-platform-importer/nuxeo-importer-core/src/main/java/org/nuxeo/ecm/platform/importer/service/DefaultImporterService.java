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

import org.nuxeo.ecm.platform.importer.executor.AbstractImporterExecutor;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Allows basic configuration of the default importer :
 * <p>
 * Allows configuration of the a DocumentModelFactory and the document types it creates ( if no implementation is
 * contributed, <code>DefaultDocumentModelFactory</code> is used;
 * <p>
 * Also allows configuration of the SourceNode implementation; if none is provided the
 * <code>FileSourceNode<code>> it's used by default
 */
public interface DefaultImporterService {

    /**
     * Imports documents using a DefaultImporterExecutor and the contributed documentModelFactory and SourceNode
     * implementations; If no documentModelFactory implementation was contributed to the service,
     * <code>DefaultDocumentModelFactory</code> it's used If no SourceNode implementation was contributed to the
     * service, <code>FileSourceNode</code> it's used
     *
     * @param destionationPath
     * @param sourcePath
     * @param skipRootContainerCreation
     * @param batchSize
     * @param noImportingThreads
     */
    void importDocuments(String destionationPath, String sourcePath, boolean skipRootContainerCreation, int batchSize,
            int noImportingThreads);

    /***
     * Imports documents using a the given executor and the contributed documentModelFactory and SourceNode
     * implementations; If no documentModelFactory implementation was contributed to the service,
     * <code>DefaultDocumentModelFactory</code> it's used If no SourceNode implementation was contributed to the
     * service, <code>FileSourceNode</code> it's used
     *
     * @param executor
     * @param destinationPath
     * @param sourcePath
     * @param skipRootContainerCreation
     * @param batchSize
     * @param noImportingThreads
     * @param interactive
     */
    String importDocuments(AbstractImporterExecutor executor, String destinationPath, String sourcePath,
            boolean skipRootContainerCreation, int batchSize, int noImportingThreads, boolean interactive);

    /***
     * Imports documents using a the given executor and the contributed documentModelFactory and SourceNode
     * implementations; Allows to overwrite the leaf and folderish types used by the documentModelFactory when
     * importing; if one of them is not specified then the contributed one is used If no documentModelFactory
     * implementation was contributed to the service, <code>DefaultDocumentModelFactory</code> it's used If no
     * SourceNode implementation was contributed to the service, <code>FileSourceNode</code> it's used
     *
     * @param executor
     * @param destinationPath
     * @param sourcePath
     * @param skipRootContainerCreation
     * @param batchSize
     * @param noImportingThreads
     * @param interactive
     */
    String importDocuments(AbstractImporterExecutor executor, String leafType, String folderishType,
            String destinationPath, String sourcePath, boolean skipRootContainerCreation, int batchSize,
            int noImportingThreads, boolean interactive);

    void setSourceNodeClass(Class<? extends SourceNode> sourceNodeClass);

    void setDocModelFactoryClass(Class<? extends ImporterDocumentModelFactory> docModelFactoryClass);

    void setLeafDocType(String fileDocType);

    void setFolderishDocType(String folderishDocType);

    void setImporterLogger(ImporterLogger importerLogger);

    /**
     * @since 5.9.4
     */
    void setTransactionTimeout(int transactionTimeout);

    /**
     * @since 7.1
     * @param repositoryName
     */
    void setRepository(String repositoryName);

    /**
     * Added waiting the importer refactoring. Only used by Scan Importer.
     *
     * @since 5.7.3
     */
    @Deprecated
    Class<? extends SourceNode> getSourceNodeClass();

    /**
     * Added waiting the importer refactoring. Only used by Scan Importer.
     *
     * @since 5.7.3
     */
    @Deprecated
    Class<? extends ImporterDocumentModelFactory> getDocModelFactoryClass();

    /**
     * Sets the bulk mode for the importer.
     *
     * @param bulkMode {@code true} to enable bulk mode (default), or {@code false} to disable it
     * @since 8.3
     */
    void setBulkMode(boolean bulkMode);

}

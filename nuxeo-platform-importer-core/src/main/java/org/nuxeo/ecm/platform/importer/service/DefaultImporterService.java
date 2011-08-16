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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.importer.factories.AbstractDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Allows basic configuration of the default importer :
 * <p>
 * Allows configuration of the a DocumentModelFactory and the document types it
 * creates ( if no implementation is contributed,
 * <code>DefaultDocumentModelFactory</code> is used;
 * <p>
 * Also allows configuration of the SourceNode implementation; if none is
 * provided the <code>FileSourceNode<code>> it's used by default
 * 
 */
public interface DefaultImporterService {

    /**
     * Imports documents using a DefaultImporterExecutor and the contributed
     * documentModelFactory and SourceNode implementations;
     * 
     * If no documentModelFactory implementation was contributed to the service,
     * <code>DefaultDocumentModelFactory</code> it's used
     * 
     * If no SourceNode implementation was contributed to the service,
     * <code>FileSourceNode</code> it's used
     * 
     * @param destionationPath
     * @param sourcePath
     * @param skipRootContainerCreation
     * @param batchSize
     * @param noImportingThreads
     * @throws ClientException
     */
    void importDocuments(String destionationPath, String sourcePath,
            boolean skipRootContainerCreation, int batchSize,
            int noImportingThreads) throws ClientException;

    void setSourceNodeClass(Class<? extends SourceNode> sourceNodeClass);

    void setDocModelFactoryClass(
            Class<? extends AbstractDocumentModelFactory> docModelFactoryClass);

    void setLeafDocType(String fileDocType);

    void setFolderishDocType(String folderishDocType);
}
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

package org.nuxeo.ecm.platform.importer.factories;

import java.io.IOException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.base.GenericThreadedImportTask;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Interface for DocumentModel factory
 *
 * @author Thierry Delprat
 * @author Antoine Taillefer
 */
public interface ImporterDocumentModelFactory {

    boolean isTargetDocumentModelFolderish(SourceNode node);

    DocumentModel createFolderishNode(CoreSession session, DocumentModel parent, SourceNode node) throws IOException;

    DocumentModel createLeafNode(CoreSession session, DocumentModel parent, SourceNode node) throws IOException;

    /**
     * Defines the process to execute when a folderish node creation error occurs.
     * <p>
     * This method is called by
     * {@code GenericThreadedImportTask#doCreateFolderishNode(DocumentModel parent, SourceNode node)} if an exception is
     * thrown by {@link #createFolderishNode(CoreSession, DocumentModel, SourceNode)}.
     * </p>
     *
     * @return true if the global import task should continue after processing the error, false if it should be stopped
     *         immediately after processing the error.
     */
    boolean processFolderishNodeCreationError(CoreSession session, DocumentModel parent, SourceNode node);

    /**
     * Defines the process to execute when a leaf node creation error occurs.
     * <p>
     * This method is called by {@code GenericThreadedImportTask#doCreateLeafNode(DocumentModel parent, SourceNode node)}
     * if an exception is thrown by {@link #createLeafNode(CoreSession, DocumentModel, SourceNode)}.
     * </p>
     *
     * @return true if the global import task should continue after processing the error, false if it should be stopped
     *         immediately after processing the error.
     */
    boolean processLeafNodeCreationError(CoreSession session, DocumentModel parent, SourceNode node);

}

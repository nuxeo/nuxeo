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

package org.nuxeo.ecm.platform.importer.factories;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.base.GenericThreadedImportTask;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 *
 * Interface for DocumentModel factory
 *
 * @author Thierry Delprat
 * @author Antoine Taillefer
 *
 */
public interface ImporterDocumentModelFactory {

    public boolean isTargetDocumentModelFolderish(SourceNode node);

    public DocumentModel createFolderishNode(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception;

    public DocumentModel createLeafNode(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception;

    /**
     * Defines the process to execute when a folderish node creation error
     * occurs.
     * <p>
     * This method is called by
     * {@link GenericThreadedImportTask#doCreateFolderishNode(DocumentModel parent, SourceNode node)}
     * if an exception is thrown by
     * {@link #createFolderishNode(CoreSession, DocumentModel, SourceNode)}.
     * </p>
     *
     * @return true if the global import task should continue after processing
     *         the error, false if it should be stopped immediately after
     *         processing the error.
     */
    public boolean processFolderishNodeCreationError(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception;

    /**
     * Defines the process to execute when a leaf node creation error occurs.
     * <p>
     * This method is called by
     * {@link GenericThreadedImportTask#doCreateLeafNode(DocumentModel parent, SourceNode node)}
     * if an exception is thrown by
     * {@link #createLeafNode(CoreSession, DocumentModel, SourceNode)}.
     * </p>
     *
     * @return true if the global import task should continue after processing
     *         the error, false if it should be stopped immediately after
     *         processing the error.
     */
    public boolean processLeafNodeCreationError(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception;

}

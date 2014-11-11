/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.api.repository;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author Max Stepanov
 *
 */
public interface DocumentProvider {

    CoreSession getSession();

    DocumentModel getDocument(String id) throws ClientException;
    DocumentModel getDocument(String id, boolean force) throws ClientException;
    DocumentModel getCachedDocument(String id);

    /**
     * Gets a document model given its reference.
     * <p>
     * The default schemas are used to populate the returned document model.
     * Default schemas are configured via the document type manager.
     * <p>
     * Any other data model not part of the default schemas will be lazily
     * loaded as needed.
     *
     * @param docRef the document reference
     * @return the document
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel getDocument(DocumentRef docRef) throws ClientException;

    /**
     * Gets a document model given its reference.
     * <p>
     * The default schemas are used to populate the returned document model.
     * Default schemas are configured via the document type manager.
     * <p>
     * Any other data model not part of the default schemas will be lazily
     * loaded as needed.
     *
     * @param docRef the document reference
     * @param force force getting fresh document copy
     * @return the document
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel getDocument(DocumentRef docRef, boolean force) throws ClientException;

    /**
     * Gets a document model given its reference and the initial set of schemas
     * to use.
     * <p>
     * Same as the previous method with the difference that the default schemas
     * are overwriten by the given schemas.
     *
     * @param docRef the document reference
     * @param schemas the initial schemas to use to populate the document model
     * @return
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel getDocument(DocumentRef docRef, String[] schemas) throws ClientException;

    /**
     * Gets a cached document model given its reference.
     * @param docRef the document reference
     * @return
     */
    DocumentModel getCachedDocument(DocumentRef docRef);

    /**
     * Gets the children of the given parent.
     *
     * @param parent the parent reference
     * @return the children if any, an empty list if no children or null if the
     *         specified parent document is not a folder
     * @throws ClientException
     */
    DocumentModelList getChildren(DocumentRef parent) throws ClientException;

    /**
     * Gets a child document given its name and the parent reference.
     * <p>
     * Throws an exception if the document could not be found.
     * <p>
     * If the supplied id is null, returns the default child of the document if
     * any, otherwise raises an exception.
     * <p>
     * If the parent is null or its path is null, then root is considered.
     *
     * @param parent the reference to the parent document
     * @param name the name of the child document to retrieve
     * @return the named child if exists, raises a ClientException otherwise
     * @throws ClientException if there is no child with the given name
     */
    DocumentModel getChild(DocumentRef parent, String name) throws ClientException;

    /**
     * Gets the children of the given parent.
     *
     * @param parent the parent reference
     * @return iterator over the children collection or null if the specified
     *         parent document is not a folder
     * @throws ClientException
     */
    DocumentModelIterator getChildrenIterator(DocumentRef parent) throws ClientException;

    /**
     * Gets the root document of this repository.
     *
     * @return the root document. cannot be null
     * @throws ClientException
     * @throws SecurityException
     */
    DocumentModel getRootDocument() throws ClientException;

    /**
     * Gets the parent document or null if this is the root document.
     *
     * @return the parent document or null if this is the root document
     * @throws ClientException
     */
    DocumentModel getParentDocument(DocumentRef docRef) throws ClientException;

    /**
     * Tests if the document pointed by the given reference exists and is
     * accessible.
     * <p>
     * This operation makes no difference between non-existence and
     * permission problems.
     * </p>
     * <p>
     * If the parent is null or its path is null, then root is considered.
     * </p>
     * @param docRef the reference to the document to test for existence
     * @return true if the referenced document exists, false otherwise
     * @throws ClientException
     */
    boolean exists(DocumentRef docRef) throws ClientException;

    /**
     * Gets the children of the given parent filtered according to the given
     * document type.
     *
     * @param parent the parent reference
     * @param type the wanted document type
     * @return the documents if any, an empty list if none were found or null if
     *         the parent document is not a folder
     * @throws ClientException
     */
    DocumentModelList getChildren(DocumentRef parent, String type) throws ClientException;

}

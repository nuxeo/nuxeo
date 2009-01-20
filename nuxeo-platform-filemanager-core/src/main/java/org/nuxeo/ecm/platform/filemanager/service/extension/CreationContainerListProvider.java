/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: CreationContainerListProvider.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Interface to implement for contributions to the FileManagerService
 * creationContainerListProvider extension point.
 * <p>
 * The provider should tell for a given (handled) document type the list of
 * candidate container the user can create new document in.
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public interface CreationContainerListProvider {

    /**
     * Unique name of the CreationContainerListProvider. The name of a provider
     * should be used for the equals.
     *
     * @return the name
     */
    String getName();

    void setName(String name);

    /**
     * Arrays of the document types accepted by the
     * CreationContainerListProvider instance. null or empty array mean any
     * document type is accepted.
     *
     * @return arrays of document types
     */
    String[] getDocTypes();

    void setDocTypes(String[] docTypes);

    /**
     * Tell whether docType is handled by the provider.
     *
     * @param docType name of the document core type
     * @return true is the docType is accepted
     */
    boolean accept(String docType);

    /**
     * Build the list of candidate containers for the given document type and
     * session.
     *
     * @param documentManager the current session context
     * @param docType the type of document to create
     * @return the list of candidate containers
     * @throws Exception
     */
    DocumentModelList getCreationContainerList(CoreSession documentManager,
            String docType) throws Exception;

}

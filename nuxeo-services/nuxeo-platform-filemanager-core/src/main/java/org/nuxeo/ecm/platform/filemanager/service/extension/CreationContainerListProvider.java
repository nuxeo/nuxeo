/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: CreationContainerListProvider.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Interface to implement for contributions to the FileManagerService creationContainerListProvider extension point.
 * <p>
 * The provider should tell for a given (handled) document type the list of candidate container the user can create new
 * document in.
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public interface CreationContainerListProvider {

    /**
     * Unique name of the CreationContainerListProvider. The name of a provider should be used for the equals.
     *
     * @return the name
     */
    String getName();

    void setName(String name);

    /**
     * Arrays of the document types accepted by the CreationContainerListProvider instance. null or empty array mean any
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
     * Build the list of candidate containers for the given document type and session.
     *
     * @param documentManager the current session context
     * @param docType the type of document to create
     * @return the list of candidate containers
     */
    DocumentModelList getCreationContainerList(CoreSession documentManager, String docType);

}

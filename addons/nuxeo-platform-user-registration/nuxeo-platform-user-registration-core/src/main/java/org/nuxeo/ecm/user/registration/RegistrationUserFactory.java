/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.registration;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface RegistrationUserFactory {

    /**
     * 
     * @deprecated Logic into createUser will be moved into the component to
     *             prevent from the need to call doPostUserCreation inside this
     *             method. Must be moved into doCreateUser method.
     * @see org.nuxeo.ecm.user.registration.DefaultRegistrationUserFactory#createUser
     */
    NuxeoPrincipal createUser(CoreSession session, DocumentModel registrationDoc)
            throws ClientException, UserRegistrationException;

    /**
     * Handle user creation
     * 
     * @since 5.6
     */
    NuxeoPrincipal doCreateUser(CoreSession session,
            DocumentModel registrationDoc) throws ClientException,
            UserRegistrationException;

    /**
     * Called just after the user is created
     */
    void doPostUserCreation(CoreSession session, DocumentModel registrationDoc,
            NuxeoPrincipal user) throws ClientException,
            UserRegistrationException;

    /**
     * @since 5.6
     * @see UserRegistrationComponent#addRightsOnDoc
     */
    DocumentModel doAddDocumentPermission(CoreSession session,
                                          DocumentModel registrationDoc) throws ClientException;

    /**
     * Called just after the right is setted
     * 
     * @since 5.6
     * @see UserRegistrationComponent#addRightsOnDoc
     */
    void doPostAddDocumentPermission(CoreSession session,
                                     DocumentModel registrationDoc, DocumentModel document)
            throws ClientException;
}

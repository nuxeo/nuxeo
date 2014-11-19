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

package org.nuxeo.ecm.user.invite;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface InvitationUserFactory {

    /**
     * Handle user creation
     *
     * @since 5.6
     */
    NuxeoPrincipal doCreateUser(CoreSession session,
            DocumentModel registrationDoc,UserRegistrationConfiguration configuration) throws ClientException,
            UserRegistrationException;

    /**
     * Called just after the user is created
     */
    void doPostUserCreation(CoreSession session, DocumentModel registrationDoc,
            NuxeoPrincipal user) throws ClientException,
            UserRegistrationException;
}

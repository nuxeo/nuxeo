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

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.user.invite.UserInvitationService;
import org.nuxeo.ecm.user.invite.UserRegistrationConfiguration;
import org.nuxeo.ecm.user.invite.UserRegistrationException;
import org.nuxeo.ecm.user.invite.UserRegistrationInfo;

public interface UserRegistrationService extends UserInvitationService {

    public static final String CONFIGURATION_NAME = UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;

    /**
     * Stores a registration request and return a unique ID for it
     *
     * @return
     */
    String submitRegistrationRequest(UserRegistrationInfo userInfo,
            Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept,
            String principalName) throws ClientException,
            UserRegistrationException;

    /**
     * Validate a registration request and generate the target User
     *
     * @param requestId
     */
    Map<String, Serializable> validateRegistrationAndSendEmail(
            String requestId, Map<String, Serializable> additionnalInfo)
            throws ClientException, UserRegistrationException;

    /**
     * Add an ACL with the right specified in the registration Doc or nothing,
     * if no rights needed.
     *
     * @param registrationDoc containing all registration info
     * @since 5.6
     */
    void addRightsOnDoc(CoreSession session, DocumentModel registrationDoc)
            throws ClientException;

    /**
     * Stores a registration request like submitRegistrationRequest with
     * Document information
     *
     * @return a unique ID for it`
     * @since 5.6
     */
    String submitRegistrationRequest(String configurationName,
            UserRegistrationInfo userInfo, DocumentRegistrationInfo docInfo,
            Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept,
            String principalName) throws ClientException,
            UserRegistrationException;
}

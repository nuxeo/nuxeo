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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.registration;

import java.io.Serializable;
import java.util.Map;

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
    String submitRegistrationRequest(UserRegistrationInfo userInfo, Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept, String principalName) throws
            UserRegistrationException;

    /**
     * Validate a registration request and generate the target User
     *
     * @param requestId
     */
    Map<String, Serializable> validateRegistrationAndSendEmail(String requestId,
            Map<String, Serializable> additionnalInfo) throws UserRegistrationException;

    /**
     * Add an ACL with the right specified in the registration Doc or nothing, if no rights needed.
     *
     * @param registrationDoc containing all registration info
     * @since 5.6
     */
    void addRightsOnDoc(CoreSession session, DocumentModel registrationDoc);

    /**
     * Stores a registration request like submitRegistrationRequest with Document information
     *
     * @return a unique ID for it`
     * @since 5.6
     */
    String submitRegistrationRequest(String configurationName, UserRegistrationInfo userInfo,
            DocumentRegistrationInfo docInfo, Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept, String principalName) throws
            UserRegistrationException;
}

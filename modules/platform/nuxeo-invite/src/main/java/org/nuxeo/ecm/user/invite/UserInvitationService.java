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

package org.nuxeo.ecm.user.invite;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface UserInvitationService {

    String REGISTRATION_CONFIGURATION_NAME = "configurationName";

    String REGISTRATION_DATA_DOC = "registrationDoc";

    String REGISTRATION_DATA_USER = "registeredUser";

    public enum ValidationMethod {
        EMAIL, NONE
    }

    /**
     * Create a document model for the UserRegistration doctype.
     *
     * @param configurationName The name of the configuration.
     * @return The document model
     * @since 5.9.3
     */
    DocumentModel getUserRegistrationModel(String configurationName);

    /**
     * Stores a registration request and return a unique ID for it
     *
     * @return
     */
    String submitRegistrationRequest(DocumentModel userRegistrationModel, Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept) throws UserRegistrationException;

    /**
     * accept the registration request
     *
     * @param requestId
     */
    void acceptRegistrationRequest(String requestId, Map<String, Serializable> additionnalInfo) throws
            UserRegistrationException;

    /**
     * reject the registration request
     *
     * @param requestId
     */
    void rejectRegistrationRequest(String requestId, Map<String, Serializable> additionnalInfo) throws
            UserRegistrationException;

    /**
     * Validate a registration request and generate the target User
     *
     * @param requestId
     */
    Map<String, Serializable> validateRegistration(String requestId, Map<String, Serializable> additionnalInfo);

    /**
     * Validate a registration request and generate the target User
     *
     * @param requestId
     */
    Map<String, Serializable> validateRegistrationAndSendEmail(String requestId,
            Map<String, Serializable> additionnalInfo) throws UserRegistrationException;

    NuxeoPrincipal createUser(CoreSession session, DocumentModel registrationDoc) throws
            UserRegistrationException;

    /**
     * Send a mail to the invited user to revive his invitation If an error occured while sending an email, it logs it
     * and continue.
     *
     * @since 5.6
     */
    void reviveRegistrationRequests(CoreSession session, List<DocumentModel> registrationDocs);

    /**
     * Delete a registration document
     *
     * @since 5.6
     */
    void deleteRegistrationRequests(CoreSession session, List<DocumentModel> registrationDoc);

    UserRegistrationConfiguration getConfiguration();

    /**
     * Retrieve registrations for a document givent the username
     *
     * @since 5.6
     */
    DocumentModelList getRegistrationsForUser(String docId, String username, String configurationName);

    /**
     * Return specific configuration for the specified name
     *
     * @param name configuration name
     * @since 5.6
     */
    UserRegistrationConfiguration getConfiguration(String name);

    /**
     * @since 5.6
     */
    UserRegistrationConfiguration getConfiguration(DocumentModel requestDoc);

    /**
     * Get documentmodel that stores request configuration using RegistrationConfiguration facet.
     *
     * @param session
     * @return
     */
    DocumentModel getRegistrationRulesDocument(CoreSession session, String configurationName);

    /**
     * Stores a resgitration request like submitRegistrationRequest with Document information
     *
     * @return a unique ID for it
     * @since 5.6
     */
    String submitRegistrationRequest(String configurationName, DocumentModel userRegistrationModel,
            Map<String, Serializable> additionnalInfo, ValidationMethod validationMethod, boolean autoAccept)
            throws UserRegistrationException;

    /**
     * Get registration rules adapter
     *
     * @since 5.6
     */
    RegistrationRules getRegistrationRules(String configurationName);

    /**
     * List all registered onfiguration name
     */
    Set<String> getConfigurationsName();

    /**
     * The method checks if the request id is a valid one.
     *
     * @param requestId The value of the request id.
     * @since 5.9.3
     */
    void checkRequestId(String requestId) throws UserRegistrationException;

    /**
     * @return The name of the event when the registration is submitted.
     * @since 5.9.3
     */
    String getNameEventRegistrationSubmitted();

    /**
     * @return The name of the event when the registration is accepted.
     * @since 5.9.3
     */
    String getNameEventRegistrationAccepted();

    /**
     * @return The name of the event when the registration is rejected.
     * @since 5.9.3
     */
    String getNameEventRegistrationRejected();

    /**
     * @return The name of the event when the registration is validated.
     * @since 5.9.3
     */
    String getNameEventRegistrationValidated();

}

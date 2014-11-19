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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface UserInvitationService {

    public static final String REGISTRATION_CONFIGURATION_NAME = "configurationName";
    
    public static final String REGISTRATION_DATA_DOC = "registrationDoc";

    public static final String REGISTRATION_DATA_USER = "registeredUser";

    public enum ValidationMethod {
        EMAIL, NONE
    }

    /**
     * Create a document model for the UserRegistration doctype.
     *
     * @param configurationName The name of the configuration.
     * @throws ClientException
     * @return The document model
     *
     * @since 5.9.3
     */
    DocumentModel getUserRegistrationModel(String configurationName)
            throws ClientException;

    /**
     * Stores a registration request and return a unique ID for it
     *
     * @return
     */
    String submitRegistrationRequest(DocumentModel userRegistrationModel,
            Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept)
            throws ClientException, UserRegistrationException;

    /**
     * accept the registration request
     *
     * @param requestId
     */
    void acceptRegistrationRequest(String requestId,
            Map<String, Serializable> additionnalInfo) throws ClientException,
            UserRegistrationException;

    /**
     * reject the registration request
     *
     * @param requestId
     */
    void rejectRegistrationRequest(String requestId,
            Map<String, Serializable> additionnalInfo) throws ClientException,
            UserRegistrationException;

    /**
     * Validate a registration request and generate the target User
     *
     * @param requestId
     */
    Map<String, Serializable> validateRegistration(String requestId,
            Map<String, Serializable> additionnalInfo) throws ClientException;

    /**
     * Validate a registration request and generate the target User
     *
     * @param requestId
     */
    Map<String, Serializable> validateRegistrationAndSendEmail(
            String requestId, Map<String, Serializable> additionnalInfo)
            throws ClientException, UserRegistrationException;

    NuxeoPrincipal createUser(CoreSession session, DocumentModel registrationDoc)
            throws ClientException, UserRegistrationException;

    /**
     * Send a mail to the invited user to revive his invitation If an error
     * occured while sending an email, it logs it and continue.
     *
     * @since 5.6
     */
    void reviveRegistrationRequests(CoreSession session,
            List<DocumentModel> registrationDocs) throws ClientException;

    /**
     * Delete a registration document
     *
     * @since 5.6
     */
    void deleteRegistrationRequests(CoreSession session,
            List<DocumentModel> registrationDoc) throws ClientException;

    UserRegistrationConfiguration getConfiguration();

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
     * Get documentmodel that stores request configuration using
     * RegistrationConfiguration facet.
     *
     * @param session
     * @return
     */
    DocumentModel getRegistrationRulesDocument(CoreSession session,
            String configurationName) throws ClientException;

    /**
     * Stores a resgitration request like submitRegistrationRequest with
     * Document information
     *
     * @return a unique ID for it
     * @since 5.6
     */
    String submitRegistrationRequest(String configurationName,
            DocumentModel userRegistrationModel,
            Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept)
            throws ClientException, UserRegistrationException;

    /**
     * Get registration rules adapter
     *
     * @since 5.6
     */
    RegistrationRules getRegistrationRules(String configurationName)
            throws ClientException;

    /**
     * List all registered onfiguration name
     */
    Set<String> getConfigurationsName();

    /**
     * The method checks if the request id is a valid one.
     *
     * @param requestId The value of the request id.
     *
     * @since 5.9.3
     */
    void checkRequestId(String requestId) throws ClientException,
            UserRegistrationException;

    /**
     * @return The name of the event when the registration is submitted.
     *
     * @since 5.9.3
     */
    String getNameEventRegistrationSubmitted();

    /**
     * @return The name of the event when the registration is accepted.
     *
     * @since 5.9.3
     */
    String getNameEventRegistrationAccepted();

    /**
     * @return The name of the event when the registration is rejected.
     *
     * @since 5.9.3
     */
    String getNameEventRegistrationRejected();

    /**
     * @return The name of the event when the registration is validated.
     *
     * @since 5.9.3
     */
    String getNameEventRegistrationValidated();
}

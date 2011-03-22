package org.nuxeo.ecm.user.registration;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface UserRegistrationService {

    public enum ValidationMethod {
        EMAIL, NONE
    }

    // events fired by the service impl
    public static final String REGISTRATION_SUBMITTED_EVENT = "registrationSubmitted";
    public static final String REGISTRATION_ACCEPTED_EVENT = "registrationAccepted";
    public static final String REGISTRATION_VALIDATED_EVENT = "registrationValidated";

    /**
     * Stores a registration request and return a unique ID for it
     *
     * @return
     */
    String submitRegistrationRequest(UserRegistrationInfo userInfo, Map<String, Serializable> additionnalInfo, ValidationMethod validationMethod, boolean autoAccept) throws ClientException, UserRegistrationException ;

    /**
     * accept the registration request
     *
     * @param requestId
     */
    void acceptRegistrationRequest(String requestId, Map<String, Serializable> additionnalInfo) throws ClientException, UserRegistrationException ;

    /**
     * Validate a registration request and generate the target User
     *
     * @param requestId
     */
    Map<String, Serializable> validateRegistration(String requestId) throws ClientException, UserRegistrationException ;

    /**
     * Validate a registration request and generate the target User
     *
     * @param requestId
     */
    Map<String, Serializable> validateRegistrationAndSendEmail(String requestId, Map<String, Serializable> additionnalInfo) throws ClientException, UserRegistrationException ;

    NuxeoPrincipal createUser(CoreSession session, DocumentModel registrationDoc) throws ClientException, UserRegistrationException;
}

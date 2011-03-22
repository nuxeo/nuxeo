package org.nuxeo.ecm.user.registration;

import org.nuxeo.ecm.core.api.ClientException;

public class UserRegistrationException extends ClientException {

    private static final long serialVersionUID = 1L;

    public UserRegistrationException(String message) {
        super(message);
    }

}

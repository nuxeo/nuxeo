package org.nuxeo.ecm.user.registration;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class AlreadyProcessedRegistrationException extends UserRegistrationException {
    public AlreadyProcessedRegistrationException(String message) {
        super(message);
    }
}

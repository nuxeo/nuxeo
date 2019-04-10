package org.nuxeo.ecm.user.registration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */

public class RegistrationRules {
    public static final String FACET_REGISTRATION_CONFIGURATION = "RegistrationConfiguration";

    public static final String SCHEMA_REGISTRATION_RULES = "registrationconfiguration";

    public static final String FIELD_ALLOW_USER_CREATION = SCHEMA_REGISTRATION_RULES
            + ":" + "allowUserCreation";

    public static final String FIELD_ALLOW_DIRECT_VALIDATION = SCHEMA_REGISTRATION_RULES
            + ":" + "allowDirectValidationForExistingUser";

    public static final String FIELD_FORCE_RIGHT = SCHEMA_REGISTRATION_RULES
            + ":" + "forceRightAssignment";

    public static final String FIELD_CONFIGURATION_NAME = SCHEMA_REGISTRATION_RULES
            + ":" + "name";

    protected DocumentModel requestContainer;

    private static final Log log = LogFactory.getLog(RegistrationRules.class);

    public RegistrationRules(DocumentModel requestContainer)
            throws ClientException {
        this.requestContainer = requestContainer;
    }

    public boolean allowUserCreation() {
        try {
            return (Boolean) requestContainer.getPropertyValue(FIELD_ALLOW_USER_CREATION);
        } catch (ClientException e) {
            log.warn("Unable to fetch AllowUserCreation flag using default value: "
                    + e.getMessage());
            return true;
        }
    }

    public boolean allowDirectValidationForExistingUser() {
        try {
            return (Boolean) requestContainer.getPropertyValue(FIELD_ALLOW_DIRECT_VALIDATION);
        } catch (ClientException e) {
            log.warn("Unable to fetch AllowDirectValidation flag using default value: "
                    + e.getMessage());
            return false;
        }
    }

    public boolean isForcingRight() {
        try {
            return (Boolean) requestContainer.getPropertyValue(FIELD_FORCE_RIGHT);
        } catch (ClientException e) {
            log.warn("Unable to fetch ForceRight flag using default value: "
                    + e.getMessage());
            return false;
        }
    }
}

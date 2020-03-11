/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 */
package org.nuxeo.ecm.user.invite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */

public class RegistrationRules {
    public static final String FACET_REGISTRATION_CONFIGURATION = "RegistrationConfiguration";

    public static final String SCHEMA_REGISTRATION_RULES = "registrationconfiguration";

    public static final String FIELD_ALLOW_USER_CREATION = SCHEMA_REGISTRATION_RULES + ":" + "allowUserCreation";

    public static final String FIELD_ALLOW_DIRECT_VALIDATION = SCHEMA_REGISTRATION_RULES + ":"
            + "allowDirectValidationForExistingUser";

    public static final String FIELD_FORCE_RIGHT = SCHEMA_REGISTRATION_RULES + ":" + "forceRightAssignment";

    public static final String FIELD_CONFIGURATION_NAME = SCHEMA_REGISTRATION_RULES + ":" + "name";

    public static final String FIELD_DISPLAY_LOCAL_TAB = SCHEMA_REGISTRATION_RULES + ":"
            + "displayLocalRegistrationTab";

    public static final String FORCE_VALIDATION_NON_EXISTING_USER_PROPERTY = "nuxeo.user.registration.force.validation.non.existing";

    protected DocumentModel requestContainer;

    private static final Log log = LogFactory.getLog(RegistrationRules.class);

    public RegistrationRules(DocumentModel requestContainer) {
        this.requestContainer = requestContainer;
    }

    public boolean allowUserCreation() {
        try {
            return (Boolean) requestContainer.getPropertyValue(FIELD_ALLOW_USER_CREATION);
        } catch (PropertyException e) {
            log.warn("Unable to fetch AllowUserCreation flag using default value: " + e.getMessage());
            return true;
        }
    }

    public boolean allowDirectValidationForExistingUser() {
        try {
            return (Boolean) requestContainer.getPropertyValue(FIELD_ALLOW_DIRECT_VALIDATION);
        } catch (PropertyException e) {
            log.warn("Unable to fetch AllowDirectValidation flag using default value: " + e.getMessage());
            return false;
        }
    }

    public boolean isForcingRight() {
        try {
            return (Boolean) requestContainer.getPropertyValue(FIELD_FORCE_RIGHT);
        } catch (PropertyException e) {
            log.warn("Unable to fetch ForceRight flag using default value: " + e.getMessage());
            return false;
        }
    }

    public boolean isDisplayLocalTab() {
        try {
            return (Boolean) requestContainer.getPropertyValue(FIELD_DISPLAY_LOCAL_TAB);
        } catch (PropertyException e) {
            log.warn("Unable to fetch display local tab flag using default value: " + e.getMessage());
            return true;
        }
    }

    public boolean allowDirectValidationForNonExistingUser() {
        return Framework.isBooleanPropertyTrue(FORCE_VALIDATION_NON_EXISTING_USER_PROPERTY);
    }
}

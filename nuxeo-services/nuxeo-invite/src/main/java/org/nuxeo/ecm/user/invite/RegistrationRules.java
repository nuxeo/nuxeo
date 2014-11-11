/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 */
package org.nuxeo.ecm.user.invite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

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

    public static final String FIELD_DISPLAY_LOCAL_TAB = SCHEMA_REGISTRATION_RULES
            + ":" + "displayLocalRegistrationTab";

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

    public boolean isDisplayLocalTab() {
        try {
            return (Boolean) requestContainer.getPropertyValue(FIELD_DISPLAY_LOCAL_TAB);
        } catch (ClientException e) {
            log.warn("Unable to fetch display local tab flag using default value: "
                    + e.getMessage());
            return true;
        }
    }

    public boolean allowDirectValidationForNonExistingUser() {
        return Framework.isBooleanPropertyTrue("nuxeo.user.registration.force.validation.non.existing");
    }
}

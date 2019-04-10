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

import static org.nuxeo.ecm.user.invite.UserRegistrationInfo.EMAIL_FIELD;
import static org.nuxeo.ecm.user.invite.UserRegistrationInfo.USERNAME_FIELD;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.user.invite.RegistrationRules;
import org.nuxeo.ecm.user.invite.UserInvitationComponent;
import org.nuxeo.ecm.user.invite.UserRegistrationConfiguration;
import org.nuxeo.ecm.user.invite.UserRegistrationException;
import org.nuxeo.ecm.user.invite.UserRegistrationInfo;
import org.nuxeo.runtime.api.Framework;

public class UserRegistrationComponent extends UserInvitationComponent
        implements UserRegistrationService {

    protected static Log log = LogFactory.getLog(UserRegistrationService.class);

    private static final String REGISTRATION_SUBMITTED_EVENT = "registrationSubmitted";

    private static final String REGISTRATION_ACCEPTED_EVENT = "registrationAccepted";

    private static final String REGISTRATION_REJECTED_EVENT = "registrationRejected";

    private static final String REGISTRATION_VALIDATED_EVENT = "registrationValidated";

    protected class RegistrationCreator extends UnrestrictedSessionRunner {

        protected UserRegistrationInfo userInfo;

        protected DocumentRegistrationInfo docInfo;

        protected Map<String, Serializable> additionnalInfo;

        protected String registrationUuid;

        protected ValidationMethod validationMethod;

        protected UserRegistrationConfiguration configuration;

        public String getRegistrationUuid() {
            return registrationUuid;
        }

        public RegistrationCreator(String configurationName,
                UserRegistrationInfo userInfo,
                DocumentRegistrationInfo docInfo,
                Map<String, Serializable> additionnalInfo,
                ValidationMethod validationMethod) {
            super(getTargetRepositoryName());
            this.userInfo = userInfo;
            this.additionnalInfo = additionnalInfo;
            this.validationMethod = validationMethod;
            this.docInfo = docInfo;
            this.configuration = getConfiguration(configurationName);
        }

        @Override
        public void run() throws ClientException {

            String title = "registration request for " + userInfo.getLogin()
                    + " (" + userInfo.getEmail() + " " + userInfo.getCompany()
                    + ") ";
            String name = IdUtils.generateId(title + "-"
                    + System.currentTimeMillis());

            String targetPath = getOrCreateRootDocument(session,
                    configuration.getName()).getPathAsString();

            DocumentModel doc = session.createDocumentModel(configuration.getRequestDocType());
            doc.setPathInfo(targetPath, name);
            doc.setPropertyValue("dc:title", title);

            // store userinfo
            doc.setPropertyValue(USERNAME_FIELD, userInfo.getLogin());
            doc.setPropertyValue(UserRegistrationInfo.PASSWORD_FIELD,
                    userInfo.getPassword());
            doc.setPropertyValue(UserRegistrationInfo.FIRSTNAME_FIELD,
                    userInfo.getFirstName());
            doc.setPropertyValue(UserRegistrationInfo.LASTNAME_FIELD,
                    userInfo.getLastName());
            doc.setPropertyValue(EMAIL_FIELD, userInfo.getEmail());
            doc.setPropertyValue(UserRegistrationInfo.COMPANY_FIELD,
                    userInfo.getCompany());

            // validation method
            doc.setPropertyValue("registration:validationMethod",
                    validationMethod.toString());

            // Document info
            doc.setPropertyValue(DocumentRegistrationInfo.DOCUMENT_ID_FIELD,
                    docInfo.getDocumentId());
            doc.setPropertyValue(DocumentRegistrationInfo.DOCUMENT_RIGHT_FIELD,
                    docInfo.getPermission());
            doc.setPropertyValue(DocumentRegistrationInfo.DOCUMENT_TITLE_FIELD,
                    docInfo.getDocumentTitle());

            // additionnal infos
            for (String key : additionnalInfo.keySet()) {
                try {
                    doc.setPropertyValue(key, additionnalInfo.get(key));
                } catch (PropertyException e) {
                    // skip silently
                }
            }

            doc = session.createDocument(doc);

            registrationUuid = doc.getId();

            sendEvent(session, doc, getNameEventRegistrationSubmitted());

            session.save();
        }

    }

    public String submitRegistrationRequest(UserRegistrationInfo userInfo,
            Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept)
            throws ClientException {
        return submitRegistrationRequest(CONFIGURATION_NAME, userInfo,
                new DocumentRegistrationInfo(), additionnalInfo,
                validationMethod, autoAccept);
    }

    @Override
    public String submitRegistrationRequest(String configurationName,
            UserRegistrationInfo userInfo, DocumentRegistrationInfo docInfo,
            Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept)
            throws ClientException, UserRegistrationException {
        RegistrationCreator creator = new RegistrationCreator(
                configurationName, userInfo, docInfo, additionnalInfo,
                validationMethod);
        creator.runUnrestricted();
        String registrationUuid = creator.getRegistrationUuid();

        boolean userAlreadyExists = null != Framework.getLocalService(
                UserManager.class).getPrincipal(userInfo.getLogin());
        // Directly accept registration if the configuration allow it and the
        // user already exists
        RegistrationRules registrationRules = getRegistrationRules(configurationName);
        boolean byPassAdminValidation = autoAccept;
        byPassAdminValidation |= userAlreadyExists
                && registrationRules.allowDirectValidationForExistingUser();
        byPassAdminValidation |= registrationRules.allowDirectValidationForExistingUser()
                && registrationRules.allowDirectValidationForNonExistingUser();
        if (byPassAdminValidation) {
            // Build validationBaseUrl with nuxeo.url property as request is
            // not accessible.
            if (!additionnalInfo.containsKey("enterPasswordUrl")) {
                String baseUrl = Framework.getProperty(NUXEO_URL_KEY);

                baseUrl = StringUtils.isBlank(baseUrl) ? "/" : baseUrl;
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/";
                }
                String enterPasswordUrl = getConfiguration(configurationName).getEnterPasswordUrl();
                if (enterPasswordUrl.startsWith("/")) {
                    enterPasswordUrl = enterPasswordUrl.substring(1);
                }
                additionnalInfo.put("enterPasswordUrl",
                        baseUrl.concat(enterPasswordUrl));
            }
            acceptRegistrationRequest(registrationUuid, additionnalInfo);
        }
        return registrationUuid;
    }

    public Map<String, Serializable> validateRegistrationAndSendEmail(
            String requestId, Map<String, Serializable> additionnalInfo)
            throws ClientException, UserRegistrationException {

        Map<String, Serializable> registrationInfo = validateRegistration(
                requestId, additionnalInfo);

        Map<String, Serializable> input = new HashMap<String, Serializable>();
        input.putAll(registrationInfo);
        input.put("info", (Serializable) additionnalInfo);
        StringWriter writer = new StringWriter();

        UserRegistrationConfiguration configuration = getConfiguration((DocumentModel) registrationInfo.get(REGISTRATION_DATA_DOC));
        try {
            rh.getRenderingEngine().render(
                    configuration.getSuccessEmailTemplate(), input, writer);
        } catch (Exception e) {
            throw new ClientException("Error during rendering email", e);
        }

        String emailAdress = ((NuxeoPrincipalImpl) registrationInfo.get("registeredUser")).getEmail();
        String body = writer.getBuffer().toString();
        String title = configuration.getValidationEmailTitle();
        if (!Framework.isTestModeSet()) {
            try {
                generateMail(emailAdress, null, title, body);
            } catch (Exception e) {
                throw new ClientException("Error while sending mail : ", e);
            }
        } else {
            testRendering = body;
        }

        return registrationInfo;
    }

    @Override
    public void addRightsOnDoc(CoreSession session,
            DocumentModel registrationDoc) throws ClientException {
        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        DocumentModel document = ((DefaultRegistrationUserFactory) getRegistrationUserFactory(configuration)).doAddDocumentPermission(
                session, registrationDoc);
        if (document != null) {
            ((RegistrationUserFactory) getRegistrationUserFactory(configuration)).doPostAddDocumentPermission(
                    session, registrationDoc, document);
        }
    }

    @Override
    public String getNameEventRegistrationSubmitted() {
        return REGISTRATION_SUBMITTED_EVENT;
    }

    @Override
    public String getNameEventRegistrationAccepted() {
        return REGISTRATION_ACCEPTED_EVENT;
    }

    @Override
    public String getNameEventRegistrationRejected() {
        return REGISTRATION_REJECTED_EVENT;
    }

    @Override
    public String getNameEventRegistrationValidated() {
        return REGISTRATION_VALIDATED_EVENT;
    }
}

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
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.naming.NamingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.user.invite.RegistrationRules;
import org.nuxeo.ecm.user.invite.UserInvitationComponent;
import org.nuxeo.ecm.user.invite.UserRegistrationConfiguration;
import org.nuxeo.ecm.user.invite.UserRegistrationException;
import org.nuxeo.ecm.user.invite.UserRegistrationInfo;
import org.nuxeo.runtime.api.Framework;

public class UserRegistrationComponent extends UserInvitationComponent implements UserRegistrationService {

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

        protected String principalName;

        protected ValidationMethod validationMethod;

        protected UserRegistrationConfiguration configuration;

        public String getRegistrationUuid() {
            return registrationUuid;
        }

        public RegistrationCreator(String configurationName, UserRegistrationInfo userInfo,
                DocumentRegistrationInfo docInfo, Map<String, Serializable> additionnalInfo,
                ValidationMethod validationMethod, String principalName) {
            super(getTargetRepositoryName());
            this.userInfo = userInfo;
            this.additionnalInfo = additionnalInfo;
            this.validationMethod = validationMethod;
            this.docInfo = docInfo;
            this.configuration = getConfiguration(configurationName);
            this.principalName = principalName;
        }

        @Override
        public void run() {

            // Check if login is defined - if not define it with email
            userInfo.setLogin(userInfo.getLogin() == null ? userInfo.getEmail() : userInfo.getLogin());

            String title = "registration request for " + userInfo.getLogin() + " (" + userInfo.getEmail() + " "
                    + userInfo.getCompany() + ") ";
            String name = IdUtils.generateId(title + "-" + System.currentTimeMillis(), "-", true, 24);

            String targetPath = getOrCreateRootDocument(session, configuration.getName()).getPathAsString();

            DocumentModel doc = session.createDocumentModel(configuration.getRequestDocType());
            doc.setPathInfo(targetPath, name);
            doc.setPropertyValue("dc:title", title);

            // store userinfo
            doc.setPropertyValue(configuration.getUserInfoUsernameField(), userInfo.getLogin());
            doc.setPropertyValue(configuration.getUserInfoFirstnameField(), userInfo.getFirstName());
            doc.setPropertyValue(configuration.getUserInfoLastnameField(), userInfo.getLastName());
            doc.setPropertyValue(configuration.getUserInfoEmailField(), userInfo.getEmail());
            doc.setPropertyValue(configuration.getUserInfoCompanyField(), userInfo.getCompany());
            doc.setPropertyValue(configuration.getUserInfoGroupsField(), (Serializable) userInfo.getGroups());
            doc.setPropertyValue(configuration.getUserInfoTenantIdField(), userInfo.getTenantId());

            // validation method
            doc.setPropertyValue("registration:validationMethod", validationMethod.toString());

            // Document info
            doc.setPropertyValue(DocumentRegistrationInfo.DOCUMENT_ID_FIELD, docInfo.getDocumentId());
            doc.setPropertyValue(DocumentRegistrationInfo.DOCUMENT_RIGHT_FIELD, docInfo.getPermission());
            doc.setPropertyValue(DocumentRegistrationInfo.DOCUMENT_TITLE_FIELD, docInfo.getDocumentTitle());
            doc.setPropertyValue(DocumentRegistrationInfo.DOCUMENT_BEGIN_FIELD, docInfo.getBegin());
            doc.setPropertyValue(DocumentRegistrationInfo.DOCUMENT_END_FIELD, docInfo.getEnd());

            // additionnal infos
            for (String key : additionnalInfo.keySet()) {
                try {
                    doc.setPropertyValue(key, additionnalInfo.get(key));
                } catch (PropertyException e) {
                    // skip silently
                }
            }

            doc = session.createDocument(doc);

            // Set the ACP for the UserRegistration object
            ACP acp = new ACPImpl();
            ACE denyEverything = new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
            ACE allowEverything = new ACE(principalName, SecurityConstants.EVERYTHING, true);
            ACL acl = new ACLImpl();
            acl.setACEs(new ACE[] { allowEverything, denyEverything });
            acp.addACL(acl);
            doc.setACP(acp, true);

            registrationUuid = doc.getId();

            sendEvent(session, doc, getNameEventRegistrationSubmitted());

            session.save();
        }

    }

    @Override
    public String submitRegistrationRequest(UserRegistrationInfo userInfo, Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept, String principalName) {
        return submitRegistrationRequest(CONFIGURATION_NAME, userInfo, new DocumentRegistrationInfo(), additionnalInfo,
                validationMethod, autoAccept, principalName);
    }

    @Override
    public String submitRegistrationRequest(String configurationName, UserRegistrationInfo userInfo,
            DocumentRegistrationInfo docInfo, Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept, String principalName)
            throws UserRegistrationException {

        // First check that we have the originating user for that request
        if (StringUtils.isEmpty((String) additionnalInfo.get(PARAM_ORIGINATING_USER))) {
            throw new IllegalArgumentException("Originating user should be provided in a registration request");
        }

        RegistrationCreator creator = new RegistrationCreator(configurationName, userInfo, docInfo, additionnalInfo,
                validationMethod, principalName);
        creator.runUnrestricted();
        String registrationUuid = creator.getRegistrationUuid();

        boolean userAlreadyExists = null != Framework.getService(UserManager.class)
                                                     .getPrincipal(userInfo.getLogin());
        // Directly accept registration if the configuration allow it and the
        // user already exists
        RegistrationRules registrationRules = getRegistrationRules(configurationName);
        boolean byPassAdminValidation = autoAccept;
        byPassAdminValidation |= userAlreadyExists && registrationRules.allowDirectValidationForExistingUser();
        byPassAdminValidation |= !userAlreadyExists && registrationRules.allowDirectValidationForNonExistingUser();
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
                additionnalInfo.put("enterPasswordUrl", baseUrl.concat(enterPasswordUrl));
            }
            acceptRegistrationRequest(registrationUuid, additionnalInfo);
        }
        return registrationUuid;
    }

    @Override
    public Map<String, Serializable> validateRegistrationAndSendEmail(String requestId,
            Map<String, Serializable> additionnalInfo) throws UserRegistrationException {

        Map<String, Serializable> registrationInfo = validateRegistration(requestId, additionnalInfo);

        Map<String, Serializable> input = new HashMap<String, Serializable>();
        input.putAll(registrationInfo);
        input.put("info", (Serializable) additionnalInfo);
        StringWriter writer = new StringWriter();

        UserRegistrationConfiguration configuration = getConfiguration(
                (DocumentModel) registrationInfo.get(REGISTRATION_DATA_DOC));
        try {
            rh.getRenderingEngine().render(configuration.getSuccessEmailTemplate(), input, writer);
        } catch (RenderingException e) {
            throw new NuxeoException("Error during rendering email", e);
        }

        String emailAdress = ((NuxeoPrincipalImpl) registrationInfo.get("registeredUser")).getEmail();
        String body = writer.getBuffer().toString();
        String title = configuration.getValidationEmailTitle();
        if (!Framework.isTestModeSet()) {
            try {
                generateMail(emailAdress, null, title, body);
            } catch (NamingException | MessagingException e) {
                throw new NuxeoException("Error while sending mail", e);
            }
        } else {
            testRendering = body;
        }

        return registrationInfo;
    }

    @Override
    public void addRightsOnDoc(CoreSession session, DocumentModel registrationDoc) {
        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        DocumentModel document = ((DefaultRegistrationUserFactory) getRegistrationUserFactory(
                configuration)).doAddDocumentPermission(session, registrationDoc, configuration);
        if (document != null) {
            ((RegistrationUserFactory) getRegistrationUserFactory(configuration)).doPostAddDocumentPermission(session,
                    registrationDoc, document);
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

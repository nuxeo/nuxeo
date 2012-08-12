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

import static org.nuxeo.ecm.user.registration.RegistrationRules.FACET_REGISTRATION_CONFIGURATION;
import static org.nuxeo.ecm.user.registration.RegistrationRules.FIELD_CONFIGURATION_NAME;
import static org.nuxeo.ecm.user.registration.UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;
import static org.nuxeo.ecm.user.registration.UserRegistrationInfo.EMAIL_FIELD;
import static org.nuxeo.ecm.user.registration.UserRegistrationInfo.SCHEMA_NAME;
import static org.nuxeo.ecm.user.registration.UserRegistrationInfo.USERNAME_FIELD;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class UserRegistrationComponent extends DefaultComponent implements
        UserRegistrationService {

    protected static Log log = LogFactory.getLog(UserRegistrationService.class);

    public static final String NUXEO_URL_KEY = "nuxeo.url";

    protected String repoName = null;

    protected String testRendering = null;

    protected RenderingHelper rh = new RenderingHelper();

    protected Map<String, UserRegistrationConfiguration> configurations = new HashMap<String, UserRegistrationConfiguration>();

    // protected UserRegistrationConfiguration configuration;

    public String getTestedRendering() {
        return testRendering;
    }

    protected String getTargetRepositoryName() {

        if (repoName == null) {
            try {
                RepositoryManager rm = Framework.getService(RepositoryManager.class);
                repoName = rm.getDefaultRepository().getName();
            } catch (Exception e) {
                log.error("Error while getting default repository name", e);
                repoName = "default";
            }
        }
        return repoName;
    }

    protected boolean userAlreadyExists(
            UserRegistrationInfo userRegistrationInfo) {
        try {
            DocumentModel user = Framework.getLocalService(UserManager.class).getUserModel(
                    userRegistrationInfo.getLogin());
            return user != null;
        } catch (ClientException e) {
            log.debug(e, e);
            return false;
        }
    }

    protected String getJavaMailJndiName() {
        return Framework.getProperty("jndi.java.mail", "java:/Mail");
    }

    public DocumentModel getRegistrationRulesDocument(CoreSession session,
            String configurationName) throws ClientException {
        // By default, configuration is hold by the root request document
        return getOrCreateRootDocument(session, configurationName);
    }

    protected DocumentModel getOrCreateRootDocument(CoreSession session,
            String configurationName) throws ClientException {
        UserRegistrationConfiguration configuration = getConfiguration(configurationName);

        String targetPath = configuration.getContainerParentPath()
                + configuration.getContainerName();
        DocumentRef targetRef = new PathRef(targetPath);
        DocumentModel root;

        if (!session.exists(targetRef)) {
            root = session.createDocumentModel(configuration.getContainerDocType());
            root.setPathInfo(configuration.getContainerParentPath(),
                    configuration.getContainerName());
            root.setPropertyValue("dc:title", configuration.getContainerTitle());
            // XXX ACLs ?!!!
            root = session.createDocument(root);
        } else {
            root = session.getDocument(targetRef);
        }

        // Add configuration facet
        if (!root.hasFacet(FACET_REGISTRATION_CONFIGURATION)) {
            root.addFacet(FACET_REGISTRATION_CONFIGURATION);
            root.setPropertyValue(FIELD_CONFIGURATION_NAME,
                    configuration.getName());
            root = session.saveDocument(root);
        }
        return root;
    }

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

            sendEvent(session, doc,
                    UserRegistrationService.REGISTRATION_SUBMITTED_EVENT);

            session.save();
        }

    }

    protected class RegistrationAcceptor extends UnrestrictedSessionRunner {

        protected String uuid;

        protected Map<String, Serializable> additionnalInfo;

        public RegistrationAcceptor(String registrationUuid,
                Map<String, Serializable> additionnalInfo) {
            super(getTargetRepositoryName());
            this.uuid = registrationUuid;
            this.additionnalInfo = additionnalInfo;
        }

        @Override
        public void run() throws ClientException {

            DocumentModel doc = session.getDocument(new IdRef(uuid));
            String validationMethod = (String) doc.getPropertyValue("registration:validationMethod");

            // XXX test Validation Method
            sendValidationEmail(additionnalInfo, doc);

            doc.setPropertyValue("registration:accepted", true);
            if (doc.getAllowedStateTransitions().contains("accept")) {
                doc.followTransition("accept");
            }
            doc = session.saveDocument(doc);
            session.save();

            sendEvent(session, doc,
                    UserRegistrationService.REGISTRATION_ACCEPTED_EVENT);
        }
    }

    protected class RegistrationRejector extends UnrestrictedSessionRunner {

        protected String uuid;

        protected Map<String, Serializable> additionnalInfo;

        public RegistrationRejector(String registrationUuid,
                Map<String, Serializable> additionnalInfo) {
            super(getTargetRepositoryName());
            this.uuid = registrationUuid;
            this.additionnalInfo = additionnalInfo;
        }

        @Override
        public void run() throws ClientException {

            DocumentModel doc = session.getDocument(new IdRef(uuid));

            doc.setPropertyValue("registration:accepted", false);
            if (doc.getAllowedStateTransitions().contains("reject")) {
                doc.followTransition("reject");
            }
            doc = session.saveDocument(doc);
            session.save();

            sendEvent(session, doc,
                    UserRegistrationService.REGISTRATION_REJECTED_EVENT);
        }
    }

    protected class RegistrationValidator extends UnrestrictedSessionRunner {

        protected String uuid;

        protected Map<String, Serializable> registrationData = new HashMap<String, Serializable>();

        protected Map<String, Serializable> additionnalInfo;

        public RegistrationValidator(String uuid,
                Map<String, Serializable> additionnalInfo) {
            super(getTargetRepositoryName());
            this.uuid = uuid;
            this.additionnalInfo = additionnalInfo;
        }

        public Map<String, Serializable> getRegistrationData() {
            return registrationData;
        }

        @Override
        public void run() throws ClientException {
            DocumentRef idRef = new IdRef(uuid);

            if (!session.exists(idRef)) {
                throw new UserRegistrationException(
                        "There is no existing registration request with id "
                                + uuid);
            }

            DocumentModel registrationDoc = session.getDocument(idRef);

            // additionnal infos
            for (String key : additionnalInfo.keySet()) {
                try {
                    registrationDoc.setPropertyValue(key,
                            additionnalInfo.get(key));
                } catch (PropertyException e) {
                    // skip silently
                }
            }

            if (registrationDoc.getLifeCyclePolicy().equals(
                    "registrationRequest")) {
                if (registrationDoc.getCurrentLifeCycleState().equals(
                        "accepted")) {
                    registrationDoc.followTransition("validate");
                } else {
                    if (registrationDoc.getCurrentLifeCycleState().equals(
                            "validated")) {
                        throw new AlreadyProcessedRegistrationException(
                                "Registration request has already been processed.");
                    } else {
                        throw new UserRegistrationException(
                                "Registration request has not been accepted yet.");
                    }
                }
            }

            session.saveDocument(registrationDoc);
            session.save();
            EventContext evContext = sendEvent(session, registrationDoc,
                    UserRegistrationService.REGISTRATION_VALIDATED_EVENT);

            ((DocumentModelImpl) registrationDoc).detach(sessionIsAlreadyUnrestricted);
            registrationData.put(REGISTRATION_DATA_DOC, registrationDoc);
            registrationData.put(REGISTRATION_DATA_USER,
                    evContext.getProperty("registeredUser"));
        }

    }

    protected EventContext sendEvent(CoreSession session, DocumentModel source,
            String evName) throws UserRegistrationException {
        try {
            EventService evService = Framework.getService(EventService.class);
            EventContext evContext = new DocumentEventContext(session,
                    session.getPrincipal(), source);

            Event event = evContext.newEvent(evName);

            evService.fireEvent(event);

            return evContext;
        } catch (UserRegistrationException ue) {
            log.warn("Error during event processing", ue);
            throw ue;
        } catch (Exception e) {
            log.error("Error while sending event", e);
            return null;
        }

    }

    protected void sendValidationEmail(
            Map<String, Serializable> additionnalInfo,
            DocumentModel registrationDoc) throws ClientException {

        String emailAdress = (String) registrationDoc.getPropertyValue(EMAIL_FIELD);

        Map<String, Serializable> input = new HashMap<String, Serializable>();
        input.put(REGISTRATION_DATA_DOC, registrationDoc);
        input.put("info", (Serializable) additionnalInfo);
        input.put("userAlreadyExists",
                checkUserFromRegistrationExistence(registrationDoc));
        StringWriter writer = new StringWriter();

        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        try {
            rh.getRenderingEngine().render(
                    configuration.getValidationEmailTemplate(), input, writer);
        } catch (Exception e) {
            throw new ClientException("Error during rendering email", e);
        }

        String body = writer.getBuffer().toString();
        String title = configuration.getValidationEmailTitle();
        String copyTo = (String) registrationDoc.getPropertyValue("registration:copyTo");
        if (!Framework.isTestModeSet()) {
            try {
                generateMail(emailAdress, copyTo, title, body);
            } catch (Exception e) {
                throw new ClientException("Error while sending mail : ", e);
            }
        } else {
            testRendering = body;
        }

    }

    protected boolean checkUserFromRegistrationExistence(
            DocumentModel registrationDoc) throws ClientException {
        return null != Framework.getLocalService(UserManager.class).getPrincipal(
                (String) registrationDoc.getPropertyValue(USERNAME_FIELD));
    }

    protected void generateMail(String destination, String copy, String title,
            String content) throws Exception {

        InitialContext ic = new InitialContext();
        Session session = (Session) ic.lookup(getJavaMailJndiName());

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(session.getProperty("mail.from")));
        msg.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse((String) destination, false));
        if (!StringUtils.isBlank(copy)) {
            msg.addRecipient(Message.RecipientType.CC, new InternetAddress(
                    copy, false));
        }

        msg.setSubject(title, "UTF-8");
        msg.setSentDate(new Date());
        msg.setContent(content, "text/html; charset=utf-8");

        Transport.send(msg);

    }

    public String submitRegistrationRequest(UserRegistrationInfo userInfo,
            Map<String, Serializable> additionnalInfo,
            ValidationMethod validationMethod, boolean autoAccept)
            throws ClientException {
        return submitRegistrationRequest(DEFAULT_CONFIGURATION_NAME, userInfo,
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
            // Build validationBaseUrl with nuxeo.url property as request is not
            // accessible.
            if (!additionnalInfo.containsKey("validationBaseUrl")) {
                String baseUrl = Framework.getProperty(NUXEO_URL_KEY);
                Path path = new Path(StringUtils.isBlank(baseUrl) ? "/"
                        : baseUrl);
                path = path.append(getConfiguration(configurationName).getValidationRelUrl());
                additionnalInfo.put("validationBaseURL", path.toString());
            }
            acceptRegistrationRequest(registrationUuid, additionnalInfo);
        }
        return registrationUuid;
    }

    public void acceptRegistrationRequest(String requestId,
            Map<String, Serializable> additionnalInfo) throws ClientException,
            UserRegistrationException {
        RegistrationAcceptor acceptor = new RegistrationAcceptor(requestId,
                additionnalInfo);
        acceptor.runUnrestricted();

    }

    public void rejectRegistrationRequest(String requestId,
            Map<String, Serializable> additionnalInfo) throws ClientException,
            UserRegistrationException {

        RegistrationRejector rejector = new RegistrationRejector(requestId,
                additionnalInfo);
        rejector.runUnrestricted();

    }

    public Map<String, Serializable> validateRegistration(String requestId,
            Map<String, Serializable> additionnalInfo) throws ClientException,
            UserRegistrationException {
        RegistrationValidator validator = new RegistrationValidator(requestId,
                additionnalInfo);
        validator.runUnrestricted();
        return validator.getRegistrationData();
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
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("configuration".equals(extensionPoint)) {
            UserRegistrationConfiguration newConfig = (UserRegistrationConfiguration) contribution;

            if (configurations.containsKey(newConfig.getName())) {
                if (newConfig.isMerge()) {
                    configurations.get(newConfig.getName()).mergeWith(newConfig);
                } else if (newConfig.isRemove()) {
                    configurations.remove(newConfig.getName());
                } else {
                    log.warn("Trying to register an existing userRegistration configuration without removing or merging it, in: "
                            + contributor.getName());
                }
            } else {
                configurations.put(newConfig.getName(), newConfig);
            }
        }
    }

    protected RegistrationUserFactory getRegistrationUserFactory(
            UserRegistrationConfiguration configuration) {
        RegistrationUserFactory factory = null;
        Class<? extends RegistrationUserFactory> factoryClass = configuration.getRegistrationUserFactory();
        if (factoryClass != null) {
            try {
                factory = factoryClass.newInstance();
            } catch (InstantiationException e) {
                log.warn("Failed to instanciate RegistrationUserFactory", e);
            } catch (IllegalAccessException e) {
                log.warn("Failed to instanciate RegistrationUserFactory", e);
            }
        }
        if (factory == null) {
            factory = new DefaultRegistrationUserFactory();
        }
        return factory;
    }

    @Override
    public NuxeoPrincipal createUser(CoreSession session,
            DocumentModel registrationDoc) throws ClientException,
            UserRegistrationException {
        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        return getRegistrationUserFactory(configuration).createUser(session,
                registrationDoc);
    }

    @Override
    public void addRightsOnDoc(CoreSession session,
            DocumentModel registrationDoc) throws ClientException {
        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        DocumentModel document = getRegistrationUserFactory(configuration).doAddDocumentPermission(
                session, registrationDoc);
        if (document != null) {
            getRegistrationUserFactory(configuration).doPostAddDocumentPermission(
                    session, registrationDoc, document);
        }
    }

    protected class RootDocumentGetter extends UnrestrictedSessionRunner {

        protected DocumentModel doc;

        protected String configurationName;

        protected RootDocumentGetter(String configurationName) {
            super(getTargetRepositoryName());
            this.configurationName = configurationName;
        }

        @Override
        public void run() throws ClientException {
            doc = getOrCreateRootDocument(session, configurationName);
            ((DocumentModelImpl) doc).detach(true);
        }

        public DocumentModel getDoc() {
            return doc;
        }
    }

    @Override
    public UserRegistrationConfiguration getConfiguration() {
        return getConfiguration(DEFAULT_CONFIGURATION_NAME);
    }

    @Override
    public UserRegistrationConfiguration getConfiguration(
            DocumentModel requestDoc) {
        try {
            DocumentModel parent = requestDoc.getCoreSession().getDocument(
                    requestDoc.getParentRef());
            String configurationName = DEFAULT_CONFIGURATION_NAME;
            if (parent.hasFacet(FACET_REGISTRATION_CONFIGURATION)) {
                configurationName = (String) parent.getPropertyValue(FIELD_CONFIGURATION_NAME);
            }

            if (!configurations.containsKey(configurationName)) {
                throw new ClientException("Configuration " + configurationName
                        + " is not registered");
            }
            return configurations.get(configurationName);
        } catch (ClientException e) {
            log.info("Unable to get request parent document: " + e.getMessage());
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public UserRegistrationConfiguration getConfiguration(String name) {
        if (!configurations.containsKey(name)) {
            throw new ClientRuntimeException(
                    "Trying to get unknown user registration configuration.");
        }
        return configurations.get(name);
    }

    @Override
    public RegistrationRules getRegistrationRules(String configurationName)
            throws ClientException {
        RootDocumentGetter rdg = new RootDocumentGetter(configurationName);
        rdg.runUnrestricted();
        return rdg.getDoc().getAdapter(RegistrationRules.class);
    }

    @Override
    public void reviveRegistrationRequests(CoreSession session,
            List<DocumentModel> registrationDocs) throws ClientException {
        for (DocumentModel registrationDoc : registrationDocs) {
            reviveRegistrationRequest(session, registrationDoc,
                    new HashMap<String, Object>());
        }
    }

    protected void reviveRegistrationRequest(CoreSession session,
            DocumentModel registrationDoc, Map<String, Object> additionalInfos)
            throws ClientException {
        StringWriter writer = new StringWriter();
        Map<String, Object> input = new HashMap<String, Object>();
        additionalInfos.put("validationBaseURL", BaseURL.getBaseURL()
                + getConfiguration(registrationDoc).getValidationRelUrl());
        input.put("info", additionalInfos);
        input.put("userAlreadyExists",
                checkUserFromRegistrationExistence(registrationDoc));
        input.put(REGISTRATION_DATA_DOC, registrationDoc);

        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        try {
            rh.getRenderingEngine().render(
                    configuration.getReviveEmailTemplate(), input, writer);
        } catch (RenderingException e) {
            throw new ClientException("Error during templating email : ", e);
        }

        String emailAdress = (String) registrationDoc.getPropertyValue(EMAIL_FIELD);
        String body = writer.getBuffer().toString();
        String title = configuration.getReviveEmailTitle();

        if (!Framework.isTestModeSet()) {
            try {
                generateMail(emailAdress, null, title, body);
            } catch (Exception e) {
                throw new ClientException("Error while sending mail : ", e);
            }
        } else {
            testRendering = body;
        }
    }

    @Override
    public void deleteRegistrationRequests(CoreSession session,
            List<DocumentModel> registrationDocs) throws ClientException {
        for (DocumentModel registration : registrationDocs) {
            if (!registration.hasSchema(SCHEMA_NAME)) {
                throw new ClientException(
                        "Registration document do not contains needed schema");
            }

            String userName = (String) registration.getPropertyValue(USERNAME_FIELD);
            session.removeDocument(registration.getRef());
        }
    }

    @Override
    public Set<String> getConfigurationsName() {
        return configurations.keySet();
    }

    @Override
    public DocumentModelList getRegistrationsForUser(final String docId,
            final String username) throws ClientException {
        final DocumentModelList registrationDocs = new DocumentModelListImpl();
        new UnrestrictedSessionRunner(getTargetRepositoryName()) {
            @Override
            public void run() throws ClientException {
                String query = "SELECT * FROM Document WHERE ecm:mixinType = 'UserRegistration' AND docinfo:documentId = '%s' AND userinfo:login = '%s' AND ecm:isCheckedInVersion = 0";
                query = String.format(query, docId, username);
                registrationDocs.addAll(session.query(query));
            }
        }.runUnrestricted();
        return registrationDocs;
    }
}

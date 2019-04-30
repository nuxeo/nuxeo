/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.user.invite;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.nuxeo.ecm.user.invite.RegistrationRules.FACET_REGISTRATION_CONFIGURATION;
import static org.nuxeo.ecm.user.invite.RegistrationRules.FIELD_CONFIGURATION_NAME;
import static org.nuxeo.ecm.user.invite.UserInvitationService.ValidationMethod.EMAIL;
import static org.nuxeo.ecm.user.invite.UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class UserInvitationComponent extends DefaultComponent implements UserInvitationService {

    public static final String PARAM_ORIGINATING_USER = "registration:originatingUser";

    protected static Log log = LogFactory.getLog(UserInvitationService.class);

    public static final String NUXEO_URL_KEY = "nuxeo.url";

    protected String repoName = null;

    protected String testRendering = null;

    protected RenderingHelper rh = new RenderingHelper();

    protected Map<String, UserRegistrationConfiguration> configurations = new HashMap<String, UserRegistrationConfiguration>();

    private static final String INVITATION_SUBMITTED_EVENT = "invitationSubmitted";

    private static final String INVITATION_ACCEPTED_EVENT = "invitationAccepted";

    private static final String INVITATION_REJECTED_EVENT = "invitationRejected";

    private static final String INVITATION_VALIDATED_EVENT = "invitationValidated";

    public String getTestedRendering() {
        return testRendering;
    }

    protected String getTargetRepositoryName() {
        if (repoName == null) {
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            repoName = rm.getDefaultRepositoryName();
        }
        return repoName;
    }

    protected boolean userAlreadyExists(UserRegistrationInfo userRegistrationInfo) {
        DocumentModel user = Framework.getLocalService(UserManager.class).getUserModel(userRegistrationInfo.getLogin());
        return user != null;
    }

    protected String getJavaMailJndiName() {
        return Framework.getProperty("jndi.java.mail", "java:/Mail");
    }

    @Override
    public DocumentModel getUserRegistrationModel(String configurationName) {
        // Test if the configuration is defined
        if (StringUtils.isEmpty(configurationName)) {
            configurationName = DEFAULT_CONFIGURATION_NAME;
        }
        // Get the DocumentModel for the doctype defined in the configuration
        UserRegistrationModelCreator creator = new UserRegistrationModelCreator(configurationName);
        creator.runUnrestricted();
        return creator.getUserRegistrationModel();
    }

    @Override
    public DocumentModel getRegistrationRulesDocument(CoreSession session, String configurationName) {
        // By default, configuration is hold by the root request document
        return getOrCreateRootDocument(session, configurationName);
    }

    public DocumentModel getOrCreateRootDocument(CoreSession session, String configurationName) {
        UserRegistrationConfiguration configuration = getConfiguration(configurationName);

        String targetPath = configuration.getContainerParentPath() + configuration.getContainerName();
        DocumentRef targetRef = new PathRef(targetPath);
        DocumentModel root;

        if (!session.exists(targetRef)) {
            root = session.createDocumentModel(configuration.getContainerDocType());
            root.setPathInfo(configuration.getContainerParentPath(), configuration.getContainerName());
            root.setPropertyValue("dc:title", configuration.getContainerTitle());
            // XXX ACLs ?!!!
            root = session.createDocument(root);
        } else {
            root = session.getDocument(targetRef);
        }

        // Add configuration facet
        if (!root.hasFacet(FACET_REGISTRATION_CONFIGURATION)) {
            root.addFacet(FACET_REGISTRATION_CONFIGURATION);
            root.setPropertyValue(FIELD_CONFIGURATION_NAME, configuration.getName());
            root = session.saveDocument(root);
        }
        return root;
    }

    protected class UserRegistrationModelCreator extends UnrestrictedSessionRunner {

        DocumentModel userRegistrationModel;

        protected UserRegistrationConfiguration configuration;

        public UserRegistrationModelCreator(String configurationName) {
            super(getTargetRepositoryName());
            configuration = getConfiguration(configurationName);
        }

        @Override
        public void run() {
            userRegistrationModel = session.createDocumentModel(configuration.getRequestDocType());
        }

        public DocumentModel getUserRegistrationModel() {
            return userRegistrationModel;
        }
    }

    protected class RegistrationCreator extends UnrestrictedSessionRunner {

        protected Map<String, Serializable> additionnalInfo;

        protected String registrationUuid;

        protected ValidationMethod validationMethod;

        protected DocumentModel userRegistrationModel;

        protected UserRegistrationConfiguration configuration;

        public String getRegistrationUuid() {
            return registrationUuid;
        }

        public RegistrationCreator(String configurationName, DocumentModel userRegistrationModel,
                Map<String, Serializable> additionnalInfo, ValidationMethod validationMethod) {
            super(getTargetRepositoryName());
            this.userRegistrationModel = userRegistrationModel;
            this.additionnalInfo = additionnalInfo;
            this.validationMethod = validationMethod;
            configuration = getConfiguration(configurationName);
        }

        @Override
        public void run() {

            String title = "registration request for "
                    + userRegistrationModel.getPropertyValue(configuration.getUserInfoUsernameField()) + " ("
                    + userRegistrationModel.getPropertyValue(configuration.getUserInfoEmailField()) + " "
                    + userRegistrationModel.getPropertyValue(configuration.getUserInfoCompanyField()) + ") ";
            PathSegmentService pss = Framework.getLocalService(PathSegmentService.class);
            String name = pss.generatePathSegment(title + "-" + System.currentTimeMillis());

            String targetPath = getOrCreateRootDocument(session, configuration.getName()).getPathAsString();

            userRegistrationModel.setPathInfo(targetPath, name);
            userRegistrationModel.setPropertyValue("dc:title", title);

            // validation method
            userRegistrationModel.setPropertyValue("registration:validationMethod", validationMethod.toString());

            // additionnal infos
            if (additionnalInfo != null && !additionnalInfo.isEmpty()) {
                for (String key : additionnalInfo.keySet()) {
                    try {
                        userRegistrationModel.setPropertyValue(key, additionnalInfo.get(key));
                    } catch (PropertyException e) {
                        // skip silently
                    }
                }
            }

            userRegistrationModel = session.createDocument(userRegistrationModel);

            registrationUuid = userRegistrationModel.getId();

            sendEvent(session, userRegistrationModel, getNameEventRegistrationSubmitted());

            session.save();
        }

    }

    protected class RegistrationApprover extends UnrestrictedSessionRunner {

        private final UserManager userManager;

        protected String uuid;

        protected Map<String, Serializable> additionnalInfo;

        public RegistrationApprover(String registrationUuid, Map<String, Serializable> additionnalInfo) {
            super(getTargetRepositoryName());
            uuid = registrationUuid;
            this.additionnalInfo = additionnalInfo;
            this.userManager = Framework.getLocalService(UserManager.class);
        }

        @Override
        public void run() {

            DocumentModel doc = session.getDocument(new IdRef(uuid));
            String validationMethod = (String) doc.getPropertyValue("registration:validationMethod");

            NuxeoPrincipal targetPrincipal = userManager.getPrincipal((String) doc.getPropertyValue("userinfo:login"));
            if (targetPrincipal != null) {
                throw new UserAlreadyExistsException();
            }

            targetPrincipal = userManager.getPrincipal((String) doc.getPropertyValue("userinfo:email"));
            if (targetPrincipal != null) {
                DocumentModel target = session.getDocument(
                        new IdRef((String) doc.getPropertyValue("docinfo:documentId")));
                ACP acp = target.getACP();
                Map<String, Serializable> contextData = new HashMap<>();
                contextData.put("notify", true);
                contextData.put("comment", doc.getPropertyValue("registration:comment"));
                acp.addACE(ACL.LOCAL_ACL,
                        ACE.builder(targetPrincipal.getName(), (String) doc.getPropertyValue("docinfo:permission"))
                           .creator((String) doc.getPropertyValue("docinfo:creator"))
                           .contextData(contextData)
                           .build());
                target.setACP(acp, true);
                // test Validation Method
            } else if (StringUtils.equals(EMAIL.toString(), validationMethod)) {
                sendValidationEmail(additionnalInfo, doc);
            }

            doc.setPropertyValue("registration:accepted", true);
            if (doc.getAllowedStateTransitions().contains("approve")) {
                doc.followTransition("approve");
            }
            doc = session.saveDocument(doc);
            session.save();

            sendEvent(session, doc, getNameEventRegistrationAccepted());
        }
    }

    protected class RegistrationRejector extends UnrestrictedSessionRunner {

        protected String uuid;

        protected Map<String, Serializable> additionnalInfo;

        public RegistrationRejector(String registrationUuid, Map<String, Serializable> additionnalInfo) {
            super(getTargetRepositoryName());
            uuid = registrationUuid;
            this.additionnalInfo = additionnalInfo;
        }

        @Override
        public void run() {

            DocumentModel doc = session.getDocument(new IdRef(uuid));

            doc.setPropertyValue("registration:accepted", false);
            if (doc.getAllowedStateTransitions().contains("reject")) {
                doc.followTransition("reject");
            }
            doc = session.saveDocument(doc);
            session.save();

            sendEvent(session, doc, getNameEventRegistrationRejected());
        }
    }

    protected class RegistrationAcceptator extends UnrestrictedSessionRunner {

        protected String uuid;

        protected Map<String, Serializable> registrationData = new HashMap<String, Serializable>();

        protected Map<String, Serializable> additionnalInfo;

        public RegistrationAcceptator(String uuid, Map<String, Serializable> additionnalInfo) {
            super(getTargetRepositoryName());
            this.uuid = uuid;
            this.additionnalInfo = additionnalInfo;
        }

        public Map<String, Serializable> getRegistrationData() {
            return registrationData;
        }

        @Override
        public void run() {
            DocumentRef idRef = new IdRef(uuid);

            DocumentModel registrationDoc = session.getDocument(idRef);

            // additionnal infos
            for (String key : additionnalInfo.keySet()) {
                try {
                    if (DefaultInvitationUserFactory.PASSWORD_KEY.equals(key)) {
                        // add the password as a transient context data
                        registrationDoc.putContextData(DefaultInvitationUserFactory.PASSWORD_KEY,
                                additionnalInfo.get(key));
                    } else {
                        registrationDoc.setPropertyValue(key, additionnalInfo.get(key));
                    }
                } catch (PropertyException e) {
                    // skip silently
                }
            }

            NuxeoPrincipal principal = null;
            if (registrationDoc.getLifeCyclePolicy().equals("registrationRequest")) {
                if (registrationDoc.getCurrentLifeCycleState().equals("approved")) {
                    try {
                        UserInvitationService userRegistrationService = Framework.getService(
                                UserInvitationService.class);
                        UserRegistrationConfiguration config = userRegistrationService.getConfiguration(
                                registrationDoc);
                        RegistrationRules rules = userRegistrationService.getRegistrationRules(config.getName());
                        if (rules.allowUserCreation()) {
                            principal = userRegistrationService.createUser(session, registrationDoc);
                        }
                        registrationDoc.followTransition("accept");
                    } catch (NuxeoException e) {
                        e.addInfo("Unable to complete registration");
                        throw e;
                    }
                } else {
                    if (registrationDoc.getCurrentLifeCycleState().equals("accepted")) {
                        throw new AlreadyProcessedRegistrationException(
                                "Registration request has already been processed");
                    } else {
                        throw new UserRegistrationException("Registration request has not been accepted yet");
                    }
                }
            }

            session.saveDocument(registrationDoc);
            session.save();
            sendEvent(session, registrationDoc, getNameEventRegistrationValidated());
            registrationDoc.detach(sessionIsAlreadyUnrestricted);
            registrationData.put(REGISTRATION_DATA_DOC, registrationDoc);
            registrationData.put(REGISTRATION_DATA_USER, principal);
        }

    }

    protected class RequestIdValidator extends UnrestrictedSessionRunner {

        protected String uuid;

        public RequestIdValidator(String uuid) {
            super(getTargetRepositoryName());
            this.uuid = uuid;
        }

        @Override
        public void run() {
            DocumentRef idRef = new IdRef(uuid);
            // Check if the id matches an existing document
            if (!session.exists(idRef)) {
                throw new UserRegistrationException("There is no existing registration request with id " + uuid);
            }

            // Check if the request has not been already validated
            DocumentModel registrationDoc = session.getDocument(idRef);
            if (registrationDoc.getCurrentLifeCycleState().equals("accepted")) {
                throw new AlreadyProcessedRegistrationException("Registration request has already been processed");
            }
        }
    }

    protected EventContext sendEvent(CoreSession session, DocumentModel source, String evName)
            throws UserRegistrationException {
        try {
            EventService evService = Framework.getService(EventService.class);
            EventContext evContext = new DocumentEventContext(session, session.getPrincipal(), source);

            Event event = evContext.newEvent(evName);

            evService.fireEvent(event);

            return evContext;
        } catch (UserRegistrationException ue) {
            log.warn("Error during event processing", ue);
            throw ue;
        }

    }

    protected void sendValidationEmail(Map<String, Serializable> additionnalInfo, DocumentModel registrationDoc) {
        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        sendEmail(additionnalInfo, registrationDoc, configuration.getValidationEmailTemplate(),
                configuration.getValidationEmailTitle());
    }

    protected void sendEmail(Map<String, Serializable> additionnalInfo, DocumentModel registrationDoc,
            String emailTemplatePath, String emailTitle) {
        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);

        String emailAdress = (String) registrationDoc.getPropertyValue(configuration.getUserInfoEmailField());

        Map<String, Serializable> input = new HashMap<String, Serializable>();
        Map<String, Serializable> userinfo = new HashMap<String, Serializable>();
        userinfo.put("firstName", registrationDoc.getPropertyValue(configuration.getUserInfoFirstnameField()));
        userinfo.put("lastName", registrationDoc.getPropertyValue(configuration.getUserInfoLastnameField()));
        userinfo.put("login", registrationDoc.getPropertyValue(configuration.getUserInfoUsernameField()));
        userinfo.put("id", registrationDoc.getId());

        String documentTitle = "";

        if (registrationDoc.hasSchema("docinfo")) {
            documentTitle = (String) registrationDoc.getPropertyValue("docinfo:documentTitle");
        }
        input.put("documentTitle", documentTitle);
        input.put("configurationName", configuration.getName());
        input.put("comment", registrationDoc.getPropertyValue("registration:comment"));
        input.put(UserInvitationService.REGISTRATION_CONFIGURATION_NAME, configuration.getName());
        input.put("userinfo", (Serializable) userinfo);
        input.put("info", (Serializable) additionnalInfo);
        input.put("userAlreadyExists", checkUserFromRegistrationExistence(registrationDoc));
        input.put("productName", Framework.getProperty("org.nuxeo.ecm.product.name"));
        StringWriter writer = new StringWriter();

        try {
            rh.getRenderingEngine().render(emailTemplatePath, input, writer);
        } catch (RenderingException e) {
            throw new NuxeoException("Error during rendering email", e);
        }

        // render custom email subject
        emailTitle = renderSubjectTemplate(emailTitle, input);

        String body = writer.getBuffer().toString();
        String copyTo = (String) registrationDoc.getPropertyValue("registration:copyTo");
        if (!isTestModeSet()) {
            try {
                generateMail(emailAdress, copyTo, emailTitle, body);
            } catch (NamingException | MessagingException e) {
                throw new NuxeoException("Error while sending mail: ", e);
            }
        } else {
            testRendering = body;
        }
    }

    private String renderSubjectTemplate(String emailTitle, Map<String, Serializable> input) {
        Configuration stringCfg = rh.getEngineConfiguration();
        Writer out;
        try {
            Template templ = new Template("subjectTemplate", new StringReader(emailTitle), stringCfg);
            out = new StringWriter();
            templ.process(input, out);
            out.flush();
        } catch (IOException | TemplateException e) {
            throw new NuxeoException("Error while rendering email subject: ", e);
        }
        return out.toString();
    }

    protected static boolean isTestModeSet() {
        return Framework.isTestModeSet() || !isBlank(Framework.getProperty("org.nuxeo.ecm.tester.name"));
    }

    protected boolean checkUserFromRegistrationExistence(DocumentModel registrationDoc) {
        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        return null != Framework.getLocalService(UserManager.class).getPrincipal(
                (String) registrationDoc.getPropertyValue(configuration.getUserInfoUsernameField()));
    }

    protected void generateMail(String destination, String copy, String title, String content)
            throws NamingException, MessagingException {

        InitialContext ic = new InitialContext();
        Session session = (Session) ic.lookup(getJavaMailJndiName());

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(session.getProperty("mail.from")));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destination, false));
        if (!isBlank(copy)) {
            msg.addRecipient(Message.RecipientType.CC, new InternetAddress(copy, false));
        }

        msg.setSubject(title, "UTF-8");
        msg.setSentDate(new Date());
        msg.setContent(content, "text/html; charset=utf-8");

        Transport.send(msg);
    }

    @Override
    public String submitRegistrationRequest(DocumentModel userRegistrationModel,
            Map<String, Serializable> additionnalInfo, ValidationMethod validationMethod, boolean autoAccept) {
        return submitRegistrationRequest(DEFAULT_CONFIGURATION_NAME, userRegistrationModel, additionnalInfo,
                validationMethod, autoAccept);
    }

    @Override
    public DocumentModelList getRegistrationsForUser(final String docId, final String username,
            final String configurationName) {
        final DocumentModelList registrationDocs = new DocumentModelListImpl();
        new UnrestrictedSessionRunner(getTargetRepositoryName()) {
            @Override
            public void run() {
                String query = "SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'validated' AND"
                        + " ecm:mixinType = '" + getConfiguration(configurationName).getRequestDocType()
                        + "' AND docinfo:documentId = '%s' AND"
                        + getConfiguration(configurationName).getUserInfoUsernameField()
                        + " = '%s' AND ecm:isCheckedInVersion = 0";
                query = String.format(query, docId, username);
                registrationDocs.addAll(session.query(query));
            }
        }.runUnrestricted();
        return registrationDocs;
    }

    protected static boolean isEmailExist(UserRegistrationConfiguration configuration, DocumentModel userRegistration) {
        String email = (String) userRegistration.getPropertyValue(configuration.getUserInfoEmailField());
        if (isBlank(email)) {
            return false;
        }

        Map<String, Serializable> filter = new HashMap<>(1);
        filter.put(UserConfig.EMAIL_COLUMN, email);

        DocumentModelList users = Framework.getLocalService(UserManager.class).searchUsers(filter, null);
        return !users.isEmpty();
    }

    @Override
    public String submitRegistrationRequest(String configurationName, DocumentModel userRegistrationModel,
            Map<String, Serializable> additionnalInfo, ValidationMethod validationMethod, boolean autoAccept) {

        // First check that we have the originating user for that request
        if (StringUtils.isBlank((String)additionnalInfo.get(PARAM_ORIGINATING_USER))) {
            throw new IllegalArgumentException("Originating user should be provided in a registration request");
        }
        RegistrationCreator creator = new RegistrationCreator(configurationName, userRegistrationModel, additionnalInfo,
                validationMethod);
        creator.runUnrestricted();
        String registrationUuid = creator.getRegistrationUuid();

        UserRegistrationConfiguration currentConfig = getConfiguration(configurationName);
        boolean userAlreadyExists = null != Framework.getLocalService(UserManager.class).getPrincipal(
                (String) userRegistrationModel.getPropertyValue(currentConfig.getUserInfoUsernameField()));

        if (!userAlreadyExists && isEmailExist(currentConfig, userRegistrationModel)) {
            log.info("Trying to submit a registration from an existing email with a different username.");
            throw new UserAlreadyExistsException();
        }

        // Directly accept registration if the configuration allow it and the
        // user already exists
        RegistrationRules registrationRules = getRegistrationRules(configurationName);
        boolean byPassAdminValidation = autoAccept;
        byPassAdminValidation |= userAlreadyExists && registrationRules.allowDirectValidationForExistingUser();
        byPassAdminValidation |= !userAlreadyExists && registrationRules.allowDirectValidationForNonExistingUser();
        if (byPassAdminValidation) {
            // Build validationBaseUrl with nuxeo.url property as request is not
            // accessible.
            if (!additionnalInfo.containsKey("enterPasswordUrl")) {
                additionnalInfo.put("enterPasswordUrl", buildEnterPasswordUrl(currentConfig));
            }
            acceptRegistrationRequest(registrationUuid, additionnalInfo);
        }
        return registrationUuid;
    }

    protected String buildEnterPasswordUrl(UserRegistrationConfiguration configuration) {
        String baseUrl = Framework.getProperty(NUXEO_URL_KEY);

        baseUrl = isBlank(baseUrl) ? "/" : baseUrl;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl.concat(configuration.getEnterPasswordUrl());
    }

    @Override
    public void acceptRegistrationRequest(String requestId, Map<String, Serializable> additionnalInfo)
            throws UserRegistrationException {
        RegistrationApprover acceptor = new RegistrationApprover(requestId, additionnalInfo);
        acceptor.runUnrestricted();

    }

    @Override
    public void rejectRegistrationRequest(String requestId, Map<String, Serializable> additionnalInfo)
            throws UserRegistrationException {

        RegistrationRejector rejector = new RegistrationRejector(requestId, additionnalInfo);
        rejector.runUnrestricted();

    }

    @Override
    public Map<String, Serializable> validateRegistration(String requestId, Map<String, Serializable> additionnalInfo)
            throws UserRegistrationException {
        RegistrationAcceptator validator = new RegistrationAcceptator(requestId, additionnalInfo);
        validator.runUnrestricted();
        return validator.getRegistrationData();
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
                throw new NuxeoException("Error while sending mail : ", e);
            }
        } else {
            testRendering = body;
        }

        return registrationInfo;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("configuration".equals(extensionPoint)) {
            UserRegistrationConfiguration newConfig = (UserRegistrationConfiguration) contribution;

            if (configurations.containsKey(newConfig.getName())) {
                if (newConfig.isMerge()) {
                    configurations.get(newConfig.getName()).mergeWith(newConfig);
                } else if (newConfig.isRemove()) {
                    configurations.remove(newConfig.getName());
                } else {
                    log.warn(
                            "Trying to register an existing userRegistration configuration without removing or merging it, in: "
                                    + contributor.getName());
                }
            } else {
                configurations.put(newConfig.getName(), newConfig);
            }
        }
    }

    protected InvitationUserFactory getRegistrationUserFactory(UserRegistrationConfiguration configuration) {
        InvitationUserFactory factory = null;
        Class<? extends InvitationUserFactory> factoryClass = configuration.getRegistrationUserFactory();
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
            factory = new DefaultInvitationUserFactory();
        }
        return factory;
    }

    @Override
    public NuxeoPrincipal createUser(CoreSession session, DocumentModel registrationDoc)
            throws UserRegistrationException {
        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        return getRegistrationUserFactory(configuration).doCreateUser(session, registrationDoc, configuration);
    }

    protected class RootDocumentGetter extends UnrestrictedSessionRunner {

        protected DocumentModel doc;

        protected String configurationName;

        protected RootDocumentGetter(String configurationName) {
            super(getTargetRepositoryName());
            this.configurationName = configurationName;
        }

        @Override
        public void run() {
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
    public UserRegistrationConfiguration getConfiguration(DocumentModel requestDoc) {
        try {
            DocumentModel parent = requestDoc.getCoreSession().getDocument(requestDoc.getParentRef());
            String configurationName = DEFAULT_CONFIGURATION_NAME;
            if (parent.hasFacet(FACET_REGISTRATION_CONFIGURATION)) {
                configurationName = (String) parent.getPropertyValue(FIELD_CONFIGURATION_NAME);
            } else if (requestDoc.hasFacet(FACET_REGISTRATION_CONFIGURATION)) {
                configurationName = (String) requestDoc.getPropertyValue(FIELD_CONFIGURATION_NAME);
            }

            if (!configurations.containsKey(configurationName)) {
                throw new NuxeoException("Configuration " + configurationName + " is not registered");
            }
            return configurations.get(configurationName);
        } catch (NuxeoException e) {
            log.info("Unable to get request parent document: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public UserRegistrationConfiguration getConfiguration(String name) {
        if (!configurations.containsKey(name)) {
            throw new NuxeoException("Trying to get unknown user registration configuration.");
        }
        return configurations.get(name);
    }

    @Override
    public RegistrationRules getRegistrationRules(String configurationName) {
        RootDocumentGetter rdg = new RootDocumentGetter(configurationName);
        rdg.runUnrestricted();
        return rdg.getDoc().getAdapter(RegistrationRules.class);
    }

    @Override
    public void reviveRegistrationRequests(CoreSession session, List<DocumentModel> registrationDocs) {
        for (DocumentModel registrationDoc : registrationDocs) {
            reviveRegistrationRequest(session, registrationDoc, new HashMap<String, Serializable>());
        }
    }

    protected void reviveRegistrationRequest(CoreSession session, DocumentModel registrationDoc,
            Map<String, Serializable> additionalInfos) {
        UserRegistrationConfiguration configuration = getConfiguration(registrationDoc);
        // Build validationBaseUrl with nuxeo.url property as request is not
        // accessible.
        if (!additionalInfos.containsKey("enterPasswordUrl")) {
            additionalInfos.put("enterPasswordUrl", buildEnterPasswordUrl(configuration));
        }
        sendEmail(additionalInfos, registrationDoc, configuration.getReviveEmailTemplate(),
                configuration.getReviveEmailTitle());
    }

    @Override
    public void deleteRegistrationRequests(CoreSession session, List<DocumentModel> registrationDocs) {
        for (DocumentModel registration : registrationDocs) {
            UserRegistrationConfiguration configuration = getConfiguration(registration);
            if (!registration.hasSchema(configuration.getUserInfoSchemaName())) {
                throw new NuxeoException("Registration document do not contains needed schema");
            }

            session.removeDocument(registration.getRef());
        }
    }

    @Override
    public Set<String> getConfigurationsName() {
        return configurations.keySet();
    }

    @Override
    public void checkRequestId(final String requestId) throws UserRegistrationException {
        RequestIdValidator runner = new RequestIdValidator(requestId);
        runner.runUnrestricted();
    }

    @Override
    public String getNameEventRegistrationSubmitted() {
        return INVITATION_SUBMITTED_EVENT;
    }

    @Override
    public String getNameEventRegistrationAccepted() {
        return INVITATION_ACCEPTED_EVENT;
    }

    @Override
    public String getNameEventRegistrationRejected() {
        return INVITATION_REJECTED_EVENT;
    }

    @Override
    public String getNameEventRegistrationValidated() {
        return INVITATION_VALIDATED_EVENT;
    }

}

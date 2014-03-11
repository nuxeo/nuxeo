package org.nuxeo.ecm.user.registration.webengine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.user.registration.AlreadyProcessedRegistrationException;
import org.nuxeo.ecm.user.registration.UserRegistrationException;
import org.nuxeo.ecm.user.registration.UserRegistrationInfo;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
@Path("/userRegistration")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "userRegistration")
public class UserRegistrationObject extends ModuleRoot {
    private static final Log log = LogFactory.getLog(UserRegistrationObject.class);

    @POST
    @Path("validate")
    public Object validateTrialForm() throws Exception {
        UserRegistrationService usr = Framework.getLocalService(UserRegistrationService.class);

        FormData formData = getContext().getForm();
        String requestId = formData.getString("RequestId");
        String password = formData.getString("Password");
        String passwordConfirmation = formData.getString("PasswordConfirmation");
        if (password == null || "".equals(password.trim())) {
            return redisplayFormWithErrorMessage("EnterPassword",
                    ctx.getMessage("label.registerForm.validation.password"),
                    formData);
        }
        if (passwordConfirmation == null
                || "".equals(passwordConfirmation.trim())) {
            return redisplayFormWithErrorMessage(
                    "EnterPassword",
                    ctx.getMessage("label.registerForm.validation.passwordconfirmation"),
                    formData);
        }
        password = password.trim();
        passwordConfirmation = passwordConfirmation.trim();
        if (!password.equals(passwordConfirmation)) {
            return redisplayFormWithErrorMessage(
                    "EnterPassword",
                    ctx.getMessage("label.registerForm.validation.passwordvalidation"),
                    formData);
        }
        Map<String, Serializable> registrationData = new HashMap<String, Serializable>();
        try {
            Map<String, Serializable> additionalInfo = buildAdditionalInfos();

            // Add the entered password to the document model
            additionalInfo.put(UserRegistrationInfo.PASSWORD_FIELD, password);
            // Validate the creation of the user
            registrationData = usr.validateRegistration(requestId, additionalInfo);

            /*DocumentModel regDoc = (DocumentModel) registrationData.get(REGISTRATION_DATA_DOC);
            String docId = (String) regDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_ID_FIELD);
            if (!StringUtils.isEmpty(docId)) {
                redirectUrl = new DocumentUrlFinder(docId).getDocumentUrl();
            }*/
        } catch (AlreadyProcessedRegistrationException ape) {
            log.info("Try to validate an already processed registration");
        } catch (UserRegistrationException ue) {
            log.warn("Unable to validate registration request", ue);
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ue.getMessage());
        } catch (ClientException e) {
            log.error("Error while validating registration request", e);
            return getView("ValidationErrorTemplate").arg("error", e);
        }
        // User redirected to the logout page after validating the password
        String logoutUrl = "/" + BaseURL.getWebAppName() + "/logout";
        return getView("UserCreated").arg("data", registrationData).arg("logout", logoutUrl);
    }

    @GET
    @Path("enterpassword/{requestId}")
    public Object validatePasswordForm(@PathParam("requestId")
    String requestId) throws Exception {

        // Check if the requestId is an existing one
        UserRegistrationService usr = Framework.getLocalService(UserRegistrationService.class);
        try {
            usr.checkRequestId(requestId);
        } catch (UserRegistrationException ue) {
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ue.getMessage());
        }

        Map<String, String> data = new HashMap<String, String>();
        return getView("EnterPassword").arg("key", requestId).arg("data", data);
    }

    protected Map<String, Serializable> buildAdditionalInfos() {
        return new HashMap<String, Serializable>();
    }

    protected Template redisplayFormWithMessage(String messageType,
            String formName, String message, FormData data) {
        Map<String, String> savedData = new HashMap<String, String>();
        for (String key : data.getKeys()) {
            savedData.put(key, data.getString(key));
        }
        return getView(formName).arg("data", savedData).arg(messageType,
                message);
    }

    protected Template redisplayFormWithInfoMessage(String formName,
            String message, FormData data) {
        return redisplayFormWithMessage("info", formName, message, data);
    }

    protected Template redisplayFormWithErrorMessage(String formName,
            String message, FormData data) {
        return redisplayFormWithMessage("err", formName, message, data);
    }

    protected class DocumentUrlFinder extends UnrestrictedSessionRunner {

        protected String docId;

        protected String documentUrl;

        public DocumentUrlFinder(String docId) throws Exception {
            super(Framework.getService(RepositoryManager.class).getDefaultRepositoryName());
            this.docId = docId;
        }

        public String getDocumentUrl() throws ClientException {
            runUnrestricted();
            return documentUrl;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel target = session.getDocument(new IdRef(docId));
            documentUrl = DocumentModelFunctions.documentUrl(null, target,
                    "view_documents", null, true, ctx.getRequest());
        }
    }
}

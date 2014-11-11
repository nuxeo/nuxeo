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
package org.nuxeo.ecm.webengine.invite;

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
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.user.invite.AlreadyProcessedRegistrationException;
import org.nuxeo.ecm.user.invite.UserInvitationService;
import org.nuxeo.ecm.user.invite.UserRegistrationException;
import org.nuxeo.ecm.user.invite.UserRegistrationInfo;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
@Path("/userInvitation")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "userRegistration")
public class UserInvitationObject extends ModuleRoot {
    private static final Log log = LogFactory.getLog(UserInvitationObject.class);

    @POST
    @Path("validate")
    public Object validateTrialForm() throws Exception {
        UserInvitationService usr = fetchService();

        FormData formData = getContext().getForm();
        String requestId = formData.getString("RequestId");
        String password = formData.getString("Password");
        String passwordConfirmation = formData.getString("PasswordConfirmation");

        // Check if the requestId is an existing one
        try {
            usr.checkRequestId(requestId);
        } catch (AlreadyProcessedRegistrationException ape) {
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ctx.getMessage("label.error.requestAlreadyProcessed"));
        } catch (UserRegistrationException ue) {
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ctx.getMessage("label.error.requestNotExisting", requestId));
        }

        // Check if both entered passwords are correct
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
            registrationData = usr.validateRegistration(requestId,
                    additionalInfo);

            /*
             * DocumentModel regDoc = (DocumentModel)
             * registrationData.get(REGISTRATION_DATA_DOC); String docId =
             * (String)
             * regDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_ID_FIELD
             * ); if (!StringUtils.isEmpty(docId)) { redirectUrl = new
             * DocumentUrlFinder(docId).getDocumentUrl(); }
             */
        } catch (AlreadyProcessedRegistrationException ape) {
            log.info("Try to validate an already processed registration");
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ctx.getMessage("label.error.requestAlreadyProcessed"));
        } catch (UserRegistrationException ue) {
            log.warn("Unable to validate registration request", ue);
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ctx.getMessage("label.errror.requestNotAccepted"));
        } catch (ClientException e) {
            log.error("Error while validating registration request", e);
            return getView("ValidationErrorTemplate").arg("error", e);
        }
        // User redirected to the logout page after validating the password
        String webappName = VirtualHostHelper.getWebAppName(getContext().getRequest());
        String logoutUrl = "/" + webappName + "/logout";
        return getView("UserCreated").arg("data", registrationData).arg(
                "logout", logoutUrl);
    }

    protected UserInvitationService fetchService() {
        UserInvitationService usr = Framework.getLocalService(UserInvitationService.class);
        return usr;
    }

    @GET
    @Path("enterpassword/{requestId}")
    public Object validatePasswordForm(@PathParam("requestId")
    String requestId) throws Exception {

        UserInvitationService usr = fetchService();
        try {
            usr.checkRequestId(requestId);
        } catch (AlreadyProcessedRegistrationException ape) {
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ctx.getMessage("label.error.requestAlreadyProcessed"));
        } catch (UserRegistrationException ue) {
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ctx.getMessage("label.error.requestNotExisting", requestId));
        }

        Map<String, String> data = new HashMap<String, String>();
        data.put("RequestId", requestId);
        return getView("EnterPassword").arg("data", data);
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

}

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
import org.nuxeo.ecm.platform.usermanager.exceptions.InvalidPasswordException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.user.invite.AlreadyProcessedRegistrationException;
import org.nuxeo.ecm.user.invite.DefaultInvitationUserFactory;
import org.nuxeo.ecm.user.invite.UserInvitationService;
import org.nuxeo.ecm.user.invite.UserRegistrationException;
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
    public Object validateTrialForm() {
        UserInvitationService usr = fetchService();

        FormData formData = getContext().getForm();
        String requestId = formData.getString("RequestId");
        formData.getString("ConfigurationName");
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
                    ctx.getMessage("label.registerForm.validation.password"), formData);
        }
        if (passwordConfirmation == null || "".equals(passwordConfirmation.trim())) {
            return redisplayFormWithErrorMessage("EnterPassword",
                    ctx.getMessage("label.registerForm.validation.passwordconfirmation"), formData);
        }
        password = password.trim();
        passwordConfirmation = passwordConfirmation.trim();
        if (!password.equals(passwordConfirmation)) {
            return redisplayFormWithErrorMessage("EnterPassword",
                    ctx.getMessage("label.registerForm.validation.passwordvalidation"), formData);
        }
        Map<String, Serializable> registrationData = new HashMap<>();
        try {
            Map<String, Serializable> additionalInfo = buildAdditionalInfos();

            // Add the entered password to the document model
            additionalInfo.put(DefaultInvitationUserFactory.PASSWORD_KEY, password);
            // Validate the creation of the user
            registrationData = usr.validateRegistration(requestId, additionalInfo);

        } catch (AlreadyProcessedRegistrationException ape) {
            log.info("Try to validate an already processed registration");
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ctx.getMessage("label.error.requestAlreadyProcessed"));
        } catch (UserRegistrationException ue) {
            log.warn("Unable to validate registration request", ue);
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                    ctx.getMessage("label.errror.requestNotAccepted"));
        } catch (InvalidPasswordException ive) {
            return getView("ValidationErrorTemplate").arg("exceptionMsg",
                ctx.getMessage("label.registerForm.validation.invalidpassword"));
        }
        // User redirected to the logout page after validating the password
        String webappName = VirtualHostHelper.getWebAppName(getContext().getRequest());
        String logoutUrl = "/" + webappName + "/logout";
        return getView("UserCreated").arg("data", registrationData).arg("logout", logoutUrl);
    }

    protected UserInvitationService fetchService() {
        UserInvitationService usr = Framework.getService(UserInvitationService.class);
        return usr;
    }

    @GET
    @Path("enterpassword/{configurationName}/{requestId}")
    public Object validatePasswordForm(@PathParam("requestId") String requestId,
            @PathParam("configurationName") String configurationName) {

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

        Map<String, String> data = new HashMap<>();
        data.put("RequestId", requestId);
        data.put("ConfigurationName", configurationName);
        String webappName = VirtualHostHelper.getWebAppName(getContext().getRequest());
        String validationRelUrl = usr.getConfiguration(configurationName).getValidationRelUrl();
        String valUrl = "/" + webappName + "/" + validationRelUrl;
        data.put("ValidationUrl", valUrl);
        return getView("EnterPassword").arg("data", data);
    }

    protected Map<String, Serializable> buildAdditionalInfos() {
        return new HashMap<>();
    }

    protected Template redisplayFormWithMessage(String messageType, String formName, String message, FormData data) {
        Map<String, String> savedData = new HashMap<>();
        for (String key : data.getKeys()) {
            savedData.put(key, data.getString(key));
        }
        return getView(formName).arg("data", savedData).arg(messageType, message);
    }

    protected Template redisplayFormWithInfoMessage(String formName, String message, FormData data) {
        return redisplayFormWithMessage("info", formName, message, data);
    }

    protected Template redisplayFormWithErrorMessage(String formName, String message, FormData data) {
        return redisplayFormWithMessage("err", formName, message, data);
    }

}

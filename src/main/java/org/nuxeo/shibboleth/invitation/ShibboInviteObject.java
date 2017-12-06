/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *  Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.shibboleth.invitation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.user.invite.AlreadyProcessedRegistrationException;
import org.nuxeo.ecm.user.invite.DefaultInvitationUserFactory;
import org.nuxeo.ecm.user.invite.UserInvitationService;
import org.nuxeo.ecm.user.invite.UserRegistrationException;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

@Path("/shibboInvite")
@WebObject(type = "shibboInvite")
@Produces("text/html;charset=UTF-8")
public class ShibboInviteObject extends ModuleRoot {
    private static final Log log = LogFactory.getLog(ShibboInviteObject.class);

    @POST
    @Path("validate")
    public Object validateTrialForm(@FormParam("isShibbo") boolean isShibbo) {
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

        if (!isShibbo) {
            // Check if both entered passwords are correct
            if (password == null || "".equals(password.trim())) {
                return redisplayFormWithErrorMessage("EnterPassword",
                        ctx.getMessage("label.registerForm.validation.password"), formData);

            }
            if (passwordConfirmation == null || "".equals(passwordConfirmation.trim()) && !isShibbo) {
                return redisplayFormWithErrorMessage("EnterPassword",
                        ctx.getMessage("label.registerForm.validation.passwordconfirmation"), formData);
            }
            password = password.trim();
            passwordConfirmation = passwordConfirmation.trim();
            if (!password.equals(passwordConfirmation) && !isShibbo) {
                return redisplayFormWithErrorMessage("EnterPassword",
                        ctx.getMessage("label.registerForm.validation.passwordvalidation"), formData);
            }
        }
        Map<String, Serializable> registrationData;
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
        }
        // User redirected to the logout page after validating the password
        String webappName = VirtualHostHelper.getWebAppName(getContext().getRequest());
        String redirectUrl = "/" + webappName + "/";
        if (!isShibbo) {
            redirectUrl += "logout";
        }
        return getView("UserCreated").arg("redirectUrl", redirectUrl)
                                     .arg("data", registrationData)
                                     .arg("isShibbo", isShibbo);
    }

    protected UserInvitationService fetchService() {
        return Framework.getLocalService(UserRegistrationService.class);
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

        Map<String, String> data = new HashMap<String, String>();
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
        Map<String, String> savedData = new HashMap<String, String>();
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

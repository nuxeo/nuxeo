/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml.processor.action;

import static org.nuxeo.ecm.platform.auth.saml.SAMLAuthenticationProvider.ERROR_AUTH;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.setLoginError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.opensaml.profile.action.AbstractConditionalProfileAction;
import org.opensaml.profile.context.PreviousEventContext;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

/**
 * @since 2023.0
 */
public class HandleEventContextErrorAction extends AbstractConditionalProfileAction {

    private static final Logger log = LogManager.getLogger(HandleEventContextErrorAction.class);

    public HandleEventContextErrorAction() {
        // enable it only on error
        setActivationCondition(prc -> prc.getSubcontext(PreviousEventContext.class) != null);
    }

    @Override
    protected void doExecute(@NotNull ProfileRequestContext profileRequestContext) {
        var request = HttpServletRequestResponseContext.getRequest();
        var eventContext = profileRequestContext.getSubcontext(PreviousEventContext.class);
        log.info("Error processing SAML message, reason: {}", eventContext::getEvent);
        setLoginError(request, ERROR_AUTH);
    }
}

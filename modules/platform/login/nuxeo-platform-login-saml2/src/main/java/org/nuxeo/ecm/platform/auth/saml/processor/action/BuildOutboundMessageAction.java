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

import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jetbrains.annotations.NotNull;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.SAMLBindingSupport;

import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

/**
 * @since 2023.0
 */
public class BuildOutboundMessageAction extends AbstractProfileAction {

    protected final Function<MessageContext, SAMLObject> buildRequest;

    public BuildOutboundMessageAction(Function<MessageContext, SAMLObject> buildRequest) {
        this.buildRequest = buildRequest;
    }

    @Override
    protected void doExecute(@NotNull ProfileRequestContext profileRequestContext) {
        var context = profileRequestContext.getOutboundMessageContext();

        var request = buildRequest.apply(context);
        context.setMessage(request);

        // Store the requested URL in the Relay State
        String requestedUrl = getRequestedUrl(HttpServletRequestResponseContext.getRequest());
        if (requestedUrl != null) {
            SAMLBindingSupport.setRelayState(context, requestedUrl);
        }
        profileRequestContext.setOutboundMessageContext(context);
    }

    protected String getRequestedUrl(HttpServletRequest request) {
        String requestedUrl = (String) request.getAttribute(NXAuthConstants.REQUESTED_URL);
        if (requestedUrl == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                requestedUrl = (String) session.getAttribute(NXAuthConstants.START_PAGE_SAVE_KEY);
            }
        }
        return requestedUrl;
    }
}

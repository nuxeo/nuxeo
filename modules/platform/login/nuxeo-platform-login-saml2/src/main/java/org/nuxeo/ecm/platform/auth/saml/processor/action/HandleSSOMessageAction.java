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

import org.jetbrains.annotations.NotNull;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.nuxeo.ecm.platform.auth.saml.processor.messaging.SAMLAssertionsContentContext;
import org.opensaml.profile.action.AbstractConditionalProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLSelfEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLSubjectNameIdentifierContext;

import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

/**
 * @since 2023.0
 */
public class HandleSSOMessageAction extends AbstractConditionalProfileAction {

    @Override
    protected void doExecute(@NotNull ProfileRequestContext profileRequestContext) {
        var request = HttpServletRequestResponseContext.getRequest();

        var inboundMessageContext = profileRequestContext.getInboundMessageContext();
        var assertionsContentContext = inboundMessageContext.getSubcontext(SAMLAssertionsContentContext.class, true);

        var nameID = inboundMessageContext.getSubcontext(SAMLSubjectNameIdentifierContext.class, true)
                                          .getSAML2SubjectNameID();
        var sessionIndexes = assertionsContentContext.getSessionIndexes();
        var remoteEntityId = inboundMessageContext.getSubcontext(SAMLPeerEntityContext.class).getEntityId();
        var relayState = inboundMessageContext.getSubcontext(SAMLBindingContext.class).getRelayState();
        var attributes = assertionsContentContext.getAttributes();
        var localEntityId = inboundMessageContext.getSubcontext(SAMLSelfEntityContext.class).getEntityId();
        var credential = new SAMLCredential(nameID, sessionIndexes, remoteEntityId, relayState, attributes,
                localEntityId);
        request.setAttribute("SAMLCredential", credential);
    }
}

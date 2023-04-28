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
package org.nuxeo.ecm.platform.auth.saml.processor;

import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.buildSAMLObject;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.getStartPageURL;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.newUUID;

import java.time.Instant;
import java.util.List;

import org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration;
import org.nuxeo.ecm.platform.auth.saml.processor.action.BuildOutboundMessageAction;
import org.nuxeo.ecm.platform.auth.saml.processor.action.EncodeOutboundResponseAction;
import org.nuxeo.ecm.platform.auth.saml.processor.action.InvokeInitInboundHandlerAction;
import org.nuxeo.ecm.platform.auth.saml.processor.action.SetEndpointAction;
import org.nuxeo.ecm.platform.auth.saml.processor.binding.SAMLOutboundBinding;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.profile.action.ProfileAction;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

/**
 * @since 2023.0
 */
public class WebSSOOutboundProcessor extends AbstractSAMLProcessor {

    /**
     * Identifier of the WebSSO profile.
     */
    public static final String PROFILE_URI = "urn:oasis:names:tc:SAML:2.0:profiles:SSO:browser";

    protected final MessageHandler initInboundHandler;

    protected final MessageHandler outboundHandler;

    protected final SAMLOutboundBinding outboundBinding;

    public WebSSOOutboundProcessor(MessageHandler initInboundHandler, MessageHandler outboundHandler,
            SAMLOutboundBinding outboundBinding) {
        this.initInboundHandler = initInboundHandler;
        this.outboundHandler = outboundHandler;
        this.outboundBinding = outboundBinding;
    }

    @Override
    protected List<ProfileAction> getActions() {
        return List.of( //
                new InvokeInitInboundHandlerAction(initInboundHandler), //
                new SetEndpointAction(IDPSSODescriptor::getSingleSignOnServices, outboundBinding), //
                new BuildOutboundMessageAction(this::buildAuthnRequest), //
                new EncodeOutboundResponseAction(outboundBinding, outboundHandler) //
        );
    }

    protected AuthnRequest buildAuthnRequest(MessageContext ctx) {
        var endpoint = ctx.getSubcontext(SAMLPeerEntityContext.class)
                          .getSubcontext(SAMLEndpointContext.class)
                          .getEndpoint();
        var request = HttpServletRequestResponseContext.getRequest();

        AuthnRequest authnRequest = buildSAMLObject(AuthnRequest.DEFAULT_ELEMENT_NAME);
        authnRequest.setID(newUUID());
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setIssueInstant(Instant.now());
        authnRequest.setDestination(endpoint.getLocation());
        // Let the IdP pick a protocol binding
        // authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);

        // Fill the assertion consumer URL
        authnRequest.setAssertionConsumerServiceURL(getStartPageURL(request));

        Issuer issuer = buildSAMLObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(SAMLConfiguration.getEntityId());
        authnRequest.setIssuer(issuer);

        NameIDPolicy nameIDPolicy = buildSAMLObject(NameIDPolicy.DEFAULT_ELEMENT_NAME);
        nameIDPolicy.setFormat(NameIDType.UNSPECIFIED);
        authnRequest.setNameIDPolicy(nameIDPolicy);

        return authnRequest;
    }
}

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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.buildSAMLObject;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.getSAMLSessionCookie;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.newUUID;

import java.time.Instant;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration;
import org.nuxeo.ecm.platform.auth.saml.SAMLUtils.SAMLSessionCookie;
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
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

import net.shibboleth.utilities.java.support.net.HttpServletRequestResponseContext;

/**
 * @since 2023.0
 */
public class SLOOutboundProcessor extends AbstractSAMLProcessor {

    /**
     * Identifier of the SLO profile.
     */
    public static final String PROFILE_URI = "urn:oasis:names:tc:SAML:2.0:profiles:SSO:logout";

    protected final MessageHandler initInboundHandler;

    protected final MessageHandler outboundHandler;

    protected final SAMLOutboundBinding outboundBinding;

    public SLOOutboundProcessor(MessageHandler initInboundHandler, MessageHandler outboundHandler,
            SAMLOutboundBinding outboundBinding) {
        this.initInboundHandler = initInboundHandler;
        this.outboundHandler = outboundHandler;
        this.outboundBinding = outboundBinding;
    }

    @Override
    protected List<ProfileAction> getActions() {
        return List.of( //
                new InvokeInitInboundHandlerAction(initInboundHandler), //
                new SetEndpointAction(IDPSSODescriptor::getSingleLogoutServices, outboundBinding), //
                new BuildOutboundMessageAction(this::buildLogoutRequest), //
                new EncodeOutboundResponseAction(outboundBinding, outboundHandler) //
        );
    }

    protected LogoutRequest buildLogoutRequest(MessageContext ctx) {
        var endpoint = ctx.getSubcontext(SAMLPeerEntityContext.class)
                          .getSubcontext(SAMLEndpointContext.class)
                          .getEndpoint();
        var request = HttpServletRequestResponseContext.getRequest();
        var samlSessionCookie = getSAMLSessionCookie(request).orElseThrow(
                () -> new NuxeoException("Unable to retrieve the SAML Session Cookie"));

        LogoutRequest logoutRequest = buildSAMLObject(LogoutRequest.DEFAULT_ELEMENT_NAME);
        logoutRequest.setID(newUUID());
        logoutRequest.setVersion(SAMLVersion.VERSION_20);
        logoutRequest.setIssueInstant(Instant.now());
        logoutRequest.setDestination(endpoint.getLocation());

        Issuer issuer = buildSAMLObject(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(SAMLConfiguration.getEntityId());
        logoutRequest.setIssuer(issuer);
        logoutRequest.setNameID(buildNameID(samlSessionCookie));

        // Add session indexes
        SessionIndex index = buildSAMLObject(SessionIndex.DEFAULT_ELEMENT_NAME);
        index.setValue(samlSessionCookie.sessionId());
        logoutRequest.getSessionIndexes().add(index);

        return logoutRequest;
    }

    protected NameID buildNameID(SAMLSessionCookie sessionCookie) {
        // Retrieve the credential from cookie
        String nameValue = sessionCookie.nameValue();
        String nameFormat = sessionCookie.nameFormat();
        if (isBlank(nameFormat) || "null".equals(nameFormat)) {
            nameFormat = NameIDType.UNSPECIFIED;
        }
        NameID nameID = buildSAMLObject(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue(nameValue);
        nameID.setFormat(nameFormat);
        return nameID;
    }
}

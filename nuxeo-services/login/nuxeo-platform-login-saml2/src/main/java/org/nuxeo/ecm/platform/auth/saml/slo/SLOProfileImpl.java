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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.slo;

import org.joda.time.DateTime;
import org.nuxeo.ecm.platform.auth.saml.AbstractSAMLProfile;
import org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.xml.encryption.DecryptionException;

/**
 * WebSLO (Single Log Out) profile implementation.
 *
 * @since 6.0
 */
public class SLOProfileImpl extends AbstractSAMLProfile implements SLOProfile {

    public SLOProfileImpl(SingleLogoutService slo) {
        super(slo);
    }

    @Override
    public String getProfileIdentifier() {
        return PROFILE_URI;
    }

    public LogoutRequest buildLogoutRequest(SAMLMessageContext context, SAMLCredential credential) throws SAMLException {

        LogoutRequest request = build(LogoutRequest.DEFAULT_ELEMENT_NAME);
        request.setID(newUUID());
        request.setVersion(SAMLVersion.VERSION_20);
        request.setIssueInstant(new DateTime());
        request.setDestination(getEndpoint().getLocation());

        Issuer issuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(SAMLConfiguration.getEntityId());
        request.setIssuer(issuer);

        // Add session indexes
        if (credential.getSessionIndexes() == null || credential.getSessionIndexes().isEmpty()) {
            throw new SAMLException("No session indexes found");
        }
        for (String sessionIndex : credential.getSessionIndexes()) {
            SessionIndex index = build(SessionIndex.DEFAULT_ELEMENT_NAME);
            index.setSessionIndex(sessionIndex);
            request.getSessionIndexes().add(index);
        }

        request.setNameID(credential.getNameID());

        return request;

    }

    public boolean processLogoutRequest(SAMLMessageContext context, SAMLCredential credential) throws SAMLException {

        SAMLObject message = context.getInboundSAMLMessage();

        // Verify type
        if (message == null || !(message instanceof LogoutRequest)) {
            throw new SAMLException("Message is not of a LogoutRequest object type");
        }

        LogoutRequest request = (LogoutRequest) message;

        // Validate signature of the response if present
        if (request.getSignature() != null) {
            log.debug("Verifying message signature");
            validateSignature(request.getSignature(), context.getPeerEntityId());
            context.setInboundSAMLMessageAuthenticated(true);
        }

        // TODO - Validate destination

        // Validate issuer
        if (request.getIssuer() != null) {
            log.debug("Verifying issuer of the message");
            Issuer issuer = request.getIssuer();
            validateIssuer(issuer, context);
        }

        // TODO - Validate issue time

        // Get and validate the NameID
        NameID nameID;
        if (getDecrypter() != null && request.getEncryptedID() != null) {
            try {
                nameID = (NameID) getDecrypter().decrypt(request.getEncryptedID());
            } catch (DecryptionException e) {
                throw new SAMLException("Failed to decrypt NameID", e);
            }
        } else {
            nameID = request.getNameID();
        }

        if (nameID == null) {
            throw new SAMLException("The requested NameID is invalid");
        }

        // If no index is specified do logout
        if (request.getSessionIndexes() == null || request.getSessionIndexes().isEmpty()) {
            return true;
        }

        // Else check if this is on of our session indexes
        for (SessionIndex sessionIndex : request.getSessionIndexes()) {
            if (credential.getSessionIndexes().contains(sessionIndex.getSessionIndex())) {
                return true;
            }
        }

        return false;
    }

    public void processLogoutResponse(SAMLMessageContext context) throws SAMLException {

        SAMLObject message = context.getInboundSAMLMessage();

        if (!(message instanceof LogoutResponse)) {
            throw new SAMLException("Message is not of a LogoutResponse object type");
        }
        LogoutResponse response = (LogoutResponse) message;

        // Validate signature of the response if present
        if (response.getSignature() != null) {
            log.debug("Verifying message signature");
            validateSignature(response.getSignature(), context.getPeerEntityId());
            context.setInboundSAMLMessageAuthenticated(true);
        }

        // TODO - Validate destination

        // Validate issuer
        if (response.getIssuer() != null) {
            log.debug("Verifying issuer of the message");
            Issuer issuer = response.getIssuer();
            validateIssuer(issuer, context);
        }

        // TODO - Validate issue time

        // Verify status
        String statusCode = response.getStatus().getStatusCode().getValue();
        if (!statusCode.equals(StatusCode.SUCCESS_URI) && !statusCode.equals(StatusCode.PARTIAL_LOGOUT_URI)) {
            log.warn("Invalid status code " + statusCode + ": " + response.getStatus().getStatusMessage());
        }
    }
}

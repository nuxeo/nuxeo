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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.slo;

import org.joda.time.DateTime;
import org.nuxeo.ecm.platform.auth.saml.AbstractSAMLProfile;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.validation.ValidationException;

/**
 * WebSLO (Single Log Out) profile implementation.
 *
 * @since 5.9.6
 */
public class SLOProfileImpl extends AbstractSAMLProfile implements SLOProfile {

    public SLOProfileImpl(SingleLogoutService slo) {
        super(slo);
    }

    @Override
    public String getProfileIdentifier() {
        return PROFILE_URI;
    }

    public LogoutRequest buildLogoutRequest(SAMLMessageContext context, SAMLCredential credential)
            throws SAMLException {

        LogoutRequest request = build(LogoutRequest.DEFAULT_ELEMENT_NAME);
        request.setID(newUUID());
        // TODO(nfgs) Build issuer
        //request.setIssuer(issuer);
        request.setVersion(SAMLVersion.VERSION_20);
        request.setIssueInstant(new DateTime());

        request.setDestination(getEndpoint().getLocation());

        // Add session indexes
        if (credential.getSessionIndexes() == null ||
                credential.getSessionIndexes().isEmpty()) {
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

    public boolean processLogoutRequest(SAMLMessageContext context, SAMLCredential credential)
            throws SAMLException {

        SAMLObject message = context.getInboundSAMLMessage();

        // Verify type
        if (message == null || !(message instanceof LogoutRequest)) {
            throw new SAMLException(
                    "Message is not of a LogoutRequest object type");
        }

        LogoutRequest request = (LogoutRequest) message;

        // Validate signature of the response if present
        if (request.getSignature() != null) {
            log.debug("Verifying message signature");
            try {
                validateSignature(request.getSignature(),
                        context.getPeerEntityId());
            } catch (ValidationException e) {
                log.error("Error validating signature", e);
            } catch (org.opensaml.xml.security.SecurityException e) {
                e.printStackTrace();
            }
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
                nameID = (NameID) getDecrypter().decrypt(
                        request.getEncryptedID());
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
        if (request.getSessionIndexes() == null ||
                request.getSessionIndexes().isEmpty()) {
            return true;
        }

        // Else check if this is on of our session indexes
        for (SessionIndex sessionIndex : request.getSessionIndexes()) {
            if (credential.getSessionIndexes().contains(
                    sessionIndex.getSessionIndex())) {
                return true;
            }
        }

        return false;
    }

    public void processLogoutResponse(SAMLMessageContext context)
            throws SAMLException {

        SAMLObject message = context.getInboundSAMLMessage();

        if (!(message instanceof LogoutResponse)) {
            throw new SAMLException(
                    "Message is not of a LogoutResponse object type");
        }
        LogoutResponse response = (LogoutResponse) message;

        // Validate signature of the response if present
        if (response.getSignature() != null) {
            log.debug("Verifying message signature");
            try {
                validateSignature(response.getSignature(),
                        context.getPeerEntityId());
            } catch (ValidationException e) {
                log.error("Error validating signature", e);
            } catch (org.opensaml.xml.security.SecurityException e) {
                e.printStackTrace();
            }
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
        if (!statusCode.equals(StatusCode.SUCCESS_URI) &&
                !statusCode.equals(StatusCode.PARTIAL_LOGOUT_URI)) {
            log.warn("Invalid status code " + statusCode + ": " +
                    response.getStatus().getStatusMessage());
        }
    }
}

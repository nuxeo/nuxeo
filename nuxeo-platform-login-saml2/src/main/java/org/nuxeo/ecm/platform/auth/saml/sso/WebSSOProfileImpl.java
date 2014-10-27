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
package org.nuxeo.ecm.platform.auth.saml.sso;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.nuxeo.ecm.platform.auth.saml.AbstractSAMLProfile;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.metadata.*;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.validation.ValidationException;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * WebSSO (Single Sign On) profile implementation.
 *
 * @since 5.9.6
 */
public class WebSSOProfileImpl extends AbstractSAMLProfile implements WebSSOProfile {

    public WebSSOProfileImpl(SingleSignOnService sso) {
        super(sso);
    }

    @Override
    public String getProfileIdentifier() {
        return PROFILE_URI;
    }

    @Override
    public SAMLCredential processAuthenticationResponse(SAMLMessageContext context)
            throws SAMLException {
        SAMLObject message = context.getInboundSAMLMessage();

        // Validate type
        if (!(message instanceof Response)) {
            log.debug("Received response is not of a Response object type");
            throw new SAMLException(
                    "Received response is not of a Response object type");
        }
        Response response = (Response) message;

        // Validate status
        String statusCode = response.getStatus().getStatusCode().getValue();
        if (!StringUtils.equals(statusCode, StatusCode.SUCCESS_URI)) {
            log.debug("StatusCode was not a success: " + statusCode);
            throw new SAMLException(
                    "StatusCode was not a success: " + statusCode);
        }

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

        // TODO(nfgs) - Verify issue time ?!

        // TODO(nfgs) - Verify endpoint requested
        // Endpoint endpoint = context.getLocalEntityEndpoint();
        // validateEndpoint(response, ssoService);

        // Verify issuer
        if (response.getIssuer() != null) {
            log.debug("Verifying issuer of the message");
            Issuer issuer = response.getIssuer();
            validateIssuer(issuer, context);
        }

        List<Attribute> attributes = new LinkedList<>();
        List<Assertion> assertions = response.getAssertions();

        //Decrypt encrypted assertions
        List<EncryptedAssertion> encryptedAssertionList = response.getEncryptedAssertions();
        for (EncryptedAssertion ea : encryptedAssertionList) {
            try {
                log.debug("Decrypting assertion");
                assertions.add(getDecrypter().decrypt(ea));
            } catch (DecryptionException e) {
                log.debug(
                        "Decryption of received assertion failed, assertion will be skipped",
                        e);
            }
        }

        Subject subject = null;
        List<String> sessionIndexes = new ArrayList<>();

        // Find the assertion to be used for session creation, other assertions are ignored
        for (Assertion a : assertions) {

            // We're only interested in assertions with AuthnStatement
            if (a.getAuthnStatements().size() > 0) {
                try {
                    // Verify that the assertion is valid
                    validateAssertion(a, context);

                    // Store session indexes for logout
                    for (AuthnStatement statement : a.getAuthnStatements()) {
                        sessionIndexes.add(statement.getSessionIndex());
                    }

                } catch (Exception e) {
                    log.debug(
                            "Validation of received assertion failed, assertion will be skipped",
                            e);
                    continue;
                }
            }

            subject = a.getSubject();

            // Process all attributes
            for (AttributeStatement attStatement : a.getAttributeStatements()) {
                for (Attribute att : attStatement.getAttributes()) {
                    attributes.add(att);
                }
                // Decrypt attributes
                for (EncryptedAttribute att : attStatement.getEncryptedAttributes()) {
                    try {
                        attributes.add(getDecrypter().decrypt(att));
                    } catch (DecryptionException e) {
                        log.error("Failed to decrypt assertion");
                    }
                }
            }

            break;
        }

        // Make sure that at least one storage contains authentication statement and subject with bearer confirmation
        if (subject == null) {
            log.debug(
                    "Response doesn't have any valid assertion which would pass subject validation");
            throw new SAMLException("Error validating SAML response");
        }

        // Was the subject confirmed by this confirmation data? If so let's store the subject in the context.
        NameID nameID = null;
        if (subject.getEncryptedID() != null) {
            // TODO(nfgs) - Decrypt NameID
        } else {
            nameID = subject.getNameID();
        }

        if (nameID == null) {
            log.debug("NameID element must be present as part of the Subject in " +
                    "the Response message, please enable it in the IDP configuration");
            throw new SAMLException("NameID element must be present as part of the Subject " +
                    "in the Response message, please enable it in the IDP configuration");
        }

        // Populate custom data, if any
        Serializable additionalData = null; //processAdditionalData(context);

        // Create the credential
        return new SAMLCredential(nameID, sessionIndexes,
                context.getPeerEntityMetadata().getEntityID(),
                context.getRelayState(), attributes, context.getLocalEntityId(),
                additionalData);

    }

    @Override
    public AuthnRequest buildAuthRequest(HttpServletRequest httpRequest)
            throws SAMLException {

        AuthnRequest request = build(AuthnRequest.DEFAULT_ELEMENT_NAME);
        request.setID(newUUID());
        request.setVersion(SAMLVersion.VERSION_20);
        request.setIssueInstant(new DateTime());
        request.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);

        // Fill the assertion consumer URL
        request.setAssertionConsumerServiceURL(getStartPageURL(httpRequest));

        Issuer issuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(getBaseURL(httpRequest));
        request.setIssuer(issuer);

        NameIDPolicy nameIDPolicy = build(NameIDPolicy.DEFAULT_ELEMENT_NAME);
        nameIDPolicy.setFormat(NameIDType.UNSPECIFIED);
        request.setNameIDPolicy(nameIDPolicy);

        RequestedAuthnContext requestedAuthnContext = build(
                RequestedAuthnContext.DEFAULT_ELEMENT_NAME);
        requestedAuthnContext.setComparison(
                AuthnContextComparisonTypeEnumeration.EXACT);
        request.setRequestedAuthnContext(requestedAuthnContext);

        AuthnContextClassRef authnContextClassRef = build(
                AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        authnContextClassRef.setAuthnContextClassRef(
                AuthnContext.PPT_AUTHN_CTX);
        requestedAuthnContext.getAuthnContextClassRefs().add(
                authnContextClassRef);

        return request;

    }

    @Override
    protected void validateAssertion(Assertion assertion,
            SAMLMessageContext context)
            throws SAMLException, org.opensaml.xml.security.SecurityException,
            ValidationException, DecryptionException {
        super.validateAssertion(assertion, context);
        Signature signature = assertion.getSignature();
        if (signature == null) {
            SPSSODescriptor roleMetadata = (SPSSODescriptor) context.getLocalEntityRoleMetadata();

            if (roleMetadata != null &&
                    roleMetadata.getWantAssertionsSigned()) {
                if (!context.isInboundSAMLMessageAuthenticated()) {
                    throw new SAMLException("Metadata includes wantAssertionSigned, " +
                            "but neither Response nor included Assertion is signed");
                }
            }
        }
    }
}

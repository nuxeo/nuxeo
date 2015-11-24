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
package org.nuxeo.ecm.platform.auth.saml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.xml.SAMLConstants;

import org.opensaml.saml2.core.*;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.security.MetadataCriteria;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.security.criteria.UsageCriteria;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureTrustEngine;
import org.opensaml.xml.validation.ValidationException;

import javax.servlet.ServletRequest;
import javax.xml.namespace.QName;
import java.util.Date;
import java.util.UUID;

/**
 * Base abstract class for SAML profile processors.
 *
 * @since 6.0
 */
public abstract class AbstractSAMLProfile {
    protected final static Log log = LogFactory.getLog(AbstractSAMLProfile.class);

    protected final XMLObjectBuilderFactory builderFactory;

    private final Endpoint endpoint;

    private SignatureTrustEngine trustEngine;

    private Decrypter decrypter;

    public AbstractSAMLProfile(Endpoint endpoint) {
        this.endpoint = endpoint;
        this.builderFactory = Configuration.getBuilderFactory();
    }

    /**
     * @return the profile identifier (Uri).
     */
    abstract public String getProfileIdentifier();

    protected <T extends SAMLObject> T build(QName qName) {
        return (T) builderFactory.getBuilder(qName).buildObject(qName);
    }

    // VALIDATION

    protected void validateSignature(Signature signature, String IDPEntityID)
            throws ValidationException,
            org.opensaml.xml.security.SecurityException {

        if (trustEngine == null) {
            throw new SecurityException(
                    "Trust engine is not set, signature can't be verified");
        }

        SAMLSignatureProfileValidator validator = new SAMLSignatureProfileValidator();
        validator.validate(signature);
        CriteriaSet criteriaSet = new CriteriaSet();
        criteriaSet.add(new EntityIDCriteria(IDPEntityID));
        criteriaSet.add(
                new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME,
                        SAMLConstants.SAML20P_NS));
        criteriaSet.add(new UsageCriteria(UsageType.SIGNING));
        log.debug("Verifying signature: " + signature);

        if (!getTrustEngine().validate(signature, criteriaSet)) {
            throw new ValidationException(
                    "Signature is not trusted or invalid");
        }
    }

    protected void validateIssuer(Issuer issuer, SAMLMessageContext context)
            throws SAMLException {
        // Validate format of issuer
        if (issuer.getFormat() != null &&
                !issuer.getFormat().equals(NameIDType.ENTITY)) {
            throw new SAMLException("Assertion invalidated by issuer type");
        }
        // Validate that issuer is expected peer entity
        if (!context.getPeerEntityMetadata().getEntityID().equals(
                issuer.getValue())) {
            throw new SAMLException(
                    "Assertion invalidated by unexpected issuer value");
        }
    }

    protected void validateEndpoint(Response response, Endpoint endpoint)
            throws SAMLException {
        // Verify that destination in the response matches one of the available endpoints
        String destination = response.getDestination();

        if (destination != null) {
            if (destination.equals(endpoint.getLocation())) {
            } else if (destination.equals(endpoint.getResponseLocation())) {
            } else {
                log.debug("Intended destination " + destination +
                        " doesn't match any of the endpoint URLs");
                throw new SAMLException("Intended destination " + destination +
                        " doesn't match any of the endpoint URLs");
            }
        }

        // Verify response to field if present, set request if correct
        AuthnRequest request = retrieveRequest(response);

        // Verify endpoint requested in the original request
        if (request != null) {
            AssertionConsumerService assertionConsumerService = (AssertionConsumerService) endpoint;
            if (request.getAssertionConsumerServiceIndex() != null) {
                if (!request.getAssertionConsumerServiceIndex().equals(
                        assertionConsumerService.getIndex())) {
                    log.info("SAML response was received at a different endpoint " +
                            "index than was requested");
                }
            } else {
                String requestedResponseURL = request.getAssertionConsumerServiceURL();
                String requestedBinding = request.getProtocolBinding();
                if (requestedResponseURL != null) {
                    String responseLocation;
                    if (assertionConsumerService.getResponseLocation() !=
                            null) {
                        responseLocation = assertionConsumerService.getResponseLocation();
                    } else {
                        responseLocation = assertionConsumerService.getLocation();
                    }
                    if (!requestedResponseURL.equals(responseLocation)) {
                        log.info("SAML response was received at a different endpoint URL " +
                                        responseLocation +  " than was requested " +
                                        requestedResponseURL);
                    }
                }
                /*
                if (requestedBinding != null) {
                    if (!requestedBinding.equals(context.getInboundSAMLBinding())) {
                        log.info("SAML response was received using a different binding {} than was requested {}", context.getInboundSAMLBinding(), requestedBinding);
                    }
                }*/
            }
        }
    }

    protected void validateAssertion(Assertion assertion,
            SAMLMessageContext context)
            throws SAMLException, org.opensaml.xml.security.SecurityException,
            ValidationException, DecryptionException {

        validateIssuer(assertion.getIssuer(), context);

        Conditions conditions = assertion.getConditions();

        // validate conditions timestamps: notBefore, notOnOrAfter
        Date now = new DateTime().toDate();
        Date condition_notBefore = null;
        Date condition_NotOnOrAfter = null;
        if (conditions.getNotBefore() != null) {
            condition_notBefore = conditions.getNotBefore().toDate();
        }
        if (conditions.getNotOnOrAfter() != null) {
            condition_NotOnOrAfter = conditions.getNotOnOrAfter().toDate();
        }
        if (condition_notBefore != null && now.before(condition_notBefore)) {
            log.debug("Current time: [" + now + "] NotBefore: [" +
                    condition_notBefore + "]");
            throw new SAMLException("Conditions are not yet active");
        } else if (condition_NotOnOrAfter != null && (
                now.after(condition_NotOnOrAfter) || now.equals(condition_NotOnOrAfter))) {
            log.debug("Current time: [" + now + "] NotOnOrAfter: [" +
                    condition_NotOnOrAfter + "]");
            throw new SAMLException("Conditions have expired");
        }

        Signature signature = assertion.getSignature();

        if (signature != null) {
            validateSignature(signature, context.getPeerEntityMetadata().getEntityID());
        }

        // TODO(nfgs) : Check subject
    }

    protected AuthnRequest retrieveRequest(Response response)
            throws SAMLException {
        return null;
        /* TODO(nfgs) - Store SAML messages
        SAMLMessageStorage messageStorage = context.getMessageStorage();

        if (messageStorage != null && response.getInResponseTo() != null) {
            XMLObject xmlObject = messageStorage.retrieveMessage(response.getInResponseTo());
            if (xmlObject == null) {
                log.debug("InResponseToField doesn't correspond to sent message", response.getInResponseTo());
                throw new SAMLException("InResponseToField doesn't correspond to sent message");
            } else if (xmlObject instanceof AuthnRequest) {
                request = (AuthnRequest) xmlObject;
            } else {
                log.debug("Sent request was of different type than received response", response.getInResponseTo());
                throw new SAMLException("Sent request was of different type than received response");
            }
        }
        */
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public SignatureTrustEngine getTrustEngine() {
        return trustEngine;
    }

    public void setTrustEngine(SignatureTrustEngine trustEngine) {
        this.trustEngine = trustEngine;
    }

    public Decrypter getDecrypter() {
        return decrypter;
    }

    public void setDecrypter(Decrypter decrypter) {
        this.decrypter = decrypter;
    }

    protected String newUUID() {
        return UUID.randomUUID().toString();
    }

    protected String getBaseURL(ServletRequest request) {
        return VirtualHostHelper.getBaseURL(request);
    }

    protected String getStartPageURL(ServletRequest request) {
        return getBaseURL(request) +
                NuxeoAuthenticationFilter.DEFAULT_START_PAGE;
    }
}

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
package org.nuxeo.ecm.platform.auth.saml;

import java.util.UUID;

import javax.servlet.ServletRequest;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

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

    private int skewTimeMillis;

    public AbstractSAMLProfile(Endpoint endpoint) {
        this.endpoint = endpoint;
        this.builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        this.skewTimeMillis = SAMLConfiguration.getSkewTimeMillis();
    }

    /**
     * @return the profile identifier (Uri).
     */
    abstract public String getProfileIdentifier();

    protected <T extends SAMLObject> T build(QName qName) {
        return (T) builderFactory.getBuilderOrThrow(qName).buildObject(qName);
    }

    // VALIDATION

    protected void validateSignature(Signature signature, String IDPEntityID) throws SAMLException {

        if (trustEngine == null) {
            throw new SAMLException("Trust engine is not set, signature can't be verified");
        }

        try {
            SAMLSignatureProfileValidator validator = new SAMLSignatureProfileValidator();
            validator.validate(signature);
            CriteriaSet criteriaSet = new CriteriaSet();
            criteriaSet.add(new EntityIdCriterion(IDPEntityID));
            // criteriaSet.add(new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, SAMLConstants.SAML20P_NS));
            criteriaSet.add(new UsageCriterion(UsageType.SIGNING));
            log.debug("Verifying signature: " + signature);

            if (!getTrustEngine().validate(signature, criteriaSet)) {
                throw new SAMLException("Signature is not trusted or invalid");
            }
        } catch (SignatureException | org.opensaml.security.SecurityException e) {
            throw new SAMLException("Error validating signature", e);
        }

    }

    protected void validateIssuer(Issuer issuer, MessageContext<SAMLObject> context) throws SAMLException {
        // Validate format of issuer
        if (issuer.getFormat() != null && !issuer.getFormat().equals(NameIDType.ENTITY)) {
            throw new SAMLException("Assertion invalidated by issuer type");
        }
        // Validate that issuer is expected peer entity
        if (context.getPeerEntityMetadata() != null
                && !context.getPeerEntityMetadata().getEntityID().equals(issuer.getValue())) {
            throw new SAMLException("Assertion invalidated by unexpected issuer value");
        }
    }

    protected void validateEndpoint(Response response, Endpoint endpoint) throws SAMLException {
        // Verify that destination in the response matches one of the available endpoints
        String destination = response.getDestination();

        if (destination != null) {
            if (destination.equals(endpoint.getLocation())) {
            } else if (destination.equals(endpoint.getResponseLocation())) {
            } else {
                log.debug("Intended destination " + destination + " doesn't match any of the endpoint URLs");
                throw new SAMLException(
                        "Intended destination " + destination + " doesn't match any of the endpoint URLs");
            }
        }

        // Verify response to field if present, set request if correct
        AuthnRequest request = retrieveRequest(response);

        // Verify endpoint requested in the original request
        if (request != null) {
            AssertionConsumerService assertionConsumerService = (AssertionConsumerService) endpoint;
            if (request.getAssertionConsumerServiceIndex() != null) {
                if (!request.getAssertionConsumerServiceIndex().equals(assertionConsumerService.getIndex())) {
                    log.info("SAML response was received at a different endpoint " + "index than was requested");
                }
            } else {
                String requestedResponseURL = request.getAssertionConsumerServiceURL();
                request.getProtocolBinding();
                if (requestedResponseURL != null) {
                    String responseLocation;
                    if (assertionConsumerService.getResponseLocation() != null) {
                        responseLocation = assertionConsumerService.getResponseLocation();
                    } else {
                        responseLocation = assertionConsumerService.getLocation();
                    }
                    if (!requestedResponseURL.equals(responseLocation)) {
                        log.info("SAML response was received at a different endpoint URL " + responseLocation
                                + " than was requested " + requestedResponseURL);
                    }
                }
                /*
                 * if (requestedBinding != null) { if (!requestedBinding.equals(context.getInboundSAMLBinding())) {
                 * log.info("SAML response was received using a different binding {} than was requested {}",
                 * context.getInboundSAMLBinding(), requestedBinding); } }
                 */
            }
        }
    }

    protected void validateAssertion(Assertion assertion, MessageContext<SAMLObject> context) throws SAMLException {

        validateIssuer(assertion.getIssuer(), context);

        Conditions conditions = assertion.getConditions();

        // validate conditions timestamps: notBefore, notOnOrAfter
        DateTime now = new DateTime();
        DateTime notBefore = conditions.getNotBefore();
        DateTime notOnOrAfter = conditions.getNotOnOrAfter();

        if (notBefore != null && notBefore.minusMillis(getSkewTimeMillis()).isAfterNow()) {
            log.debug("Current time: [" + now + "] NotBefore: [" + notBefore + "]");
            throw new SAMLException("Conditions are not yet active");
        } else if (notOnOrAfter != null && notOnOrAfter.plusMillis(getSkewTimeMillis()).isBeforeNow()) {
            log.debug("Current time: [" + now + "] NotOnOrAfter: [" + notOnOrAfter + "]");
            throw new SAMLException("Conditions have expired");
        }

        Signature signature = assertion.getSignature();

        if (signature != null) {
            validateSignature(signature, context.getPeerEntityMetadata().getEntityID());
        }

        // TODO(nfgs) : Check subject
    }

    protected AuthnRequest retrieveRequest(Response response) throws SAMLException {
        // TODO(nfgs) - Store SAML messages to validate response.getInResponseTo()
        return null;
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

    public int getSkewTimeMillis() {
        return skewTimeMillis;
    }

    public void setSkewTimeMillis(int skewTimeMillis) {
        this.skewTimeMillis = skewTimeMillis;
    }

    protected String newUUID() {
        return "_" + UUID.randomUUID().toString();
    }

    protected String getBaseURL(ServletRequest request) {
        return VirtualHostHelper.getBaseURL(request);
    }

    protected String getStartPageURL(ServletRequest request) {
        return getBaseURL(request) + LoginScreenHelper.getStartupPagePath();
    }
}

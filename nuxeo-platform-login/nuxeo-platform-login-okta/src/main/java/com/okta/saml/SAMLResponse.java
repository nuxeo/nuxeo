package com.okta.saml;

import com.google.inject.Inject;
import com.okta.saml.util.Clock;
import com.okta.saml.util.SimpleClock;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.validator.ResponseSchemaValidator;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for an incoming SAMLResponse
 */
public class SAMLResponse {

    private static final Logger logger = LoggerFactory.getLogger(SAMLResponse.class);

    private final Clock clock;
    private final Response response;
    private final Assertion assertion;
    private final Configuration configuration;
    private final Application app;
    private final Map<String, List<String>> attributes;

    /**
     * Parses given SAML response string and validates it against the given Configuration;
     * provides access to the response's properties
     *
     * @param responseString The SAMLResponse sent by an IdP. The responseString must NOT be Base64 encoded.
     * @param configuration Configuration that includes the IdP's public certificate necessary to
     *                      verify the responseString's signature.
     * @throws SecurityPolicyException if the response fails validation
     */
    public SAMLResponse(String responseString, Configuration configuration) throws SecurityPolicyException {
        this(responseString, configuration, new SimpleClock());
    }

    @Inject
    private SAMLResponse(String responseString, Configuration configuration, Clock clock) throws SecurityPolicyException {
        this.clock = clock;
        this.configuration = configuration;
        this.response = validatedResponse(responseString);
        this.app = configuration.getApplication(getIssuer());
        this.assertion = validatedAssertion(response);
        this.attributes = loadedAttributes();

        validateSignature();
    }

    private HashMap<String, List<String>> loadedAttributes() {
        HashMap<String, List<String>> attributes = new HashMap<String, List<String>>();
        for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (Attribute attr : attributeStatement.getAttributes()) {
                if (attr.getAttributeValues().size() < 1) continue;

                List<String> values = new ArrayList<String>();
                for (XMLObject value : attr.getAttributeValues()) {
                    values.add(value.getDOM().getTextContent());
                }
                attributes.put(attr.getName(), values);
            }
        }
        return attributes;
    }

    private void validateSignature() throws SecurityPolicyException {
        SignatureValidator signatureValidator = new SignatureValidator(app.getCertificate().getCredential());
        Signature signature = response.getSignature();

        if (signature == null) {
            throw new SecurityPolicyException("No signature present");
        }

        try {
            signatureValidator.validate(signature);
        } catch (ValidationException e) {
            logger.debug(e.getMessage(), e);
            throw new SecurityPolicyException("Invalid signature");
        }
    }

    private Response validatedResponse(String assertion) throws SecurityPolicyException {
        Response response;
        try {
            XMLObject parsedResponse = parseSAML(assertion);
            if (!Response.class.isInstance(parsedResponse)) {
                logger.debug("Parsed response did not result in a Response node: " + parsedResponse.getElementQName());
                throw new SecurityPolicyException("Malformatted response");
            }
            response = (Response) parsedResponse;
            new ResponseSchemaValidator().validate(response);
        } catch (ValidationException e) {
            throw new SecurityPolicyException("Invalid response");
        }

        String issuer = response.getIssuer().getValue();
        if (configuration.getApplication(issuer) == null) {
            logger.debug("Configuration does not contain issuer: " + issuer);
            throw new SecurityPolicyException("Configuration does not have a matching issuer");
        }

        String statusCode = response.getStatus().getStatusCode().getValue();
        if (!StringUtils.equals(statusCode, StatusCode.SUCCESS_URI)) {
            logger.debug("StatusCode was not a success: " + statusCode);
            throw new SecurityPolicyException("StatusCode was not a success");
        }

        return response;
    }

    private Assertion validatedAssertion(Response response) throws SecurityPolicyException {
        List<Assertion> assertionList = response.getAssertions();
        if (assertionList.isEmpty()) {
            throw new SecurityPolicyException("No assertions found");
        } else if (assertionList.size() > 1) {
            throw new SecurityPolicyException("More than one assertion was found");
        }
        Assertion assertion = assertionList.get(0);


        if (!StringUtils.equals(assertion.getIssuer().getValue(), app.getIssuer())) {
            throw new SecurityPolicyException("Assertion issuer did not match the entity ID");
        }

        Conditions conditions = assertion.getConditions();

        // validate conditions timestamps: notBefore, notOnOrAfter
        Date now = clock.dateTimeNow().toDate();
        Date condition_notBefore = conditions.getNotBefore().toDate();
        Date condition_NotOnOrAfter = conditions.getNotOnOrAfter().toDate();
        if (now.before(condition_notBefore)) {
            logger.debug("Current time: [" + now + "] NotBefore: [" + condition_notBefore + "]");
            throw new SecurityPolicyException("Conditions are not yet active");
        } else if (now.after(condition_NotOnOrAfter) || now.equals(condition_NotOnOrAfter)) {
            logger.debug("Current time: [" + now + "] NotOnOrAfter: [" + condition_NotOnOrAfter + "]");
            throw new SecurityPolicyException("Conditions have expired");
        }

        return assertion;
    }

    /**
     * @return Assertion > Subject > NameID
     */
    public String getUserID() {
        return assertion.getSubject().getNameID().getValue();
    }

    /**
     * @return Assertion > Issuer
     */
    public String getIssuer() {
        return response.getIssuer().getValue();
    }

    /**
     * @return the attributes in a Map
     *          where the key is the name of the attribute
     *          and the value is the List of all the values of the attribute
     */
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    /**
     * @return the Response Destination
     */
    public String getDestination() {
        return response.getDestination();
    }

    /**
     * @return Assertion > Subject > SubjectConfirmation > Recipient
     */
    public String getRecipient() {
        return assertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getRecipient();
    }

    /**
     * @return Assertion > Conditions > AudienceRestriction > Audience
     */
    public String getAudience() {
        return assertion.getConditions().getAudienceRestrictions().get(0).getAudiences().get(0).getAudienceURI();
    }

    /**
     * @return the Response IssueInstant
     */
    public Date getIssueInstant() {
        return response.getIssueInstant().toDate();
    }

    private XMLObject parseSAML(String response) throws SecurityPolicyException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(response.getBytes("UTF-8"));
            Element root = new BasicParserPool().parse(bais).getDocumentElement();

            return org.opensaml.Configuration.getUnmarshallerFactory().getUnmarshaller(root).unmarshall(root);

        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            throw new SecurityPolicyException("Problem parsing the response.");
        }
    }
}

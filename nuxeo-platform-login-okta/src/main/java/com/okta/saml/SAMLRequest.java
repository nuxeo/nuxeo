package com.okta.saml;

import com.google.inject.Inject;
import com.okta.saml.util.Clock;
import com.okta.saml.util.Identifier;
import com.okta.saml.util.SimpleClock;
import com.okta.saml.util.UUIDIdentifer;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.AuthnContext;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.core.RequestedAuthnContext;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.StringWriter;

/**
 * Wrapper for an outgoing SAMLRequest
 */
public class SAMLRequest {

    private final AuthnRequest request;

    /**
     * Creates a SAML request based on the given Application
     * @param application Application that includes the URL where the SAMLRequest should be sent to and the issuer
     */
    public SAMLRequest(Application application) {
        this(application, new UUIDIdentifer(), new SimpleClock());
    }

    @Inject
    private SAMLRequest(Application application, Identifier identifier, Clock clock) {
        request = build(AuthnRequest.DEFAULT_ELEMENT_NAME);
        request.setID(identifier.getId());
        request.setVersion(SAMLVersion.VERSION_20);
        request.setIssueInstant(clock.dateTimeNow());
        request.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        request.setAssertionConsumerServiceURL(application.getAuthenticationURL());

        Issuer issuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(application.getIssuer());
        request.setIssuer(issuer);

        NameIDPolicy nameIDPolicy = build(NameIDPolicy.DEFAULT_ELEMENT_NAME);
        nameIDPolicy.setFormat(NameIDType.UNSPECIFIED);
        request.setNameIDPolicy(nameIDPolicy);

        RequestedAuthnContext requestedAuthnContext = build(RequestedAuthnContext.DEFAULT_ELEMENT_NAME);
        requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
        request.setRequestedAuthnContext(requestedAuthnContext);

        AuthnContextClassRef authnContextClassRef = build(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        authnContextClassRef.setAuthnContextClassRef(AuthnContext.PPT_AUTHN_CTX);
        requestedAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);
    }

    @SuppressWarnings("unchecked")
    private <T extends SAMLObject> T build(QName qName) {
        return (T) org.opensaml.Configuration.getBuilderFactory().getBuilder(qName).buildObject(qName);
    }

    /**
     * @return the created SAML request in a string format
     */
    public String toString() {
        try {
            Marshaller marshaller = org.opensaml.Configuration.getMarshallerFactory().getMarshaller(request);
            Element dom = marshaller.marshall(request);
            StringWriter stringWriter = new StringWriter();
            XMLHelper.writeNode(dom, stringWriter);
            return stringWriter.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return the created SAML request in openSAML AuthnRequest
     */
    public AuthnRequest getAuthnRequest() {
        return request;
    }
}

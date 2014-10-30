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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.auth.saml.binding.HTTPPostBinding;
import org.nuxeo.ecm.platform.auth.saml.binding.HTTPRedirectBinding;
import org.nuxeo.ecm.platform.auth.saml.binding.SAMLBinding;
import org.nuxeo.ecm.platform.auth.saml.key.KeyManager;
import org.nuxeo.ecm.platform.auth.saml.slo.SLOProfile;
import org.nuxeo.ecm.platform.auth.saml.slo.SLOProfileImpl;
import org.nuxeo.ecm.platform.auth.saml.sso.WebSSOProfile;
import org.nuxeo.ecm.platform.auth.saml.sso.WebSSOProfileImpl;
import org.nuxeo.ecm.platform.auth.saml.user.EmailBasedUserResolver;
import org.nuxeo.ecm.platform.auth.saml.user.UserResolver;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLinkComputer;
import org.nuxeo.runtime.api.Framework;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.saml2.metadata.*;
import org.opensaml.saml2.metadata.provider.*;
import org.opensaml.security.MetadataCredentialResolver;
import org.opensaml.util.URLBuilder;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.encryption.ChainingEncryptedKeyResolver;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.encryption.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.opensaml.xml.signature.SignatureTrustEngine;
import org.opensaml.xml.signature.impl.ExplicitKeySignatureTrustEngine;
import org.opensaml.xml.util.Pair;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGIN_ERROR;

/**
 * A SAML2 authentication provider.
 *
 * @since 5.9.6
 */
public class SAMLAuthenticationProvider
        implements NuxeoAuthenticationPlugin, LoginProviderLinkComputer,
        NuxeoAuthenticationPluginLogoutExtension {

    private static final Log log = LogFactory.getLog(SAMLAuthenticationProvider.class);

    // User Resolver
    private static final Class<? extends UserResolver> DEFAULT_USER_RESOLVER_CLASS = EmailBasedUserResolver.class;

    // SAML Constants
    static final String SAML_SESSION_KEY = "SAML_SESSION";

    // Supported SAML Bindings
    // TODO: Allow registering new bindings
    private static List<SAMLBinding> bindings = new ArrayList<>();
    static {
        bindings.add(new HTTPPostBinding());
        bindings.add(new HTTPRedirectBinding());
    }

    // Decryption key resolver
    private static ChainingEncryptedKeyResolver encryptedKeyResolver = new ChainingEncryptedKeyResolver();
    static {
        encryptedKeyResolver.getResolverChain().add(new InlineEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new EncryptedElementTypeEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new SimpleRetrievalMethodEncryptedKeyResolver());
    }

    // Profiles supported by the IdP
    private Map<String, AbstractSAMLProfile> profiles = new HashMap<>();

    private UserResolver userResolver;

    private KeyManager keyManager;

    private SignatureTrustEngine trustEngine;

    private Decrypter decrypter;

    private MetadataProvider metadataProvider;

    @Override
    public void initPlugin(Map<String, String> parameters) {

        // Initialize the User Resolver
        try {
            userResolver = DEFAULT_USER_RESOLVER_CLASS.newInstance();
        } catch (Exception e) {
            log.error("Failed to instantiate UserResolver", e);
        }

        // Initialize the OpenSAML library
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            log.error("Failed to bootstrap OpenSAML", e);
        }

        // Read the IdP metadata and initialize the supported profiles
        try {
            // Read the IdP metadata
            initializeMetadataProvider(parameters);

            // Setup Signature Trust Engine
            MetadataCredentialResolver metadataCredentialResolver =
                    new MetadataCredentialResolver(metadataProvider);
            trustEngine = new ExplicitKeySignatureTrustEngine(
                    metadataCredentialResolver,
                    org.opensaml.xml.Configuration.getGlobalSecurityConfiguration().getDefaultKeyInfoCredentialResolver());

            // Setup decrypter
            Credential encryptionCredential = getKeyManager().getEncryptionCredential();
            if (encryptionCredential != null) {
                KeyInfoCredentialResolver resolver = new StaticKeyInfoCredentialResolver(
                        encryptionCredential);
                decrypter = new Decrypter(null, resolver, encryptedKeyResolver);
                decrypter.setRootInNewDocument(true);
            }

            // Process IdP roles
            for (RoleDescriptor roleDescriptor : getIdPDescriptor().getRoleDescriptors()) {

                // Web SSO role
                if (roleDescriptor.getElementQName().equals(IDPSSODescriptor.DEFAULT_ELEMENT_NAME) &&
                        roleDescriptor.isSupportedProtocol(
                                org.opensaml.common.xml.SAMLConstants.SAML20P_NS)) {

                    IDPSSODescriptor idpSSO = (IDPSSODescriptor) roleDescriptor;

                    // SSO
                    for (SingleSignOnService sso : idpSSO.getSingleSignOnServices()) {
                        if (sso.getBinding().equals(SAMLConstants.SAML2_REDIRECT_BINDING_URI)) {
                            addProfile(new WebSSOProfileImpl(sso));
                            break;
                        }
                    }

                    // SLO
                    for (SingleLogoutService slo : idpSSO.getSingleLogoutServices()) {
                        if (slo.getBinding().equals(SAMLConstants.SAML2_REDIRECT_BINDING_URI)) {
                            addProfile(new SLOProfileImpl(slo));
                            break;
                        }
                    }
                }

                // TODO: Allow registering new profiles

            }

        } catch (MetadataProviderException e) {
            log.warn("Failed to register IdP: " + e.getMessage());
        }

        // contribute icon and link to the Login Screen
        if (parameters.containsKey("name")) {
            LoginScreenHelper.registerLoginProvider(
                    parameters.get("name"),
                    parameters.get("icon"),
                    null,
                    parameters.get("label"),
                    parameters.get("description"),
                    this);
        }
    }

    private void addProfile(AbstractSAMLProfile profile) {
        profile.setTrustEngine(trustEngine);
        profile.setDecrypter(decrypter);
        profiles.put(profile.getProfileIdentifier(), profile);
    }

    private void initializeMetadataProvider(Map<String, String> parameters)
            throws MetadataProviderException {
        AbstractMetadataProvider metadataProvider;

        String metadataUrl = parameters.get("metadata");
        if (metadataUrl == null) {
            throw new MetadataProviderException(
                "No metadata URI set for provider " +
                    ((parameters.containsKey("name")) ? parameters.get("name") : ""));
        }

        int requestTimeout = parameters.containsKey("timeout") ?
                Integer.parseInt(parameters.get("timeout")) : 5;

        if (metadataUrl.startsWith("http:") || metadataUrl.startsWith("https:")) {
            metadataProvider = new HTTPMetadataProvider(metadataUrl,
                    requestTimeout * 1000);
        } else { // file
            metadataProvider = new FilesystemMetadataProvider(new File(metadataUrl));
        }

        metadataProvider.setParserPool(new BasicParserPool());
        metadataProvider.initialize();

        this.metadataProvider = metadataProvider;
    }

    private EntityDescriptor getIdPDescriptor()
            throws MetadataProviderException {
        return (EntityDescriptor) metadataProvider.getMetadata();
    }

    /**
     * Returns a Login URL to use with HTTP Redirect
     */
    protected String getSSOUrl(HttpServletRequest request, HttpServletResponse response) {
        WebSSOProfile sso = (WebSSOProfile) profiles.get(WebSSOProfile.PROFILE_URI);
        if (sso == null) {
            return null;
        }

        // Create and populate the context
        SAMLMessageContext context = new BasicSAMLMessageContext();
        populateLocalContext(context);

        // Store the requested URL in the Relay State
        String requestedUrl = getRequestedUrl(request);
        if (requestedUrl != null) {
            context.setRelayState(requestedUrl);
        }

        // Get the encoded SAML request
        String encodedSaml = "";
        try {
            AuthnRequest authnRequest = sso.buildAuthRequest(request);
            // TODO(nfgs) - This should be enough!
            //context.setOutboundSAMLMessage(authnRequest);
            //context.setPeerEntityEndpoint(sso.getEndpoint());
            // TODO(nfgs) : Allow using some other binding
            //new HTTPRedirectDeflateEncoder().encode(context);

            Marshaller marshaller = Configuration.getMarshallerFactory()
                    .getMarshaller(authnRequest);
            if (marshaller == null) {
                log.error("Unable to marshall message, no marshaller registered " +
                        "for message object: " + authnRequest.getElementQName());
            }
            Element dom = marshaller.marshall(authnRequest);
            StringWriter buffer = new StringWriter();
            XMLHelper.writeNode(dom, buffer);
            encodedSaml = Base64.encodeBase64String(
                    buffer.toString().getBytes());
        } catch (SAMLException e) {
            log.error("Failed to get SAML Auth request", e);
        } catch (MarshallingException e) {
            log.error("Encountered error marshalling message to its DOM representation", e);
        }

        String loginURL = sso.getEndpoint().getLocation();
        try {
            URLBuilder urlBuilder = new URLBuilder(loginURL);
            urlBuilder.getQueryParams().add(
                    new Pair<>(HTTPRedirectBinding.SAML_REQUEST, encodedSaml));
            loginURL = urlBuilder.buildURL();
        } catch (IllegalArgumentException e) {
            log.error("Error while encoding URL", e);
            return null;
        }
        return loginURL;
    }

    private String getRequestedUrl(HttpServletRequest request) {
        String requestedUrl = (String) request.getAttribute(
                NXAuthConstants.REQUESTED_URL);
        if (requestedUrl == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                requestedUrl = (String) session.getAttribute(
                        NXAuthConstants.START_PAGE_SAVE_KEY);
            }
        }
        return requestedUrl;
    }

    @Override
    public String computeUrl(HttpServletRequest request, String requestedUrl) {
        return getSSOUrl(request, null);
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest request,
            HttpServletResponse response, String baseURL) {

        String loginURL = getSSOUrl(request, response);

        if (log.isDebugEnabled()) {
            log.debug("Send redirect to " + loginURL);
        }
        try {
            response.sendRedirect(loginURL);
        } catch (IOException e) {
            String errorMessage = String.format(
                    "Unable to send redirect on %s", loginURL);
            log.error(errorMessage, e);
            return false;
        }
        return true;
    }

    // Retrieves user identification information from the request.
    @Override
    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest request, HttpServletResponse response) {

        HttpServletRequestAdapter inTransport = new HttpServletRequestAdapter(
                request);
        SAMLBinding binding = getBinding(inTransport);

        // Check if we support this binding
        if (binding == null) {
            return null;
        }

        HttpServletResponseAdapter outTransport = new HttpServletResponseAdapter(
                response, request.isSecure());

        // Create and populate the context
        SAMLMessageContext context = new BasicSAMLMessageContext();
        context.setInboundMessageTransport(inTransport);
        context.setOutboundMessageTransport(outTransport);
        populateLocalContext(context);

        // Decode the message
        try {
            binding.decode(context);
        } catch (Exception e) {
            log.error("Error during SAML decoding", e);
            return null;
        }

        // Set Peer context info if needed
        try {
            if (context.getPeerEntityId() == null) {
                context.setPeerEntityId(getIdPDescriptor().getEntityID());
            }
            if (context.getPeerEntityMetadata() == null) {
                context.setPeerEntityMetadata(getIdPDescriptor());
            }
            if (context.getPeerEntityRole() == null) {
                context.setPeerEntityRole(
                        IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
            }
        } catch (MetadataProviderException e) {
            //
        }

        /* Tries to load peer SSL certificate from the inbound message transport using attribute
        X509Certificate[] chain = (X509Certificate[]) context.getInboundMessageTransport()
            .getAttribute(ServletRequestX509CredentialAdapter.X509_CERT_REQUEST_ATTRIBUTE);

        if (chain != null && chain.length > 0) {

            log.debug("Found certificate chain from request {}", chain[0]);
            BasicX509Credential credential = new BasicX509Credential();
            credential.setEntityCertificate(chain[0]);
            credential.setEntityCertificateChain(Arrays.asList(chain));
            context.setPeerSSLCredential(credential);

        }*/

        // Check for a response processor for this profile
        AbstractSAMLProfile processor = getProcessor(context);

        if (processor == null) {
            log.warn("Unsupported profile encountered in the context " +
                    context.getCommunicationProfileId());
            return null;
        }

        // Set the communication profile
        context.setCommunicationProfileId(processor.getProfileIdentifier());

        // Delegate handling the message to the processor
        SAMLObject message = context.getInboundSAMLMessage();

        // Handle SLO
        // TODO - Try to handle IdP initiated SLO somewhere else
        if (processor instanceof SLOProfile) {
            SLOProfile slo = (SLOProfile) processor;
            try {
                // Handle SLO response
                if (message instanceof LogoutResponse) {
                    slo.processLogoutResponse(context);
                    // Handle SLO request
                } else if (message instanceof LogoutRequest) {
                    SAMLCredential credential = getSamlCredential(request);
                    slo.processLogoutRequest(context, credential);
                }
            } catch (Exception e) {
                log.debug("Error processing SAML message", e);
            }
            return null;
        }

        // Handle SSO
        SAMLCredential credential = null;

        try {
            credential = ((WebSSOProfile) processor)
                    .processAuthenticationResponse(context);
        } catch (Exception e) {
            log.debug("Error processing SAML message", e);
            return null;
        }

        String userId = userResolver.findNuxeoUser(credential);

        if (userId == null) {
            sendError(request, "No user found with email: \"" +
                    credential.getNameID().getValue() + "\".");
            return null;
        }

        // Store session id in a cookie
        if (credential.getSessionIndexes() != null &&
                !credential.getSessionIndexes().isEmpty()) {
            String nameValue = credential.getNameID().getValue();
            String nameFormat = credential.getNameID().getFormat();
            String sessionId = credential.getSessionIndexes().get(0);
            addCookie(response, SAML_SESSION_KEY,
                    sessionId + "|" + nameValue + "|" + nameFormat);
        }

        return new UserIdentificationInfo(userId, userId);
    }

    protected AbstractSAMLProfile getProcessor(SAMLMessageContext context) {
        String profileId;
        SAMLObject message = context.getInboundSAMLMessage();
        if (message instanceof LogoutResponse ||
                message instanceof LogoutRequest) {
            profileId = SLOProfile.PROFILE_URI;
        } else {
            profileId = WebSSOProfile.PROFILE_URI;
        }

        return profiles.get(profileId);
    }

    protected SAMLBinding getBinding(InTransport transport) {

        for (SAMLBinding binding : bindings) {
            if (binding.supports(transport)) {
                return binding;
            }
        }

        return null;

    }

    private void populateLocalContext(SAMLMessageContext context) {
        // Set local info
        //context.setLocalEntityId(metadataProvider.getHostedSPName());
        context.setLocalEntityRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);

        // TODO - Generate SPSSO descriptor
        //context.setLocalEntityMetadata(entityDescriptor);
        //context.setLocalEntityRoleMetadata(roleDescriptor);

        context.setMetadataProvider(metadataProvider);
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    /**
     * Returns a Logout URL to use with HTTP Redirect
     */
    protected String getSLOUrl(HttpServletRequest request, HttpServletResponse response) {
        SLOProfile slo = (SLOProfile) profiles.get(SLOProfile.PROFILE_URI);
        if (slo == null) {
            return null;
        }

        String logoutURL = slo.getEndpoint().getLocation();

        SAMLCredential credential = getSamlCredential(request);

        // Create and populate the context
        SAMLMessageContext context = new BasicSAMLMessageContext();
        populateLocalContext(context);

        try {
            LogoutRequest logoutRequest = slo.buildLogoutRequest(context, credential);
            Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(
                    logoutRequest);
            if (marshaller == null) {
                log.error("Unable to marshall message, no marshaller registered " +
                        "for message object: " + logoutRequest.getElementQName());
            }
            Element dom = marshaller.marshall(logoutRequest);
            StringWriter buffer = new StringWriter();
            XMLHelper.writeNode(dom, buffer);
            String encodedSaml = Base64.encodeBase64String(
                    buffer.toString().getBytes());

            // Add the SAML as parameter
            URLBuilder urlBuilder = new URLBuilder(logoutURL);
            urlBuilder.getQueryParams().add(
                    new Pair<>(HTTPRedirectBinding.SAML_REQUEST, encodedSaml));
            logoutURL = urlBuilder.buildURL();
        } catch (SAMLException e) {
            log.error("Failed to get SAML Logout request", e);
        } catch (MarshallingException e) {
            log.error("Encountered error marshalling message to its DOM representation", e);
        }

        return logoutURL;
    }

    private SAMLCredential getSamlCredential(HttpServletRequest request) {
        SAMLCredential credential = null;

        // Retrieve the SAMLCredential credential from cookie
        Cookie cookie = getCookie(request, SAML_SESSION_KEY);
        if (cookie != null) {
            String[] parts = cookie.getValue().split("\\|");
            String sessionId = parts[0];
            String nameValue = parts[1];
            String nameFormat = parts[2];

            NameID nameID = (NameID) Configuration.getBuilderFactory()
                    .getBuilder(NameID.DEFAULT_ELEMENT_NAME)
                    .buildObject(NameID.DEFAULT_ELEMENT_NAME);
            nameID.setValue(nameValue);
            nameID.setFormat(nameFormat);

            List<String> sessionIndexes = new ArrayList<>();
            sessionIndexes.add(sessionId);

            credential = new SAMLCredential(nameID, sessionIndexes);
        }

        return credential;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest request,
            HttpServletResponse response) {
        String logoutURL = getSLOUrl(request, response);

        if (logoutURL == null) {
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("Send redirect to " + logoutURL);
        }

        try {
            response.sendRedirect(logoutURL);
        } catch (IOException e) {
            String errorMessage = String.format("Unable to send redirect on %s",
                    logoutURL);
            log.error(errorMessage, e);
            return false;
        }

        Cookie cookie = getCookie(request, SAML_SESSION_KEY);
        if (cookie != null) {
            removeCookie(response, cookie);
        }

        return true;
    }

    private void sendError(HttpServletRequest req, String msg) {
        req.setAttribute(LOGIN_ERROR, msg);
    }

    private KeyManager getKeyManager() {
        if (keyManager == null) {
            keyManager = Framework.getLocalService(KeyManager.class);
        }
        return keyManager;
    }

    private void addCookie(HttpServletResponse httpResponse, String name,
            String value) {
        Cookie cookie = new Cookie(name, value);
        httpResponse.addCookie(cookie);
    }

    private Cookie getCookie(HttpServletRequest httpRequest,
            String cookieName) {
        Cookie cookies[] = httpRequest.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookieName.equals(cookies[i].getName())) {
                    return cookies[i];
                }
            }
        }
        return null;
    }

    private void removeCookie(HttpServletResponse httpResponse, Cookie cookie) {
        log.debug(String.format("Removing cookie %s.", cookie.getName()));
        cookie.setMaxAge(0);
        cookie.setValue("");
        httpResponse.addCookie(cookie);
    }
}

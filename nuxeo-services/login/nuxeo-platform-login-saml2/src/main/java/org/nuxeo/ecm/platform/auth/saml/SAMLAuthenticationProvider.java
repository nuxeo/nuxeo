/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGIN_ERROR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.auth.saml.binding.HTTPPostBinding;
import org.nuxeo.ecm.platform.auth.saml.binding.HTTPRedirectBinding;
import org.nuxeo.ecm.platform.auth.saml.binding.SAMLBinding;
import org.nuxeo.ecm.platform.auth.saml.key.KeyManager;
import org.nuxeo.ecm.platform.auth.saml.slo.SLOProfile;
import org.nuxeo.ecm.platform.auth.saml.slo.SLOProfileImpl;
import org.nuxeo.ecm.platform.auth.saml.sso.WebSSOProfile;
import org.nuxeo.ecm.platform.auth.saml.sso.WebSSOProfileImpl;
import org.nuxeo.ecm.platform.auth.saml.user.AbstractUserResolver;
import org.nuxeo.ecm.platform.auth.saml.user.EmailBasedUserResolver;
import org.nuxeo.ecm.platform.auth.saml.user.UserMapperBasedResolver;
import org.nuxeo.ecm.platform.auth.saml.user.UserResolver;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLinkComputer;
import org.nuxeo.ecm.platform.web.common.CookieHelper;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.service.UserMapperService;
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
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml2.metadata.provider.AbstractMetadataProvider;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.security.MetadataCredentialResolver;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.encryption.ChainingEncryptedKeyResolver;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.encryption.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.opensaml.xml.signature.SignatureTrustEngine;
import org.opensaml.xml.signature.impl.ExplicitKeySignatureTrustEngine;

/**
 * A SAML2 authentication provider.
 *
 * @since 6.0
 */
public class SAMLAuthenticationProvider
        implements NuxeoAuthenticationPlugin, LoginProviderLinkComputer, NuxeoAuthenticationPluginLogoutExtension {

    private static final Log log = LogFactory.getLog(SAMLAuthenticationProvider.class);

    private static final String ERROR_PAGE = "/saml/error.jsp";

    private static final String ERROR_AUTH = "error.saml.auth";

    private static final String ERROR_USER = "error.saml.userMapping";

    // User Resolver
    private static final Class<? extends UserResolver> DEFAULT_USER_RESOLVER_CLASS = EmailBasedUserResolver.class;

    private static final Class<? extends UserResolver> USERMAPPER_USER_RESOLVER_CLASS = UserMapperBasedResolver.class;

    // SAML Constants
    static final String SAML_SESSION_KEY = "SAML_SESSION";

    // Supported SAML Bindings
    // TODO: Allow registering new bindings
    static List<SAMLBinding> bindings = new ArrayList<>();

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
        String userResolverClassname = parameters.get("userResolverClass");
        Class<? extends UserResolver> userResolverClass = null;
        if (StringUtils.isBlank(userResolverClassname)) {
            UserMapperService ums = Framework.getService(UserMapperService.class);
            if (ums != null) {
                userResolverClass = USERMAPPER_USER_RESOLVER_CLASS;
            } else {
                userResolverClass = DEFAULT_USER_RESOLVER_CLASS;
            }
        } else {
            try {
                userResolverClass = Class.forName(userResolverClassname).asSubclass(AbstractUserResolver.class);
            } catch (ClassNotFoundException e) {
                throw new NuxeoException("Failed get user resolver class " + userResolverClassname, e);
            }

        }
        try {
            userResolver = userResolverClass.getConstructor().newInstance();
            userResolver.init(parameters);
        } catch (ReflectiveOperationException e) {
            log.error("Failed to initialize user resolver " + userResolverClassname);
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
            MetadataCredentialResolver metadataCredentialResolver = new MetadataCredentialResolver(metadataProvider);
            trustEngine = new ExplicitKeySignatureTrustEngine(metadataCredentialResolver,
                    org.opensaml.xml.Configuration.getGlobalSecurityConfiguration()
                                                  .getDefaultKeyInfoCredentialResolver());

            // Setup decrypter
            Credential encryptionCredential = getKeyManager().getEncryptionCredential();
            if (encryptionCredential != null) {
                KeyInfoCredentialResolver resolver = new StaticKeyInfoCredentialResolver(encryptionCredential);
                decrypter = new Decrypter(null, resolver, encryptedKeyResolver);
                decrypter.setRootInNewDocument(true);
            }

            // Process IdP roles
            for (RoleDescriptor roleDescriptor : getIdPDescriptor().getRoleDescriptors()) {

                // Web SSO role
                if (roleDescriptor.getElementQName().equals(IDPSSODescriptor.DEFAULT_ELEMENT_NAME)
                        && roleDescriptor.isSupportedProtocol(org.opensaml.common.xml.SAMLConstants.SAML20P_NS)) {

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
            }

        } catch (MetadataProviderException e) {
            log.warn("Failed to register IdP: " + e.getMessage());
        }

        // contribute icon and link to the Login Screen
        if (StringUtils.isNotBlank(parameters.get("name"))) {
            LoginScreenHelper.registerLoginProvider(parameters.get("name"), parameters.get("icon"), null,
                    parameters.get("label"), parameters.get("description"), this);
        }
    }

    private void addProfile(AbstractSAMLProfile profile) {
        profile.setTrustEngine(trustEngine);
        profile.setDecrypter(decrypter);
        profiles.put(profile.getProfileIdentifier(), profile);
    }

    private void initializeMetadataProvider(Map<String, String> parameters) throws MetadataProviderException {
        AbstractMetadataProvider metadataProvider;

        String metadataUrl = parameters.get("metadata");
        if (metadataUrl == null) {
            throw new MetadataProviderException("No metadata URI set for provider "
                    + ((parameters.containsKey("name")) ? parameters.get("name") : ""));
        }

        int requestTimeout = parameters.containsKey("timeout") ? Integer.parseInt(parameters.get("timeout")) : 5;

        if (metadataUrl.startsWith("http:") || metadataUrl.startsWith("https:")) {
            metadataProvider = new HTTPMetadataProvider(metadataUrl, requestTimeout * 1000);
        } else { // file
            metadataProvider = new FilesystemMetadataProvider(new File(metadataUrl));
        }

        metadataProvider.setParserPool(new BasicParserPool());
        metadataProvider.initialize();

        this.metadataProvider = metadataProvider;
    }

    private EntityDescriptor getIdPDescriptor() throws MetadataProviderException {
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
        populateLocalContext(context, request);

        // Store the requested URL in the Relay State
        String requestedUrl = getRequestedUrl(request);
        if (requestedUrl != null) {
            context.setRelayState(requestedUrl);
        }

        // Build Uri
        HTTPRedirectBinding binding = (HTTPRedirectBinding) getBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        String loginURL = sso.getEndpoint().getLocation();
        try {
            AuthnRequest authnRequest = sso.buildAuthRequest(request);
            authnRequest.setDestination(sso.getEndpoint().getLocation());
            context.setOutboundSAMLMessage(authnRequest);
            loginURL = binding.buildRedirectURL(context, sso.getEndpoint().getLocation());
        } catch (SAMLException e) {
            log.error("Failed to build redirect URL", e);
        }
        return loginURL;
    }

    private String getRequestedUrl(HttpServletRequest request) {
        String requestedUrl = (String) request.getAttribute(NXAuthConstants.REQUESTED_URL);
        if (requestedUrl == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                requestedUrl = (String) session.getAttribute(NXAuthConstants.START_PAGE_SAVE_KEY);
            }
        }
        return requestedUrl;
    }

    @Override
    public String computeUrl(HttpServletRequest request, String requestedUrl) {
        return getSSOUrl(request, null);
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest request, HttpServletResponse response, String baseURL) {

        String loginError = (String) request.getAttribute(LOGIN_ERROR);
        if (loginError != null) {
            try {
                request.getRequestDispatcher(ERROR_PAGE).forward(request, response);
                return Boolean.TRUE;
            } catch (ServletException | IOException e) {
                log.error("Failed to redirect to error page", e);
                return Boolean.FALSE;
            }
        }

        String loginURL = getSSOUrl(request, response);
        try {
            response.sendRedirect(loginURL);
        } catch (IOException e) {
            String errorMessage = String.format("Unable to send redirect on %s", loginURL);
            log.error(errorMessage, e);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    // Retrieves user identification information from the request.
    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest request, HttpServletResponse response) {

        HttpServletRequestAdapter inTransport = new HttpServletRequestAdapter(request);
        SAMLBinding binding = getBinding(inTransport);

        // Check if we support this binding
        if (binding == null) {
            return null;
        }

        HttpServletResponseAdapter outTransport = new HttpServletResponseAdapter(response, request.isSecure());

        // Create and populate the context
        SAMLMessageContext context = new BasicSAMLMessageContext();
        context.setInboundMessageTransport(inTransport);
        context.setOutboundMessageTransport(outTransport);
        populateLocalContext(context, request);

        // Decode the message
        try {
            binding.decode(context);
        } catch (org.opensaml.xml.security.SecurityException | MessageDecodingException e) {
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
                context.setPeerEntityRole(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
            }
        } catch (MetadataProviderException e) {
            //
        }

        // Check for a response processor for this profile
        AbstractSAMLProfile processor = getProcessor(context);

        if (processor == null) {
            log.warn("Unsupported profile encountered in the context " + context.getCommunicationProfileId());
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
            } catch (SAMLException e) {
                log.debug("Error processing SAML message", e);
            }
            return null;
        }

        // Handle SSO
        SAMLCredential credential;

        try {
            credential = ((WebSSOProfile) processor).processAuthenticationResponse(context);
        } catch (SAMLException e) {
            log.error("Error processing SAML message", e);
            sendError(request, ERROR_AUTH);
            return null;
        }

        String userId = Framework.doPrivileged(() -> userResolver.findOrCreateNuxeoUser(credential));
        if (userId == null) {
            log.warn("Failed to resolve user with NameID \"" + credential.getNameID().getValue() + "\".");
            sendError(request, ERROR_USER);
            return null;
        }

        // Store session id in a cookie
        if (credential.getSessionIndexes() != null && !credential.getSessionIndexes().isEmpty()) {
            String nameValue = credential.getNameID().getValue();
            String nameFormat = credential.getNameID().getFormat();
            String sessionId = credential.getSessionIndexes().get(0);
            Cookie cookie = CookieHelper.createCookie(request, SAML_SESSION_KEY,
                    String.join("|", sessionId, nameValue, nameFormat));
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }

        // Redirect to URL in relay state if any
        HttpSession session = request.getSession(!response.isCommitted());
        if (session != null) {
            if (StringUtils.isNotEmpty(credential.getRelayState())) {
                session.setAttribute(NXAuthConstants.START_PAGE_SAVE_KEY, credential.getRelayState());
            }
        }

        return new UserIdentificationInfo(userId, userId);
    }

    protected AbstractSAMLProfile getProcessor(SAMLMessageContext context) {
        String profileId;
        SAMLObject message = context.getInboundSAMLMessage();
        if (message instanceof LogoutResponse || message instanceof LogoutRequest) {
            profileId = SLOProfile.PROFILE_URI;
        } else {
            profileId = WebSSOProfile.PROFILE_URI;
        }

        return profiles.get(profileId);
    }

    protected SAMLBinding getBinding(String bindingURI) {
        for (SAMLBinding binding : bindings) {
            if (binding.getBindingURI().equals(bindingURI)) {
                return binding;
            }
        }
        return null;
    }

    protected SAMLBinding getBinding(InTransport transport) {
        for (SAMLBinding binding : bindings) {
            if (binding.supports(transport)) {
                return binding;
            }
        }
        return null;
    }

    private void populateLocalContext(SAMLMessageContext context, HttpServletRequest request) {
        // Set local info
        context.setLocalEntityId(SAMLConfiguration.getEntityId());
        context.setLocalEntityRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);

        // Set local entity role metadata
        String baseURL = VirtualHostHelper.getBaseURL(request);
        baseURL += (baseURL.endsWith("/") ? "" : "/") + LoginScreenHelper.getStartupPagePath();
        SPSSODescriptor descriptor = SAMLConfiguration.getSPSSODescriptor(baseURL);
        context.setLocalEntityRoleMetadata(descriptor);

        context.setMetadataProvider(metadataProvider);

        // Set the signing key
        keyManager = Framework.getService(KeyManager.class);
        if (getKeyManager().getSigningCredential() != null) {
            context.setOutboundSAMLMessageSigningCredential(getKeyManager().getSigningCredential());
        }
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return Boolean.TRUE;
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
        populateLocalContext(context, request);

        try {
            LogoutRequest logoutRequest = slo.buildLogoutRequest(context, credential);
            logoutRequest.setDestination(slo.getEndpoint().getLocation());
            context.setOutboundSAMLMessage(logoutRequest);

            HTTPRedirectBinding binding = (HTTPRedirectBinding) getBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
            logoutURL = binding.buildRedirectURL(context, slo.getEndpoint().getLocation());
        } catch (SAMLException e) {
            log.error("Failed to get SAML Logout request", e);
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
    public Boolean handleLogout(HttpServletRequest request, HttpServletResponse response) {
        String logoutURL = getSLOUrl(request, response);

        if (logoutURL == null) {
            return Boolean.FALSE;
        }

        if (log.isDebugEnabled()) {
            log.debug("Send redirect to " + logoutURL);
        }

        try {
            response.sendRedirect(logoutURL);
        } catch (IOException e) {
            String errorMessage = String.format("Unable to send redirect on %s", logoutURL);
            log.error(errorMessage, e);
            return Boolean.FALSE;
        }

        Cookie cookie = getCookie(request, SAML_SESSION_KEY);
        if (cookie != null) {
            removeCookie(response, cookie);
        }

        return Boolean.TRUE;
    }

    private void sendError(HttpServletRequest req, String key) {
        String msg = I18NUtils.getMessageString("messages", key, null, req.getLocale());
        req.setAttribute(LOGIN_ERROR, msg);
    }

    private KeyManager getKeyManager() {
        if (keyManager == null) {
            keyManager = Framework.getService(KeyManager.class);
        }
        return keyManager;
    }

    private Cookie getCookie(HttpServletRequest httpRequest, String cookieName) {
        Cookie cookies[] = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cooky : cookies) {
                if (cookieName.equals(cooky.getName())) {
                    return cooky;
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

package com.okta.saml.util;

import com.okta.saml.Application;
import com.okta.saml.Configuration;
import com.okta.saml.SAMLResponse;
import com.okta.saml.SAMLValidator;
import org.apache.commons.codec.binary.Base64;
import org.opensaml.ws.security.SecurityPolicyException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Collection;
import java.util.List;

public class OktaAuthPeer {
    private String loggedInKey;
    private String loggedOutKey;
    private String configFilePath;
    private SAMLValidator validator;
    private Configuration configuration;
    private Application defaultApplication;
    private List<String> filteredHeaders;

    public OktaAuthPeer(String configFilePath, String loggedInKey, String loggedOutKey) throws SecurityPolicyException, IOException {
        init(configFilePath, loggedInKey, loggedOutKey);
    }

    public OktaAuthPeer()  {
    }

    public void init(String configFilePath, String loggedInKey, String loggedOutKey) throws SecurityPolicyException, IOException {
        this.loggedInKey = loggedInKey;
        this.loggedOutKey = loggedOutKey;
        this.configFilePath = configFilePath;

        String file = readFile(configFilePath);
        validator = new SAMLValidator();
        configuration = validator.getConfiguration(file);

        for (Application application : configuration.getApplications().values()) {
            if (defaultApplication == null) {
                defaultApplication = application;
            }
        }

    }

    public void putPrincipalInSessionContext(HttpServletRequest request, Principal principal) {
        final HttpSession httpSession = request.getSession();
        httpSession.setAttribute(loggedInKey, principal);
        httpSession.setAttribute(loggedOutKey, null);
    }

    public boolean isPrincipalAlreadyInSessionContext(final HttpServletRequest request, final Principal principal) {
        Principal currentPrincipal = (Principal) request.getSession().getAttribute(loggedInKey);
        return currentPrincipal != null && currentPrincipal.getName() != null && principal != null && currentPrincipal.getName().equals(principal.getName());
    }

    public void removePrincipalFromSessionContext(final HttpServletRequest request) {
        final HttpSession httpSession = request.getSession();
        httpSession.setAttribute(loggedInKey, null);
        httpSession.setAttribute(loggedOutKey, Boolean.TRUE);
    }

    public Principal getUserPrincipal(final SAMLResponse response) {
        return new Principal() {
            public String getName() {
                return response.getUserID();
            }
        };
    }

    public String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.forName("UTF-8").decode(bb).toString();
        }
        finally {
            stream.close();
        }
    }

    public String getAuthRedirectUrl(String redirectUrl, String relayState) {
        if (!redirectUrl.contains("RelayState") && relayState != null && !relayState.isEmpty()) {
            if (redirectUrl.contains("?")) {
                redirectUrl += "&";
            } else {
                redirectUrl += "?";
            }
            redirectUrl += "RelayState=" + relayState;
        }
        return redirectUrl;
    }

    public SAMLResponse getSAMLResponse(String assertion) throws UnsupportedEncodingException, SecurityPolicyException {
        assertion = new String(Base64.decodeBase64(assertion.getBytes("UTF-8")), Charset.forName("UTF-8"));
        return getValidator().getSAMLResponse(assertion, getConfiguration());
    }

    public SAMLValidator getValidator() {
        return validator;
    }

    public void setValidator(SAMLValidator validator) {
        this.validator = validator;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Application getDefaultApplication() {
        return defaultApplication;
    }

    public void setDefaultApplication(Application defaultApplication) {
        this.defaultApplication = defaultApplication;
    }

    public Principal getPrincipalFromSession(HttpServletRequest request) {
        if (request.getSession().getAttribute(loggedOutKey) != null) {
            return null;
        }
        return (Principal) request.getSession().getAttribute(loggedInKey);
    }

    public List<String> getFilteredHeaders() {
        return filteredHeaders;
    }

    public void setFilteredHeaders(List<String> filteredHeaders) {
        this.filteredHeaders = filteredHeaders;
    }

    public boolean shouldHandleRequest(HttpServletRequest request) {
        if (filteredHeaders == null) {
            return true;
        }

        String reqUrl = request.getRequestURL().toString();
        if (reqUrl.contains("/ForgotLoginDetails/") || reqUrl.contains("/rest/api/")) {
            return false;
        }

        String header;
        for (String headerName : filteredHeaders) {
            header = request.getHeader(headerName);
            if (header != null && !header.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isSPUser(HttpServletRequest request, String username, Collection<String> uGroups) {
        return !isIpAllowedForOkta(request) || !isUsernameAllowedForOkta(username) || isInSPGroups(uGroups);
    }

    public boolean isIpAllowedForOkta(String ip) {
        return getConfiguration().isIpAllowedForOkta(ip);
    }

    public boolean isIpAllowedForOkta(HttpServletRequest request) {
        return isIpAllowedForOkta(request.getRemoteAddr());
    }

    public boolean isSPUserOrGroupNamesUsed() {
        return getConfiguration().isSPUsernamesUsed() || getConfiguration().isSPGroupnamesUsed();
    }

    private boolean isUsernameAllowedForOkta(String username) {
        return getConfiguration().isUsernameAllowedForOkta(username);
    }

    private boolean isInSPGroups(Collection<String> userGroups) {
        return getConfiguration().isInSPGroups(userGroups);
    }
}

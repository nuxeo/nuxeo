package org.nuxeo.opensocial.shindig.oauth;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.social.core.oauth.OAuthSecurityToken;

public class NXAuthHandler implements AuthenticationHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(NXAuthHandler.class);

    public String getName() {
        return "nuxeo authentication handler";
    }

    public SecurityToken getSecurityTokenFromRequest(
            final HttpServletRequest req) {

        Principal p = req.getUserPrincipal();

        if (p != null) {

            String url = req.getRequestURI();// what else can we do?

            String realURL = req.getParameter("url");
            String referrer = req.getHeader("Referer");
            if (referrer != null) {
                Uri ref = UriBuilder.parse(referrer).toUri();
                String urlOfRef = ref.getQueryParameter("url");
                if (urlOfRef != null) {
                    realURL = urlOfRef;
                }
            }
            if (realURL != null) {
                url = realURL;
            }
            OAuthSecurityToken token = new HackyToken(p.getName(), url,
                    "nuxeo-opensocial", "nuxeo-opensocial");
            return token;
        } else {
            return null;
        }
    }

    public String getWWWAuthenticateHeader(String realm) {
        return null;
    }
}

class HackyToken extends OAuthSecurityToken {
    public HackyToken(String userId, String appUrl, String appId, String domain) {
        super(userId, appUrl, appId, domain, "default");
    }

    @Override
    public long getModuleId() {
        long id = 8989;
        return id;
    }

    @Override
    public String toSerialForm() {
        return null;
    }

    @Override
    public String getUpdatedToken() {
        return null;// this IS called
    }

    @Override
    public String getTrustedJson() {
        return null;
    }
}
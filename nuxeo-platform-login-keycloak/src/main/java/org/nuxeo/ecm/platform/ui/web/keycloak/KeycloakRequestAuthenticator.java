package org.nuxeo.ecm.platform.ui.web.keycloak;

import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.realm.GenericPrincipal;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.*;
import org.keycloak.adapters.tomcat.*;
import org.keycloak.enums.TokenStore;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.List;

public class KeycloakRequestAuthenticator extends RequestAuthenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakRequestAuthenticator.class);

    public static final String KEYCLOAK_ACCESS_TOKEN = "KEYCLOAK_ACCESS_TOKEN";

    private CatalinaUserSessionManagement userSessionManagement = new CatalinaUserSessionManagement();

    protected Request request;

    protected HttpServletResponse response;

    protected LoginConfig loginConfig;

    public KeycloakRequestAuthenticator(Request request, HttpServletResponse response, CatalinaHttpFacade facade, KeycloakDeployment deployment) {
        super(facade, deployment);
        this.request = request;
        this.response = response;
        this.tokenStore = getTokenStore();
        this.sslRedirectPort = request.getConnector().getRedirectPort();
    }

    @Override
    public AuthOutcome authenticate() {
        AuthOutcome outcome = super.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            return AuthOutcome.AUTHENTICATED;
        }
        AuthChallenge challenge = this.getChallenge();
        if (challenge != null) {
            if (loginConfig == null) {
                loginConfig = request.getContext().getLoginConfig();
            }
            if (challenge.errorPage()) {
                if (forwardToErrorPageInternal(request, response, loginConfig)) {
                    return AuthOutcome.FAILED;
                }
            }
            challenge.challenge(facade);
        }
        return AuthOutcome.FAILED;
    }

    protected boolean forwardToErrorPageInternal(Request request, HttpServletResponse response, Object loginConfig) {
        if (loginConfig == null) return false;
        LoginConfig config = (LoginConfig) loginConfig;
        if (config.getErrorPage() == null) return false;
        try {
            Method method = FormAuthenticator.class.getDeclaredMethod("forwardToErrorPage", Request.class, HttpServletResponse.class, LoginConfig.class);
            method.setAccessible(true);
            method.invoke(this, request, response, config);
        } catch (Exception e) {
            String message = "Error occurred during Keycloak authentication";
            LOGGER.error(message, e);
            throw new RuntimeException(message, e);
        }
        return true;
    }

    protected GenericPrincipalFactory createPrincipalFactory() {
        return new GenericPrincipalFactory() {
            @Override
            protected GenericPrincipal createPrincipal(Principal userPrincipal, List<String> roles) {
                return new GenericPrincipal(userPrincipal.getName(), null, roles, userPrincipal, null);
            }
        };
    }

    protected AdapterTokenStore getTokenStore() {
        final String TOKEN_STORE_NOTE = "TOKEN_STORE_NOTE";

        AdapterTokenStore store = (AdapterTokenStore) request.getNote(TOKEN_STORE_NOTE);
        if (store != null) {
            return store;
        }

        if (deployment.getTokenStore() == TokenStore.SESSION) {
            store = new CatalinaSessionTokenStore(request, deployment, userSessionManagement, createPrincipalFactory(), new KeycloakAuthenticatorValve());
        } else {
            store = new CatalinaCookieTokenStore(request, facade, deployment, createPrincipalFactory());
        }

        request.setNote(TOKEN_STORE_NOTE, store);
        return store;
    }

    @Override
    protected OAuthRequestAuthenticator createOAuthAuthenticator() {
        return new OAuthRequestAuthenticator(this, facade, deployment, sslRedirectPort, tokenStore);
    }

    @Override
    protected void completeOAuthAuthentication(final KeycloakPrincipal<RefreshableKeycloakSecurityContext> skp) {
        final AccessToken token = skp.getKeycloakSecurityContext().getToken();
        request.setAttribute(KEYCLOAK_ACCESS_TOKEN, token);
    }

    @Override
    protected void completeBearerAuthentication(KeycloakPrincipal<RefreshableKeycloakSecurityContext> skp, String method) {
        this.completeOAuthAuthentication(skp);
    }

    @Override
    protected String getHttpSessionId(boolean create) {
        HttpSession session = request.getSession(create);
        return session != null ? session.getId() : null;
    }

}

package org.nuxeo.ecm.platform.ui.web.auth.oauth2;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class NuxeoOAuth2Filter implements NuxeoAuthPreFilter {

    private static final Log log = LogFactory.getLog(NuxeoOAuth2Filter.class);

    protected static final String OAUTH2_SEGMENT = "/oauth2/";

    protected static final String ENDPOINT_AUTH = "authorization";

    protected static final String ENDPOINT_TOKEN = "token";

    public static String USERNAME_KEY = "nuxeo_user";

    public static String AUTHORIZATION_KEY = "authorization_key";

    public static enum ERRORS {
        invalid_request, unauthorized_client, access_denied, unsupported_response_type, invalid_scope, server_error, temporarily_unavailable
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (!isValid(request)) {
            chain.doFilter(request, response);
            return;
        }

        boolean startedTx = false;
        if (!TransactionHelper.isTransactionActive()) {
            startedTx = TransactionHelper.startTransaction();
        }
        boolean done = false;
        try {
            process(request, response, chain);

            done = true;
        } finally {
            if (startedTx) {
                if (!done) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    protected boolean isValid(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            return false;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        return httpRequest.getRequestURI().contains(OAUTH2_SEGMENT);
    }

    protected void process(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String endpoint = uri.split(OAUTH2_SEGMENT)[1];

        switch (endpoint) {
        case ENDPOINT_AUTH:
            processAuthorization(httpRequest, httpResponse, chain);
            break;
        case ENDPOINT_TOKEN:
            processToken(httpRequest, httpResponse, chain);
            break;
        default:
            chain.doFilter(request, response);
            break;
        }
    }

    protected void processAuthorization(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws IOException {
        AuthorizationRequest authRequest = AuthorizationRequest.from(request);
        String error = authRequest.checkError();
        if (isNotBlank(error)) {
            handleError(error, authRequest, response);
            return;
        }

        // Redirect to grant form
        if (request.getMethod().equals("GET")) {
            request.getSession().setAttribute(AUTHORIZATION_KEY,
                    authRequest.getAuthorizationKey());
            request.getSession().setAttribute("state", authRequest.getState());
            String base = VirtualHostHelper.getBaseURL(request);
            sendRedirect(response, base + "oauth2Grant.jsp", null);
            return;
        }

        // Ensure that authorization key is the correct one
        String authKeyForm = request.getParameter(AUTHORIZATION_KEY);
        if (!authRequest.getAuthorizationKey().equals(authKeyForm)) {
            handleError(ERRORS.access_denied, authRequest, response);
            return;
        }

        log.error("Authentication succeed !");

        // Save username in request object
        authRequest.setUsername((String) request.getSession().getAttribute(
                USERNAME_KEY));

        Map<String, String> params = new HashMap<>();
        params.put("code", authRequest.getAuthorizationCode());
        if (isNotBlank(authRequest.getState())) {
            params.put("state", authRequest.getState());
        }
        sendRedirect(response, authRequest.getRedirectUri(), params);
    }

    ClientRegistry getClientRegistry() {
        return Framework.getLocalService(ClientRegistry.class);
    }

    protected void processToken(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) {
        throw new NotImplementedException("Token endpoint not yet implemented");
    }

    protected void handleError(ERRORS error, AuthorizationRequest request,
            HttpServletResponse response) throws IOException {
        handleError(error.toString(), request, response);
    }

    protected void handleError(String error, AuthorizationRequest request,
            HttpServletResponse response) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("error", error);
        if (isNotBlank(request.getState())) {
            params.put("state", request.getState());
        }

        sendRedirect(response, request.getRedirectUri(), params);
    }

    protected void sendRedirect(HttpServletResponse response, String uri,
            Map<String, String> params) throws IOException {
        if (uri == null) {
            uri = "http://dummyurl";
        }

        StringBuilder sb = new StringBuilder(uri);
        if (params != null) {
            if (!uri.contains("?")) {
                sb.append("?");
            } else {
                sb.append("&");
            }

            for (String key : params.keySet()) {
                sb.append(key).append("=").append(params.get(key)).append("&");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        response.sendRedirect(sb.toString());
    }
}

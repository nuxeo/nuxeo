/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import static com.google.common.net.HttpHeaders.ORIGIN;
import static com.google.common.net.HttpHeaders.REFERER;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpTrace;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.thetransactioncompany.cors.CORSConfiguration;
import com.thetransactioncompany.cors.CORSFilter;
import com.thetransactioncompany.cors.Origin;

/**
 * Nuxeo CORS and CSRF filter, returning CORS configuration and preventing CSRF attacks by rejecting dubious requests.
 *
 * @since 5.7.2 for CORS
 * @since 10.1 for CSRF
 */
public class NuxeoCorsCsrfFilter implements Filter {

    private static final Log log = LogFactory.getLog(NuxeoCorsCsrfFilter.class);

    public static final String GET = HttpGet.METHOD_NAME;

    public static final String HEAD = HttpHead.METHOD_NAME;

    public static final String OPTIONS = HttpOptions.METHOD_NAME;

    public static final String TRACE = HttpTrace.METHOD_NAME;

    // safe methods according to RFC 7231 4.2.1
    protected static final Set<String> SAFE_METHODS = new HashSet<>(Arrays.asList(GET, HEAD, OPTIONS, TRACE));

    // RFC 6454
    // 6.2 If the origin is not a scheme/host/port triple, then return the string null
    // 7.3 Whenever a user agent issues an HTTP request from a "privacy-sensitive" context,
    // the user agent MUST send the value "null" in the Origin header field.
    public static final String ORIGIN_NULL = "null";

    // marker for privacy-sensitive origins
    public static final URI PRIVACY_SENSITIVE = URI.create("privacy-sensitive:///");

    public static final List<String> SCHEMES_ALLOWED = Arrays.asList("moz-extension", "chrome-extension");

    /**
     * Allows to disable strict CORS checks when a request has Origin: null.
     * <p>
     * This may happen for local files, or for a JavaScript-triggered redirect. Setting this to false may expose the
     * application to CSRF problems from files locally hosted on the user's disk.
     *
     * @since 10.3
     */
    public static final String ALLOW_NULL_ORIGIN_PROP = "nuxeo.cors.allowNullOrigin";

    /**
     * Configuration property (namespace) for CSRF tokens.
     *
     * @since 10.3
     */
    public static final String CSRF_TOKEN_NS_PROP = "nuxeo.csrf.token";

    /**
     * Allows enforcing the use of a CSRF token. Configuration property (under the {@value #CSRF_TOKEN_NS_PROP}
     * namespace).
     *
     * @since 10.3
     */
    public static final String CSRF_TOKEN_ENABLED_SUBPROP = "enabled";

    /** @since 10.3 */
    public static final String CSRF_TOKEN_ENABLED_DEFAULT = "false";

    /**
     * Allows definition of endpoints for which no CSRF token check is done. Configuration <em>list</em> property (under
     * the {@value #CSRF_TOKEN_NS_PROP} namespace).
     *
     * @since 10.3
     */
    public static final String CSRF_TOKEN_SKIP_SUBPROP = "skip";

    /**
     * Session attribute in which token is stored.
     *
     * @since 10.3
     */
    public static final String CSRF_TOKEN_ATTRIBUTE = "NuxeoCSRFToken";

    /**
     * Request header to pass a token, or fetch one.
     *
     * @since 10.3
     */
    public static final String CSRF_TOKEN_HEADER = "CSRF-Token";

    /**
     * Pseudo-value to fetch a token.
     *
     * @since 10.3
     */
    public static final String CSRF_TOKEN_FETCH = "fetch";

    /**
     * Pseudo-value to denote an invalid token.
     *
     * @since 10.3
     */
    public static final String CSRF_TOKEN_INVALID = "invalid";

    /**
     * Request parameter to pass a token.
     *
     * @since 10.3
     */
    public static final String CSRF_TOKEN_PARAM = "csrf-token";

    protected static final Random RANDOM = new SecureRandom();

    protected boolean allowNullOrigin;

    protected boolean csrfTokenEnabled;

    protected List<String> csrfTokenSkipPaths;

    @Override
    public void init(FilterConfig filterConfig) {
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        allowNullOrigin = configurationService.isBooleanTrue(ALLOW_NULL_ORIGIN_PROP);
        Map<String, Serializable> csrfTokenConfig = configurationService.getProperties(CSRF_TOKEN_NS_PROP);
        csrfTokenEnabled = Boolean.parseBoolean(StringUtils.defaultString(
                (String) csrfTokenConfig.get(CSRF_TOKEN_ENABLED_SUBPROP), CSRF_TOKEN_ENABLED_DEFAULT));
        csrfTokenSkipPaths = new ArrayList<>();
        Serializable skipPaths = csrfTokenConfig.get(CSRF_TOKEN_SKIP_SUBPROP);
        if (skipPaths instanceof String[]) {
            csrfTokenSkipPaths.addAll(Arrays.asList((String[]) skipPaths));
        }
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (manageCSRFToken(request, response)) {
            return;
        }

        RequestControllerManager service = Framework.getService(RequestControllerManager.class);
        CORSFilter corsFilter = service.getCorsFilterForRequest(request);
        CORSConfiguration corsConfig = corsFilter == null ? null : corsFilter.getConfiguration();
        String method = request.getMethod();
        URI sourceURI = getSourceURI(request);
        URI targetURI = getTargetURI(request);
        if (log.isDebugEnabled()) {
            log.debug("Method: " + method + ", source: " + sourceURI + ", target: " + targetURI);
        }

        boolean allow;
        if (isSafeMethod(method)) {
            // safe method according to RFC 7231 4.2.1
            log.debug("Safe method: allow");
            allow = true;
        } else if (sourceAndTargetMatch(sourceURI, targetURI)) {
            // source and target match, or not provided
            log.debug("Source and target match: allow");
            if (targetURI == null) {
                // misconfigured server or proxy headers
                log.error("Cannot determine target URL for CSRF check");
            }
            allow = true;
        } else if (corsConfig == null) {
            // source not known by CORS config: be safe and disallow
            log.debug("URL not covered by CORS config: disallow cross-site request");
            allow = false;
        } else if (!corsConfig.isAllowedOrigin(originFromURI(sourceURI))) {
            // not in allowed CORS origins
            log.debug("Origin not allowed by CORS config: disallow cross-site request");
            allow = false;
        } else if (!corsConfig.isSupportedMethod(method)) {
            // not in allowed CORS methods
            log.debug("Method not allowed by CORS config: disallow cross-site request");
            allow = false;
        } else {
            log.debug("Origin and method allowed by CORS config: allow cross-site request");
            allow = true;
        }

        if (allow) {
            if (corsFilter == null) {
                chain.doFilter(request, response);
            } else {
                request = maybeIgnoreWhitelistedOrigin(request);
                corsFilter.doFilter(request, response, chain);
            }
            return;
        }

        // disallowed cross-site request
        String message = "CSRF check failure";
        log.warn(message + ": source: " + sourceURI + " does not match target: " + targetURI
                + " and not allowed by CORS config");
        response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
    }

    /**
     * Check safe method according to RFC 7231 4.2.1.
     */
    protected boolean isSafeMethod(String method) {
        return SAFE_METHODS.contains(method);
    }

    /**
     * Manages the CSRF token.
     * <p>
     * This method may return a response with token fetch information or with an error if needed, in which case it will
     * return {@code true}.
     *
     * @return {@code true} if the caller doesn't need to do more work (a response has been sent)
     * @since 10.3
     */
    protected boolean manageCSRFToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!csrfTokenEnabled) {
            log.debug("No CSRF token check configured");
            return false; // no check to do
        }

        String method = request.getMethod();
        String path = request.getServletPath();
        if (path == null) {
            path = "";
        }
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            path += pathInfo;
        }
        String requestToken = request.getHeader(CSRF_TOKEN_HEADER);

        // token fetch request
        if (GET.equals(method) && path.isEmpty() && CSRF_TOKEN_FETCH.equals(requestToken)) {
            HttpSession session = request.getSession(); // create if needed
            String token = (String) session.getAttribute(CSRF_TOKEN_ATTRIBUTE);
            if (token == null) {
                token = generateNewToken();
                session.setAttribute(CSRF_TOKEN_ATTRIBUTE, token);
            }
            log.debug("Returning CSRF token fetch");
            response.setHeader(CSRF_TOKEN_HEADER, token);
            response.setStatus(SC_OK);
            return true;

        }

        // do we need to check the token?
        if (isSafeMethod(method)) {
            log.debug("No CSRF token check on safe method");
            return false;
        }

        // is the endpoint specially configured to skip the token check?
        if (csrfTokenSkipPaths.contains(path)) {
            log.debug("No CSRF token check on configured endpoint");
            return false;
        }

        // check the token
        HttpSession session = request.getSession(false);
        String token;
        if (session == null || (token = (String) session.getAttribute(CSRF_TOKEN_ATTRIBUTE)) == null) {
            log.debug("Error, no session or no CSRF token in session");
            String message = "CSRF check failure";
            log.warn(message + ": invalid token");
            response.setHeader(CSRF_TOKEN_HEADER, CSRF_TOKEN_INVALID);
            response.sendError(SC_FORBIDDEN, message);
            return true;
        }
        if (StringUtils.isEmpty(requestToken)) {
            // allow request parameter to contain the token too
            requestToken = request.getParameter(CSRF_TOKEN_PARAM);
        }
        if (!token.equals(requestToken)) {
            log.debug("Error, CSRF token does not match");
            String message = "CSRF check failure";
            log.warn(message + ": invalid token");
            response.setHeader(CSRF_TOKEN_HEADER, CSRF_TOKEN_INVALID);
            response.sendError(SC_FORBIDDEN, message);
            return true;
        }

        // token is ok, proceed
        log.debug("CSRF token matches");
        return false;
    }

    protected String generateNewToken() {
        return RandomStringUtils.random(40, 0, 0, true, true, null, RANDOM);
    }

    /**
     * Gets the source URI: the URI of the page from which the request is actually coming.
     * <p>
     * {@code null} is returned is there is no header.
     * <p>
     * {@link #PRIVACY_SENSITIVE} is returned is there is a null origin (RFC 6454 7.3, "privacy-sensitive" context)
     * unless configured to be ignored.
     */
    public URI getSourceURI(HttpServletRequest request) {
        String source = request.getHeader(ORIGIN);
        if (isBlank(source)) {
            source = request.getHeader(REFERER);
        }
        if (isBlank(source)) {
            return null;
        }
        source = source.trim();
        if (ORIGIN_NULL.equals(source)) {
            return allowNullOrigin ? null : PRIVACY_SENSITIVE;
        }
        if (source.contains(" ")) {
            // RFC 6454 7.1 origin-list
            // keep only the first origin to simplify the logic (nobody sends two origins anyway)
            source = source.substring(0, source.indexOf(' '));
        }
        try {
            return new URI(source); // NOSONAR (URI is not opened as a stream)
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /** Gets the target URI: the URI to which the browser is connecting. */
    public URI getTargetURI(HttpServletRequest request) {
        String baseURL = VirtualHostHelper.getServerURL(request, false);
        if (baseURL == null) {
            return null;
        }
        try {
            return new URI(baseURL); // NOSONAR (URI is not opened as a stream)
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public boolean sourceAndTargetMatch(URI sourceURI, URI targetURI) {
        if (sourceURI == null || targetURI == null) {
            return true;
        }
        if (isWhitelistedScheme(sourceURI)) {
            return true;
        }
        return Objects.equals(sourceURI.getScheme(), targetURI.getScheme()) //
                && Objects.equals(sourceURI.getHost(), targetURI.getHost()) //
                && sourceURI.getPort() == targetURI.getPort();
    }

    /**
     * Gets an Origin from a URI. Strips the path and query (which may be present in Referer headers).
     */
    protected Origin originFromURI(URI uri) {
        // remove path, query and fragment
        try {
            uri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            // keep passed-in URI
        }
        return new Origin(uri.toString());
    }

    protected HttpServletRequest maybeIgnoreWhitelistedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(ORIGIN);
        if (origin == null) {
            return request;
        }
        URI uri;
        try {
            uri = new URI(origin); // NOSONAR (URI is not opened as a stream)
        } catch (URISyntaxException e) {
            return request;
        }
        if (!isWhitelistedScheme(uri)) {
            return request;
        }
        // wrap request to pretend that the Origin is absent
        return new IgnoredOriginRequestWrapper(request);
    }

    protected boolean isWhitelistedScheme(URI uri) {
        return SCHEMES_ALLOWED.contains(uri.getScheme());
    }

    /**
     * Wrapper for the request to hide the Origin header.
     *
     * @since 10.2
     */
    public static class IgnoredOriginRequestWrapper extends HttpServletRequestWrapper {

        public IgnoredOriginRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            if (ORIGIN.equalsIgnoreCase(name)) {
                return null;
            }
            return super.getHeader(name);
        }
    }

}

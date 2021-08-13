/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.FORCE_ANONYMOUS_LOGIN;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGINCONTEXT_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGOUT_PAGE;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REQUESTED_URL;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.SECURITY_ERROR;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ErrorHandler;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author arussel
 */
public class DefaultNuxeoExceptionHandler implements NuxeoExceptionHandler {

    private static final Log log = LogFactory.getLog(DefaultNuxeoExceptionHandler.class);

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected NuxeoExceptionHandlerParameters parameters = new NuxeoExceptionHandlerParameters();

    @Override
    public void setParameters(NuxeoExceptionHandlerParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Puts a marker in request to avoid looping over the exception handling mechanism
     *
     * @throws ServletException if request has already been marked as handled. The initial exception is then wrapped.
     */

    protected void startHandlingException(HttpServletRequest request, HttpServletResponse response, Throwable t)
            throws ServletException {
        if (request.getAttribute(EXCEPTION_HANDLER_MARKER) == null) {
            if (log.isDebugEnabled()) {
                log.debug("Initial exception", t);
            }
            // mark request as already processed by this mechanism to avoid
            // looping over it
            request.setAttribute(EXCEPTION_HANDLER_MARKER, true);
            // disable further redirect by nuxeo url system
            request.setAttribute(DISABLE_REDIRECT_REQUEST_KEY, true);
        } else {
            // avoid looping over exception mechanism
            throw new ServletException(t);
        }
    }

    @Override
    public void handleException(HttpServletRequest request, HttpServletResponse response, Throwable t)
            throws IOException, ServletException {

        Throwable unwrappedException = ExceptionHelper.unwrapException(t);

        // check for Anonymous case
        if (ExceptionHelper.isSecurityError(unwrappedException)) {
            Principal principal = getPrincipal(request);
            if (principal instanceof NuxeoPrincipal) {
                NuxeoPrincipal nuxeoPrincipal = (NuxeoPrincipal) principal;
                if (nuxeoPrincipal.isAnonymous()) {
                    // redirect to login than to requested page
                    if (handleAnonymousException(request, response)) {
                        return;
                    }
                }
            }
        }

        int defaultStatus;
        Throwable cause = t;
        while (cause != null && !(cause instanceof NuxeoException)) {
            cause = cause.getCause();
        }
        if (cause instanceof NuxeoException) {
            defaultStatus = ((NuxeoException) cause).getStatusCode();
        } else {
            defaultStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        startHandlingException(request, response, t);
        try {
            ErrorHandler handler = getHandler(t);
            Integer code = handler.getCode();
            int status = code == null ? defaultStatus : code.intValue();
            Log logger = parameters.getLogger();
            parameters.getListener().startHandling(t, request, response);

            StringWriter swriter = new StringWriter();
            PrintWriter pwriter = new PrintWriter(swriter);
            t.printStackTrace(pwriter);
            String stackTrace = swriter.getBuffer().toString();
            if (DownloadHelper.isClientAbortError(t)) {
                DownloadHelper.logClientAbort(t);
            } else if (status < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                log.debug(t, t);
            } else {
                log.error(t, t);
                if (logger != null) {
                    logger.error(t, t);
                }
            }

            parameters.getListener().beforeSetErrorPageAttribute(unwrappedException, request, response);
            request.setAttribute("exception_message", unwrappedException.getLocalizedMessage());
            String message = handler.getMessage();
            if (message == null) {
                message = "Error.Unknown";
            }
            String bundleName = parameters.getBundleName();
            if (isNotBlank(bundleName)) {
                Locale locale = request.getLocale();
                request.setAttribute("messageBundle",
                        ResourceBundle.getBundle(bundleName, locale, Thread.currentThread().getContextClassLoader()));
                message = I18NUtils.getMessageString(bundleName, message, null, locale);
            }
            request.setAttribute("user_message", message);
            request.setAttribute("securityError", ExceptionHelper.isSecurityError(unwrappedException));
            String dumpedRequest = parameters.getRequestDumper().getDump(request);
            if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR && logger != null) {
                logger.error(dumpedRequest);
            }
            request.setAttribute("isDevModeSet", Framework.isDevModeSet());
            if (Framework.isDevModeSet()) {
                request.setAttribute("stackTrace", stackTrace);
                request.setAttribute("request_dump", dumpedRequest);
            }

            parameters.getListener().beforeForwardToErrorPage(unwrappedException, request, response);
            if (!response.isCommitted()) {
                // The JSP error page needs the response Writer but somebody may already have retrieved
                // the OutputStream and usage of these two can't be mixed. So we reset the response.
                response.reset();
                response.setStatus(status);
                String errorPage = defaultString(handler.getPage(), parameters.getDefaultErrorPage());
                String accept = request.getHeader("Accept");
                // client request application/json or missing config / unit tests
                // do a JSON output, it's a borderline case for an exception during filters cleanup
                if ((accept != null && accept.contains(MediaType.APPLICATION_JSON)) || isBlank(errorPage)) {
                    writeExceptionAsJson(response, status, unwrappedException);
                } else {
                    RequestDispatcher requestDispatcher = request.getRequestDispatcher(errorPage);
                    if (requestDispatcher != null) {
                        requestDispatcher.forward(request, response);
                    } else {
                        log.error("Cannot forward to error page, no RequestDispatcher found for errorPage=" + errorPage
                                + " handler=" + handler);
                    }
                }
                parameters.getListener().responseComplete();
            } else if (!DownloadHelper.isClientAbortError(t)){
                // do not throw an error, just log it: afterDispatch needs to be called
                log.error("Cannot forward to error page: response is already committed", t);
            }
            parameters.getListener().afterDispatch(unwrappedException, request, response);
        } catch (ServletException e) {
            throw e;
        } catch (RuntimeException | IOException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public boolean handleAnonymousException(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        PluggableAuthenticationService authService = (PluggableAuthenticationService) Framework.getRuntime()
                                                                                               .getComponent(
                                                                                                       PluggableAuthenticationService.NAME);
        if (authService == null) {
            return false;
        }
        authService.invalidateSession(request);
        String loginURL = getLoginURL(request);
        if (loginURL == null) {
            return false;
        }
        if (!response.isCommitted()) {
            request.setAttribute(DISABLE_REDIRECT_REQUEST_KEY, true);
            response.sendRedirect(loginURL);
            parameters.getListener().responseComplete();
        } else {
            log.error("Cannot redirect to login page: response is already committed");
        }
        return true;
    }

    @Override
    public String getLoginURL(HttpServletRequest request) {
        PluggableAuthenticationService authService = (PluggableAuthenticationService) Framework.getRuntime()
                                                                                               .getComponent(
                                                                                                       PluggableAuthenticationService.NAME);
        Map<String, String> urlParameters = new HashMap<>();
        urlParameters.put(SECURITY_ERROR, "true");
        urlParameters.put(FORCE_ANONYMOUS_LOGIN, "true");
        if (request.getAttribute(REQUESTED_URL) != null) {
            urlParameters.put(REQUESTED_URL, (String) request.getAttribute(REQUESTED_URL));
        } else {
            urlParameters.put(REQUESTED_URL, NuxeoAuthenticationFilter.getRequestedUrl(request));
        }
        String baseURL = authService.getBaseURL(request) + LOGOUT_PAGE;
        return URIUtils.addParametersToURIQuery(baseURL, urlParameters);
    }

    protected ErrorHandler getHandler(Throwable t) {
        Throwable throwable = ExceptionHelper.unwrapException(t);
        String className = throwable.getClass().getName();
        for (ErrorHandler handler : parameters.getHandlers()) {
            if (handler.getError() != null && className.matches(handler.getError())) {
                return handler;
            }
        }
        return new ErrorHandler();
    }

    protected Principal getPrincipal(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            LoginContext loginContext = (LoginContext) request.getAttribute(LOGINCONTEXT_KEY);
            principal = Optional.ofNullable(loginContext)
                                .map(LoginContext::getSubject)
                                .map(Subject::getPrincipals)
                                .flatMap(principals -> principals.stream().findFirst())
                                .orElse(null);
        }
        return principal;
    }

    protected void writeExceptionAsJson(HttpServletResponse response, int status, Throwable e) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON);
        try (PrintWriter w = response.getWriter(); //
                JsonGenerator jg = MAPPER.getFactory().createGenerator(w)) {
            jg.writeStartObject();
            jg.writeStringField("entity-type", "exception");
            jg.writeNumberField("status", status);
            jg.writeStringField("message", getExceptionMessage(e.getMessage(), status));
            jg.writeEndObject();
            jg.flush();
        }
    }

    protected String getExceptionMessage(String message, int status) {
        if (status < SC_INTERNAL_SERVER_ERROR || Framework.isDevModeSet()) {
            return defaultString(message, "");
        } else {
            return "Internal Server Error";
        }
    }

}

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

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.FORCE_ANONYMOUS_LOGIN;
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
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.WrappedException;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor.ErrorHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 */
public class DefaultNuxeoExceptionHandler implements NuxeoExceptionHandler {

    private static final Log log = LogFactory.getLog(DefaultNuxeoExceptionHandler.class);

    protected NuxeoExceptionHandlerParameters parameters;

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
            Principal principal = request.getUserPrincipal();
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

        startHandlingException(request, response, t);
        try {
            ErrorHandler handler = getHandler(t);
            Integer code = handler.getCode();
            int status = code == null ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : code.intValue();
            parameters.getListener().startHandling(t, request, response);

            StringWriter swriter = new StringWriter();
            PrintWriter pwriter = new PrintWriter(swriter);
            t.printStackTrace(pwriter);
            String stackTrace = swriter.getBuffer().toString();
            if (status < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) { // 500
                log.debug(t.getMessage(), t);
            } else {
                log.error(stackTrace);
                parameters.getLogger().error(stackTrace);
            }

            parameters.getListener().beforeSetErrorPageAttribute(unwrappedException, request, response);
            request.setAttribute("exception_message", unwrappedException.getLocalizedMessage());
            request.setAttribute("user_message", getUserMessage(handler.getMessage(), request.getLocale()));
            request.setAttribute("securityError", ExceptionHelper.isSecurityError(unwrappedException));
            request.setAttribute("messageBundle", ResourceBundle.getBundle(parameters.getBundleName(),
                    request.getLocale(), Thread.currentThread().getContextClassLoader()));
            String dumpedRequest = parameters.getRequestDumper().getDump(request);
            if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) { // 500
                parameters.getLogger().error(dumpedRequest);
            }
            request.setAttribute("isDevModeSet", Framework.isDevModeSet());
            if (Framework.isDevModeSet()) {
                request.setAttribute("stackTrace", stackTrace);
                request.setAttribute("request_dump", dumpedRequest);
            }

            parameters.getListener().beforeForwardToErrorPage(unwrappedException, request, response);
            if (!response.isCommitted()) {
                response.setStatus(status);
                String errorPage = handler.getPage();
                errorPage = (errorPage == null) ? parameters.getDefaultErrorPage() : errorPage;
                RequestDispatcher requestDispatcher = request.getRequestDispatcher(errorPage);
                if (requestDispatcher != null) {
                    requestDispatcher.forward(request, response);
                } else {
                    log.error("Cannot forward to error page, " + "no RequestDispatcher found for errorPage=" + errorPage
                            + " handler=" + handler);
                }
                parameters.getListener().responseComplete();
            } else {
                // do not throw an error, just log it: afterDispatch needs to
                // be called, and sometimes the initial error is a
                // ClientAbortException
                log.error("Cannot forward to error page: " + "response is already committed");
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
        String className = null;
        if (throwable instanceof WrappedException) {
            WrappedException wrappedException = (WrappedException) throwable;
            className = wrappedException.getClassName();
        } else {
            className = throwable.getClass().getName();
        }
        for (ErrorHandler handler : parameters.getHandlers()) {
            if (handler.getError() != null && className.matches(handler.getError())) {
                return handler;
            }
        }
        throw new NuxeoException("No error handler set.");
    }

    protected Object getUserMessage(String messageKey, Locale locale) {
        return I18NUtils.getMessageString(parameters.getBundleName(), messageKey, null, locale);
    }

}

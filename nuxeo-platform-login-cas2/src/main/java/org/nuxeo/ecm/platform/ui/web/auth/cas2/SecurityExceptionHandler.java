package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.DefaultNuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.runtime.api.Framework;

public class SecurityExceptionHandler extends DefaultNuxeoExceptionHandler {

    public static final String COOKIE_URL_TO_REACH = "cookie.name.url.to.reach.from.sso";
    public static final String CAS_REDIRECTION_URL = "/cas2.jsp";
    Cas2Authenticator cas2Authenticator = null;
    
    public SecurityExceptionHandler() throws Exception {
        super();
    }
    
    public void handleException(HttpServletRequest request,
            HttpServletResponse response, Throwable t) throws IOException,
            ServletException {
            
        
        Throwable unwrappedException = unwrapException(t);
        
        if ((! ExceptionHelper.isSecurityError(unwrappedException))
                && (! response.containsHeader(COOKIE_URL_TO_REACH))) {
            super.handleException(request, response, t);
            return;
        }

        response.resetBuffer();

        if (!response.containsHeader(COOKIE_URL_TO_REACH)) {
            Cookie cookieUrlToReach = new Cookie(COOKIE_URL_TO_REACH, request.getRequestURL().toString() + "?" + request.getQueryString());
            cookieUrlToReach.setPath("/");
            cookieUrlToReach.setMaxAge(-1);
            response.addCookie(cookieUrlToReach);

            Cookie cookieUrlToAuthenticate;
            try {
                cookieUrlToAuthenticate = new Cookie(COOKIE_URL_TO_REACH,  getCasAuthenticator().getServiceURL(request, CAS2Parameters.SERVICE_LOGIN_URL_KEY) + "?service=" + getCasAuthenticator().getAppURL(request));
                cookieUrlToAuthenticate.setPath("/");
                cookieUrlToAuthenticate.setMaxAge(-1);
                response.addCookie(cookieUrlToAuthenticate);
            } catch (ClientException e) {
                log.error("can't get CAS URL to authenticate user", e);
            }
        }
        
        request.getRequestDispatcher(CAS_REDIRECTION_URL).forward(request, response);
        FacesContext.getCurrentInstance().responseComplete();
        
    }
    
    protected Cas2Authenticator getCasAuthenticator() throws ClientException {
        
        if (cas2Authenticator == null) {
            
            PluggableAuthenticationService service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                    PluggableAuthenticationService.NAME);
            
            if (service == null) {
                throw new ClientException(
                        "Can't initialize Nuxeo Pluggable Authentication Service");
            }
            
            cas2Authenticator = (Cas2Authenticator) service.getPlugin("CAS2_AUTH");
            
            if (cas2Authenticator == null) {
                throw new ClientException(
                        "Can't get CAS authenticator");
            }
            
            
        }
        return cas2Authenticator;
    }


}

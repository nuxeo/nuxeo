package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CAS2Filter implements Filter {
    
    Log log = LogFactory.getLog(CAS2Filter.class);

    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Cookie[] cookies = httpRequest.getCookies();
        for (Cookie cookie : cookies) {
            if (SecurityExceptionHandler.COOKIE_URL_TO_REACH.equals(cookie.getName())) {
                String urlToReach = cookie.getValue();
                
                log.debug("Redirection - User asking this URL :" + urlToReach);
                try {
                    httpResponse.resetBuffer();
                    
//                    Cookie cookieUrlToReach = new Cookie(SecurityExceptionHandler.COOKIE_URL_TO_REACH, null);
//                    httpResponse.addCookie(cookieUrlToReach);
                    httpRequest.getRequestDispatcher("/cas2backToRequestedURL.jsp").forward(request, response);
                    break;
                } catch (Exception e) {
                    log.debug("Redirection failed", e);
                }
            }
        }
        
        chain.doFilter(request, response);
        
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub
        
    }

}

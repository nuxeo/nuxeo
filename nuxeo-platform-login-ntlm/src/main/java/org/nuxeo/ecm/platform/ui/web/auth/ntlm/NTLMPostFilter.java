package org.nuxeo.ecm.platform.ui.web.auth.ntlm;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Manage NTLM "Protected POST"
 *
 * see : http://jcifs.samba.org/src/docs/ntlmhttpauth.html
 *       http://curl.haxx.se/rfc/ntlm.html
 *
 * @author Thierry Delprat
 */
public class NTLMPostFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            if ("POST".equals(httpRequest.getMethod())) {
                String ntlmHeader = httpRequest.getHeader("Authorization");
                if (ntlmHeader!=null && ntlmHeader.startsWith("NTLM") && httpRequest.getContentLength()==0) {
                    handleNtlmPost(httpRequest, (HttpServletResponse) response, ntlmHeader);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    protected void handleNtlmPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String ntlmHeader) throws IOException, ServletException {
        NTLMAuthenticator.negotiate(httpRequest, httpResponse, true);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // NOP
    }

    public void destroy() {
        // NOP
    }

}

package org.nuxeo.ecm.platform.ui.web.auth.simple;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;

public class MockFilterChain implements FilterChain {
    
    PluggableAuthenticationService pas;
    
    protected PluggableAuthenticationService getPAS() {
        if (pas == null) {
            pas = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                    PluggableAuthenticationService.NAME);
            
        }
        return pas;
    }
       

    public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
 
//        for (String filterName : getPAS().getAuthChain()) {
//            NuxeoAuthenticationPlugin filter = getPAS().getPlugin(filterName);
//            
//            filter.handleLoginPrompt(httpRequest, httpResponse, baseURL)
//        }
    }

}

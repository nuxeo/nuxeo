/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.runtime.api.Framework;
import com.thetransactioncompany.cors.CORSFilter;

/**
 * Nuxeo CORS filter wrapper to com.thetransactioncompany.cors.CORSFilter allowing to configure cors filter depending of
 * the request url. Each time a request matchs a contribution is found, CORSFilter had to be re-initialized to change
 * his configurations.
 * 
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.2
 */
public class NuxeoCorsFilter extends CORSFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        FilterConfig filterConfig = getFilterConfigFrom(request);
        if (filterConfig != null) {
            super.init(filterConfig);
            super.doFilter(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    protected FilterConfig getFilterConfigFrom(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            return null;
        }
        return Framework.getService(RequestControllerManager.class).getCorsConfigForRequest(
                (HttpServletRequest) request);
    }
}

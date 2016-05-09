/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
        return Framework.getLocalService(RequestControllerManager.class).getCorsConfigForRequest(
                (HttpServletRequest) request);
    }
}

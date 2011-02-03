/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * The Authentication filter already provides a complexe plugin model.
 * Unfortunatly in some cases, it's easier to have a dedciated filter to
 * implment the custom auth logic. But in this case, you have to configure the
 * new filter for each url pattern that is already protected by
 * NuxeoAuthenticationFilter.
 *
 * In order to avoid that you can run your Filter as a pre-Filter for the
 * NuxeoAUthenticationFilter. For that you need to implement this interface and
 * register your implementation via the preFilter extension point.
 *
 * @author tiry
 *
 */
public interface NuxeoAuthPreFilter {

    /**
     * Main Filter method @See {@see Filter}
     *
     * The FilterChain is only composed of the preFilters and the
     * NuxeoAuthenticationFilter
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     */
    void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException;

}

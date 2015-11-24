/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

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

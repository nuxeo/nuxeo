/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.theme.bank;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.app.WebEngineFilter;

public class ThemeBankWebEngineFilter extends WebEngineFilter {

    private final String THEME_BANK_PATH = "/theme-banks";

    @Override
    public void preRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String pathInfo = request.getPathInfo();
        if (!pathInfo.startsWith(THEME_BANK_PATH)) {
            response.sendError(404);
        }
        super.preRequest(request, response);
    }

}

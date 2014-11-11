/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.auth.service;

import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public interface LoginProviderLinkComputer {

    /**
     * Compute Url that should be used to login via this login provider.
     * 
     * Because the url can depend onb the context, it is computed by this method
     * rather than using a static property
     * 
     * @param req
     * @param requestedUrl
     * @return
     * @since 5.7
     */
    String computeUrl(HttpServletRequest req, String requestedUrl);
}

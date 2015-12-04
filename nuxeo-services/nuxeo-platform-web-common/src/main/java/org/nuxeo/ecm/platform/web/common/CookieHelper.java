/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.web.common;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.runtime.api.Framework;

/**
 * Helpers for Cookies.
 *
 * @since 8.1
 */
public class CookieHelper {

    private CookieHelper() {
        // helper class
    }

    public static Cookie createCookie(HttpServletRequest request, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(request.getContextPath());
        if (!Framework.isDevModeSet()) {
            cookie.setSecure(true);
        }
        return cookie;
    }
}

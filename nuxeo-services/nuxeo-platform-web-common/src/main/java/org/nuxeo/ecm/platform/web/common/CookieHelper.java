/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

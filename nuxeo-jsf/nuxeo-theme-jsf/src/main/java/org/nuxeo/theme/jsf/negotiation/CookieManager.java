/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.jsf.negotiation;

import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public final class CookieManager {

    public static String getCookie(final String name,  final ExternalContext context) {
        if (context == null) {
            return null;
        }
        final Map<String, Object> cookies = context.getRequestCookieMap();
        final Cookie cookie = (Cookie) cookies.get(name);
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }

    public static void setCookie(String name, String value, final ExternalContext context) {
        if (context == null) {
            return;
        }
        HttpServletResponse response = (HttpServletResponse) context.getResponse();
        final Cookie cookie = new Cookie(name, value);
        response.addCookie(cookie);
    }

    public static void expireCookie(String name,  final ExternalContext context) {
        if (context == null) {
            return;
        }
        HttpServletResponse response = (HttpServletResponse) context.getResponse();
        final Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

}

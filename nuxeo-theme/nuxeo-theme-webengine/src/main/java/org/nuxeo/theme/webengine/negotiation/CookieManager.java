/*
 * (C) Copyright 2006-2008 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.webengine.negotiation;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.WebContext;

public final class CookieManager {

    public static String getCookie(final String name, final WebContext context) {
        if (context == null) {
            return null;
        }
        Cookie[] cookies = context.getRequest().getCookies();
        Cookie cookie = null;
        for (Cookie c : cookies) {            
            if (name.equals(c.getName())) {
                cookie = c;
                break;
            }
        }
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }

    public static void setCookie(String name, String value,
            final WebContext context) {
        if (context == null) {
            return;
        }
        HttpServletResponse response = (HttpServletResponse) context.getResponse();
        final String path = context.getBasePath();
        final Cookie cookie = new Cookie(name, value);
        cookie.setPath(path);
        response.addCookie(cookie);
    }

    public static void expireCookie(String name, final WebContext context) {
        if (context == null) {
            return;
        }
        HttpServletResponse response = (HttpServletResponse) context.getResponse();
        final String path = context.getBasePath();
        final Cookie cookie = new Cookie(name, "");
        cookie.setPath(path);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

}

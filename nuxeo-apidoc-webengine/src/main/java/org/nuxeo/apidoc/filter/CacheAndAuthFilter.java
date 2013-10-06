/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.filter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.ui.web.auth.plugins.AnonymousAuthenticator;
import org.nuxeo.runtime.api.Framework;

public class CacheAndAuthFilter extends BaseApiDocFilter {

    public static final DateFormat HTTP_EXPIRES_DATE_FORMAT = httpExpiresDateFormat();

    protected Boolean forceAnonymous;

    protected boolean forceAnonymous() {
        if (forceAnonymous == null) {
            forceAnonymous = Boolean.valueOf(Framework.isBooleanPropertyTrue("org.nuxeo.apidoc.forceanonymous"));
        }
        return forceAnonymous.booleanValue();
    }

    @Override
    protected void internalDoFilter(ServletRequest request,
            ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        boolean activateCaching = false;
        String anonymousHeader = httpRequest.getHeader("X-NUXEO-ANONYMOUS-ACCESS");
        if ("true".equals(anonymousHeader) || forceAnonymous()) {
            // activate cache
            activateCaching = true;
        } else {
            // deactivate anonymous login
            httpRequest.setAttribute(
                    AnonymousAuthenticator.BLOCK_ANONYMOUS_LOGIN_KEY,
                    Boolean.TRUE);
        }

        if (activateCaching) {
            addCacheHeader(httpResponse, false, "600");
        }

        chain.doFilter(httpRequest, httpResponse);

    }

    private static DateFormat httpExpiresDateFormat() {
        // formatted http Expires: Thu, 01 Dec 1994 16:00:00 GMT
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df;
    }

    public static void addCacheHeader(HttpServletResponse httpResponse,
            boolean isPrivate, String cacheTime) {
        if (isPrivate) {
            httpResponse.addHeader("Cache-Control", "private, max-age="
                    + cacheTime);
        } else {
            httpResponse.addHeader("Cache-Control", "public, max-age="
                    + cacheTime);
        }

        // Generating expires using current date and adding cache time.
        // we are using the format Expires: Thu, 01 Dec 1994 16:00:00 GMT
        Date date = new Date();
        long newDate = date.getTime() + Long.parseLong(cacheTime) * 1000;
        date.setTime(newDate);

        httpResponse.setHeader("Expires", HTTP_EXPIRES_DATE_FORMAT.format(date));
    }

}

/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.filter;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.FastDateFormat;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.AnonymousAuthenticator;
import org.nuxeo.runtime.api.Framework;

public class CacheAndAuthFilter extends BaseApiDocFilter {

    // formatted http Expires: Thu, 01 Dec 1994 16:00:00 GMT
    public static final FastDateFormat HTTP_EXPIRES_DATE_FORMAT = FastDateFormat.getInstance(
            "EEE, dd MMM yyyy HH:mm:ss z", TimeZone.getTimeZone("GMT"), Locale.US);

    protected Boolean forceAnonymous;

    protected boolean forceAnonymous() {
        if (forceAnonymous == null) {
            forceAnonymous = Boolean.valueOf(Framework.isBooleanPropertyTrue("org.nuxeo.apidoc.forceanonymous"));
        }
        return forceAnonymous.booleanValue();
    }

    @Override
    protected void internalDoFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        boolean activateCaching = false;
        String anonymousHeader = httpRequest.getHeader("X-NUXEO-ANONYMOUS-ACCESS");
        if ("true".equals(anonymousHeader) || forceAnonymous()) {
            // activate cache
            activateCaching = true;
        } else {
            // deactivate anonymous login
            httpRequest.setAttribute(AnonymousAuthenticator.BLOCK_ANONYMOUS_LOGIN_KEY, Boolean.TRUE);
        }

        if (activateCaching) {
            addCacheHeader(httpResponse, false, "600");
        }

        chain.doFilter(httpRequest, httpResponse);

    }

    public static void addCacheHeader(HttpServletResponse httpResponse, boolean isPrivate, String cacheTime) {
        if (isPrivate) {
            httpResponse.addHeader("Cache-Control", "private, max-age=" + cacheTime);
        } else {
            httpResponse.addHeader("Cache-Control", "public, max-age=" + cacheTime);
        }

        // Generating expires using current date and adding cache time.
        // we are using the format Expires: Thu, 01 Dec 1994 16:00:00 GMT
        Date date = new Date();
        long newDate = date.getTime() + Long.parseLong(cacheTime) * 1000;
        date.setTime(newDate);

        httpResponse.setHeader("Expires", HTTP_EXPIRES_DATE_FORMAT.format(date));
    }

}

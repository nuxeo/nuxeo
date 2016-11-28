/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.cache;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.NuxeoRequestControllerFilter;

/**
 * Adding http cache header (Cache-Control : max-age AND Expire) to the response.
 *
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 * @deprecated since 7.10 caching handled by {@link NuxeoRequestControllerFilter}
 */
@Deprecated
public class SimpleCacheFilter implements Filter {

    private String cacheTime = "3599";

    public static final DateFormat HTTP_EXPIRES_DATE_FORMAT = httpExpiresDateFormat();

    private static DateFormat httpExpiresDateFormat() {
        // formatted http Expires: Thu, 01 Dec 1994 16:00:00 GMT
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (httpRequest.getMethod().equals("GET")) {
            addCacheHeader(httpResponse, cacheTime);
        }
        chain.doFilter(request, response);
    }

    public static void addCacheHeader(HttpServletResponse httpResponse, String cacheTime) {
        httpResponse.addHeader("Cache-Control", "max-age=" + cacheTime);
        httpResponse.addHeader("Cache-Control", "public");

        // Generating expires using current date and adding cache time.
        // we are using the format Expires: Thu, 01 Dec 1994 16:00:00 GMT
        Date date = new Date();
        long newDate = date.getTime() + Long.parseLong(cacheTime) * 1000;
        date.setTime(newDate);

        httpResponse.setHeader("Expires", HTTP_EXPIRES_DATE_FORMAT.format(date));
    }

    public void destroy() {
        cacheTime = null;
    }

    public void init(FilterConfig conf) throws ServletException {
        cacheTime = conf.getInitParameter("cacheTime");
    }

}

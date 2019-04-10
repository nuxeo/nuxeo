/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.wss.handlers.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.WSSConfig;

public class ResourcesHandler {

    protected static final int BUFFER_SIZE = 1024 * 10;

    public static final DateFormat HTTP_EXPIRES_DATE_FORMAT = httpExpiresDateFormat();

    private static final Log log = LogFactory.getLog(ResourcesHandler.class);

    private static DateFormat httpExpiresDateFormat() {
        // formatted http Expires: Thu, 01 Dec 1994 16:00:00 GMT
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df;
    }

    public static InputStream getResourceAsStream(String resourceSubPath) {
        String resourcePath = WSSConfig.instance().getResourcesBasePath() + resourceSubPath;
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        return is;
    }

    protected void addCacheHeaders(HttpServletResponse response, int cacheTime) {
        response.addHeader("Cache-Control", "max-age=" + cacheTime);
        response.addHeader("Cache-Control", "public");

        Date date = new Date();
        long newDate = date.getTime() + new Long(cacheTime) * 1000;
        date.setTime(newDate);

        response.setHeader("Expires", HTTP_EXPIRES_DATE_FORMAT.format(date));
    }

    public void handleResource(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        String uri = request.getRequestURI();
        log.debug("Handling resource call on uri = " + uri);

        String[] parts = uri.split(WSSConfig.instance().getResourcesUrlPattern());
        String resourceSubPath = parts[parts.length - 1];
        InputStream is = getResourceAsStream(resourceSubPath);

        if (is == null && resourceSubPath.contains("icons/")) {
            is = getResourceAsStream("icons/file.gif");
        }

        if (is == null) {
            log.warn("Resource not found for uri " + uri);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        addCacheHeaders(response, 600);

        try {
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = is.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
            }
        } catch (Exception e) {
            log.error("Error while serving resource", e);
            throw new ServletException(e);
        } finally {
            if (response != null) {
                response.flushBuffer();
            }
            if (is != null) {
                is.close();
            }
        }
    }

}

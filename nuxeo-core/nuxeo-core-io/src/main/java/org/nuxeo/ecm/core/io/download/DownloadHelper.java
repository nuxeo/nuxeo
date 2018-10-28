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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.io.download;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.core.io.download.DownloadService.ByteRange;

/**
 * Helper class related to the download service.
 *
 * @since 7.3
 */
public class DownloadHelper {

    private static final Log log = LogFactory.getLog(DownloadHelper.class);

    public static final String INLINE = "inline";

    // tomcat catalina
    private static final String CLIENT_ABORT_EXCEPTION = "ClientAbortException";

    // jetty (with CamelCase "Eof")
    private static final String EOF_EXCEPTION = "EofException";

    // utility class
    private DownloadHelper() {
    }

    /**
     * Parses a byte range.
     *
     * @param range the byte range as a string
     * @param length the file length
     * @return the byte range, or {@code null} if it couldn't be parsed.
     */
    public static ByteRange parseRange(String range, long length) {
        try {
            // TODO does no support multiple ranges
            if (!range.startsWith("bytes=") || range.indexOf(',') >= 0) {
                return null;
            }
            int i = range.indexOf('-', 6);
            if (i < 0) {
                return null;
            }
            String start = range.substring(6, i).trim();
            String end = range.substring(i + 1).trim();
            long rangeStart = 0;
            long rangeEnd = length - 1;
            if (start.isEmpty()) {
                if (end.isEmpty()) {
                    return null;
                }
                rangeStart = length - Long.parseLong(end);
                if (rangeStart < 0) {
                    rangeStart = 0;
                }
            } else {
                rangeStart = Long.parseLong(start);
                if (!end.isEmpty()) {
                    rangeEnd = Long.parseLong(end);
                }
            }
            if (rangeStart > rangeEnd) {
                return null;
            }
            return new ByteRange(rangeStart, rangeEnd);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Generates a {@code Content-Disposition} string based on the servlet request for a given filename.
     * <p>
     * The value follows RFC2231.
     *
     * @param request the http servlet request
     * @param filename the filename
     * @return a full string to set as value of a {@code Content-Disposition} header
     */
    public static String getRFC2231ContentDisposition(HttpServletRequest request, String filename) {
        return getRFC2231ContentDisposition(request, filename, null);
    }

    /**
     * Generates a {@code Content-Disposition} string for a given filename.
     * <p>
     * The value follows RFC2231.
     *
     * @param request the http servlet request
     * @param filename the filename
     * @param inline how to set the content disposition; {@code TRUE} for {@code inline}, {@code FALSE} for
     *            {@code attachment}, or {@code null} to detect from {@code inline} request parameter or attribute
     * @return a full string to set as value of a {@code Content-Disposition} header
     * @since 7.10
     */
    public static String getRFC2231ContentDisposition(HttpServletRequest request, String filename, Boolean inline) {
        String userAgent = request.getHeader("User-Agent");
        boolean binline;
        if (inline == null) {
            String inlineParam = request.getParameter(INLINE);
            if (inlineParam == null) {
                inlineParam = (String) request.getAttribute(INLINE);
            }
            binline = inlineParam != null && !"false".equals(inlineParam);
        } else {
            binline = inline.booleanValue();
        }
        return RFC2231.encodeContentDisposition(filename, binline, userAgent);
    }

    public static boolean isClientAbortError(Throwable t) {
        int loops = 20; // no infinite loop
        while (t != null && loops > 0) {
            if (t instanceof IOException) {
                // handle all IOException that are ClientAbortException by looking
                // at their class name since the package name is not the same for
                // jboss, glassfish, tomcat and jetty and we don't want to add
                // implementation specific build dependencies to this project
                String name = t.getClass().getSimpleName();
                if (CLIENT_ABORT_EXCEPTION.equals(name) || EOF_EXCEPTION.equals(name)) {
                    return true;
                }
            }
            loops--;
            t = t.getCause();
        }
        return false;
    }

    public static void logClientAbort(Throwable t) {
        log.debug("Client disconnected: " + unwrapException(t).getMessage());
    }

    private static Throwable unwrapException(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /**
     * Re-throws the passed exception except if it corresponds to a client disconnect, for which logging doesn't bring
     * us anything.
     *
     * @param e the original exception
     * @throws IOException if this is not a client disconnect
     */
    public static void handleClientDisconnect(IOException e) throws IOException {
        if (isClientAbortError(e)) {
            logClientAbort(e);
        } else {
            // unexpected problem, let traditional error management handle it
            throw e; // NOSONAR
        }
    }

}

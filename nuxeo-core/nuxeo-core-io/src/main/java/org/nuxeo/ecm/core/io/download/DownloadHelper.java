/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
                rangeStart = length - Integer.parseInt(end);
                if (rangeStart < 0) {
                    rangeStart = 0;
                }
            } else {
                rangeStart = Integer.parseInt(start);
                if (!end.isEmpty()) {
                    rangeEnd = Integer.parseInt(end);
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

        String inline = request.getParameter("inline");
        if (inline == null) {
            inline = (String) request.getAttribute("inline");
        }
        boolean inlineFlag = (inline == null || "false".equals(inline)) ? false : true;

        String userAgent = request.getHeader("User-Agent");
        return RFC2231.encodeContentDisposition(filename, inlineFlag, userAgent);

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

    public static void logClientAbort(Exception e) {
        log.debug("Client disconnected: " + unwrapException(e).getMessage());
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
            throw e;
        }
    }

}

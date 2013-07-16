/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.web.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Helpers for servlets.
 *
 * @since 5.6
 */
public class ServletHelper {

    private static final Log log = LogFactory.getLog(ServletHelper.class);

    public static final String TX_TIMEOUT_HEADER_KEY = "Nuxeo-Transaction-Timeout";

    private ServletHelper() {
        // utility class
    }

    public static boolean startTransaction(HttpServletRequest request) {
        String header = request.getHeader(TX_TIMEOUT_HEADER_KEY);
        int timeout = 0; // default
        if (header != null) {
            try {
                timeout = Integer.parseInt(header);
            } catch (NumberFormatException e) {
                // bad header
                log.warn("Invalid request transaction timeout: " + header);
            }
        }
        return TransactionHelper.startTransaction(timeout);
    }

    /**
     * Generate a Content-Disposition string based on the servlet request for
     * a given filename. The value follows RFC2231
     *
     * @param request
     * @param filename
     * @return a full string to set as value of a {@code Content-Disposition}
     *         header
     *
     * @since 5.7.2
     */
    public static String getRFC2231ContentDisposition(HttpServletRequest request, String filename) {


        String inline = request.getParameter("inline");
        if (inline == null) {
            inline = (String) request.getAttribute("inline");
        }
        boolean inlineFlag = (inline == null || "false".equals(inline)) ? false
                : true;

        String userAgent = request.getHeader("User-Agent");
        return RFC2231.encodeContentDisposition(
                filename, inlineFlag, userAgent);

    }

}

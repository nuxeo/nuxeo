/*
 *  (C) Copyright 2000-2003 Yale University. All rights reserved.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, ARE EXPRESSLY
 *  DISCLAIMED. IN NO EVENT SHALL YALE UNIVERSITY OR ITS EMPLOYEES BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED, THE COSTS OF
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED IN ADVANCE OF THE POSSIBILITY OF SUCH
 *  DAMAGE.
 *
 *  Redistribution and use of this software in source or binary forms,
 *  with or without modification, are permitted, provided that the
 *  following conditions are met:
 *
 *  1. Any redistribution must include the above copyright notice and
 *  disclaimer and this list of conditions in any related documentation
 *  and, if feasible, in the redistributed software.
 *
 *  2. Any redistribution must include the acknowledgment, "This product
 *  includes software developed by Yale University," in any related
 *  documentation and, if feasible, in the redistributed software.
 *
 *  3. The names "Yale" and "Yale University" must not be used to endorse
 *  or promote products derived from this software.
 */

package edu.yale.its.tp.cas.client;

import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * Provides utility functions in support of CAS clients.
 */
public class Util {

    /**
     * Returns a service ID (URL) as a composite of the preconfigured server name and the runtime request.
     */
    public static String getService(HttpServletRequest request, String server) throws ServletException {
        // ensure we have a server name
        if (server == null)
            throw new IllegalArgumentException("name of server is required");

        // now, construct our best guess at the string
        StringBuilder sb = new StringBuilder();
        if (request.isSecure())
            sb.append("https://");
        else
            sb.append("http://");
        sb.append(server);
        sb.append(request.getRequestURI());

        if (request.getQueryString() != null) {
            // first, see whether we've got a 'ticket' at all
            int ticketLoc = request.getQueryString().indexOf("ticket=");

            // if ticketLoc == 0, then it's the only parameter and we ignore
            // the whole query string

            // if no ticket is present, we use the query string wholesale
            if (ticketLoc == -1)
                sb.append("?").append(request.getQueryString());
            else if (ticketLoc > 0) {
                ticketLoc = request.getQueryString().indexOf("&ticket=");
                if (ticketLoc == -1) {
                    // there was a 'ticket=' unrelated to a parameter named 'ticket'
                    sb.append("?").append(request.getQueryString());
                } else if (ticketLoc > 0) {
                    // otherwise, we use the query string up to "&ticket="
                    sb.append("?").append(request.getQueryString().substring(0, ticketLoc));
                }
            }
        }
        return URLEncoder.encode(sb.toString());
    }
}

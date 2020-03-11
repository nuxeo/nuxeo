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

package edu.yale.its.tp.cas.proxy;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import edu.yale.its.tp.cas.util.SecureURL;

/**
 * Receives and keeps track fo PGTs and serial PGT identifiers (IOUs) sent by CAS in response to a ServiceValidate
 * request.
 */
public class ProxyTicketReceptor extends HttpServlet {

    // *********************************************************************
    // Constants

    private static final long serialVersionUID = 1L;

    private static final String PGT_IOU_PARAM = "pgtIou";

    private static final String PGT_ID_PARAM = "pgtId";

    // *********************************************************************
    // Private state

    private static Map<String, String> pgt;

    private static String casProxyUrl;

    // *********************************************************************
    // Initialization

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        synchronized (ProxyTicketReceptor.class) {
            if (pgt == null)
                pgt = new HashMap<>();

            // retrieve the URL for CAS
            if (casProxyUrl == null) {
                ServletContext app = config.getServletContext();
                casProxyUrl = app.getInitParameter("edu.yale.its.tp.cas.proxyUrl");
                if (casProxyUrl == null)
                    throw new ServletException("need edu.yale.its.tp.cas.proxyUrl");
            }
        }
    }

    // *********************************************************************
    // Request handling

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pgtId = request.getParameter(PGT_ID_PARAM);
        String pgtIou = request.getParameter(PGT_IOU_PARAM);
        if (pgtId != null && pgtIou != null) {
            synchronized (pgt) {
                pgt.put(pgtIou, pgtId);
            }
        }
        @SuppressWarnings("resource")
        PrintWriter out = response.getWriter();
        out.println("<casClient:proxySuccess " + "xmlns:casClient=\"http://www.yale.edu/tp/casClient\"/>");
        out.flush();
    }

    // *********************************************************************
    // Interface to package members

    // NOTE: PUBLIC FOR THE MOMENT

    /**
     * Retrieves a proxy ticket using the PGT that corresponds to the given PGT IOU.
     */
    public static String getProxyTicket(String pgtIou, String target) throws IOException {
        synchronized (ProxyTicketReceptor.class) {
            // ensure state is sensible
            if (casProxyUrl == null || pgt == null)
                throw new IllegalStateException("getProxyTicket() only works after servlet has been initialized");
        }

        // retrieve PGT
        String pgtId = null;
        synchronized (pgt) {
            pgtId = pgt.get(pgtIou);
        }
        if (pgtId == null)
            return null;

        // retrieve an XML response from CAS's "Proxy" actuator
        String url = casProxyUrl + "?pgt=" + pgtId + "&targetService=" + target;
        String response = SecureURL.retrieve(url);

        // parse this response (use a lightweight approach for now)
        if (response.indexOf("<cas:proxySuccess>") != -1 && response.indexOf("<cas:proxyTicket>") != -1) {
            int startIndex = response.indexOf("<cas:proxyTicket>") + "<cas:proxyTicket>".length();
            int endIndex = response.indexOf("</cas:proxyTicket>");
            return response.substring(startIndex, endIndex);
        } else {
            // generic failure
            return null;
        }
    }
}

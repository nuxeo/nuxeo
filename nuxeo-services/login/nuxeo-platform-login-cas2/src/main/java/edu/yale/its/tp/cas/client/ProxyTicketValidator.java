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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Validates PTs and optionally retrieves PGT IOUs. Subclassed instead of collapsed into parent because we don't want
 * users to accidentally accept a proxy ticket when they mean only to accept service tickets. That is, proxy targets
 * need to know that they're proxy targets, not first-level web applications.
 */
public class ProxyTicketValidator extends ServiceTicketValidator {

    // *********************************************************************
    // Additive state

    protected List<String> proxyList;

    // *********************************************************************
    // Accessors

    /**
     * Retrieves a list of proxies involved in the current authentication.
     */
    public List<String> getProxyList() {
        return proxyList;
    }

    // *********************************************************************
    // Response parser

    @Override
    protected DefaultHandler newHandler() {
        return new ProxyHandler();
    }

    protected class ProxyHandler extends ServiceTicketValidator.Handler {

        // **********************************************
        // Constants

        protected static final String PROXIES = "cas:proxies";

        protected static final String PROXY = "cas:proxy";

        // **********************************************
        // Parsing state

        protected List<String> proxyList = new ArrayList<>();

        protected boolean proxyFragment = false;

        // **********************************************
        // Parsing logic

        @Override
        public void startElement(String ns, String ln, String qn, Attributes a) {
            super.startElement(ns, ln, qn, a);
            if (authenticationSuccess && qn.equals(PROXIES))
                proxyFragment = true;
        }

        @Override
        public void endElement(String ns, String ln, String qn) throws SAXException {
            super.endElement(ns, ln, qn);
            if (qn.equals(PROXIES))
                proxyFragment = false;
            else if (proxyFragment && qn.equals(PROXY))
                proxyList.add(currentText.toString().trim());
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            if (authenticationSuccess)
                ProxyTicketValidator.this.proxyList = proxyList;
        }
    }

    // *********************************************************************
    // Utility methods

    /**
     * Clears internally manufactured state.
     */
    @Override
    protected void clear() {
        super.clear();
        proxyList = null;
    }

}

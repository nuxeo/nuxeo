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

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.nuxeo.common.utils.URIUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import edu.yale.its.tp.cas.util.SecureURL;

/**
 * Validates STs and optionally retrieves PGT IOUs. Designed with a bean-like interface for simplicity and generality.
 */
public class ServiceTicketValidator {

    // *********************************************************************
    // Private state

    protected String casValidateUrl, proxyCallbackUrl, st, service, pgtIou, user, errorCode, errorMessage,
            entireResponse;

    protected boolean renew = false;

    protected boolean attemptedAuthentication;

    protected boolean successfulAuthentication;

    // *********************************************************************
    // Accessors

    /**
     * Sets the CAS validation URL to use when validating tickets and retrieving PGT IOUs.
     */
    public void setCasValidateUrl(String x) {
        this.casValidateUrl = x;
    }

    /**
     * Gets the CAS validation URL to use when validating tickets and retrieving PGT IOUs.
     */
    public String getCasValidateUrl() {
        return this.casValidateUrl;
    }

    /**
     * Sets the callback URL, owned logically by the calling service, to receive the PGTid/PGTiou mapping.
     */
    public void setProxyCallbackUrl(String x) {
        this.proxyCallbackUrl = x;
    }

    /**
     * Sets the "renew" flag on authentication. When set to "true", authentication will only succeed if this was an
     * initial login (forced by the "renew" flag being set on login).
     */
    public void setRenew(boolean b) {
        this.renew = b;
    }

    /**
     * Gets the callback URL, owned logically by the calling service, to receive the PGTid/PGTiou mapping.
     */
    public String getProxyCallbackUrl() {
        return this.proxyCallbackUrl;
    }

    /**
     * Sets the ST to validate.
     */
    public void setServiceTicket(String x) {
        this.st = x;
    }

    /**
     * Sets the service to use when validating.
     */
    public void setService(String x) {
        this.service = x;
    }

    /**
     * Returns the strongly authenticated username.
     */
    public String getUser() {
        return this.user;
    }

    /**
     * Returns the PGT IOU returned by CAS.
     */
    public String getPgtIou() {
        return this.pgtIou;
    }

    /**
     * Returns {@code true} if the most recent authentication attempted succeeded, {@code false} otherwise.
     */
    public boolean isAuthenticationSuccesful() {
        return this.successfulAuthentication;
    }

    /**
     * Returns an error message if CAS authentication failed.
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * Returns CAS's error code if authentication failed.
     */
    public String getErrorCode() {
        return this.errorCode;
    }

    /**
     * Retrieves CAS's entire response, if authentication was succsesful.
     */
    public String getResponse() {
        return this.entireResponse;
    }

    // *********************************************************************
    // Actuator

    public void validate() throws IOException, SAXException, ParserConfigurationException {
        if (casValidateUrl == null || st == null)
            throw new IllegalStateException("must set validation URL and ticket");
        clear();
        attemptedAuthentication = true;

        Map<String, String> urlParameters = new HashMap<>();
        urlParameters.put("service", service);
        urlParameters.put("ticket", st);
        if (proxyCallbackUrl != null) {
            urlParameters.put("pgtUrl", proxyCallbackUrl);
        }
        if (renew) {
            urlParameters.put("renew", "true");
        }

        String url = URIUtils.addParametersToURIQuery(casValidateUrl, urlParameters);
        String response = SecureURL.retrieve(url, false);
        this.entireResponse = response;

        // parse the response and set appropriate properties
        if (response != null) {
            XMLReader r = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            r.setFeature("http://xml.org/sax/features/namespaces", false);
            r.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            r.setContentHandler(newHandler());
            r.parse(new InputSource(new StringReader(response)));
        }
    }

    // *********************************************************************
    // Response parser

    protected DefaultHandler newHandler() {
        return new Handler();
    }

    protected class Handler extends DefaultHandler {

        // **********************************************
        // Constants

        protected static final String AUTHENTICATION_SUCCESS = "cas:authenticationSuccess";

        protected static final String AUTHENTICATION_FAILURE = "cas:authenticationFailure";

        protected static final String PROXY_GRANTING_TICKET = "cas:proxyGrantingTicket";

        protected static final String USER = "cas:user";

        // **********************************************
        // Parsing state

        protected StringBuilder currentText = new StringBuilder();

        protected boolean authenticationSuccess = false;

        protected boolean authenticationFailure = false;

        protected String netid, pgtIou, errorCode, errorMessage;

        // **********************************************
        // Parsing logic

        @Override
        public void startElement(String ns, String ln, String qn, Attributes a) {
            // clear the buffer
            currentText = new StringBuilder();

            // check outer elements
            if (qn.equals(AUTHENTICATION_SUCCESS)) {
                authenticationSuccess = true;
            } else if (qn.equals(AUTHENTICATION_FAILURE)) {
                authenticationFailure = true;
                errorCode = a.getValue("code");
                if (errorCode != null)
                    errorCode = errorCode.trim();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            // store the body, in stages if necessary
            currentText.append(ch, start, length);
        }

        @Override
        public void endElement(String ns, String ln, String qn) throws SAXException {
            if (authenticationSuccess) {
                if (qn.equals(USER))
                    user = currentText.toString().trim();
                if (qn.equals(PROXY_GRANTING_TICKET))
                    pgtIou = currentText.toString().trim();
            } else if (authenticationFailure) {
                if (qn.equals(AUTHENTICATION_FAILURE))
                    errorMessage = currentText.toString().trim();
            }
        }

        @Override
        public void endDocument() throws SAXException {
            // save values as appropriate
            if (authenticationSuccess) {
                ServiceTicketValidator.this.user = user;
                ServiceTicketValidator.this.pgtIou = pgtIou;
                ServiceTicketValidator.this.successfulAuthentication = true;
            } else if (authenticationFailure) {
                ServiceTicketValidator.this.errorMessage = errorMessage;
                ServiceTicketValidator.this.errorCode = errorCode;
                ServiceTicketValidator.this.successfulAuthentication = false;
            } else
                throw new SAXException("no indication of success of failure from CAS");
        }
    }

    // *********************************************************************
    // Utility methods

    /**
     * Clears internally manufactured state.
     */
    protected void clear() {
        user = pgtIou = errorMessage = null;
        attemptedAuthentication = false;
        successfulAuthentication = false;
    }

}

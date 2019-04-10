/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin (aka matic)
 */
package org.nuxeo.ecm.core.opencmis.impl.client.sso;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Test;

/**
 * Manages the web flow for authenticating a client with CAS.
 *
 * @author matic
 *
 */
public class CasGreeter {

    protected final HttpClient client;

    protected final String location;

    protected String ticket;

    public CasGreeter(HttpClient client, String location) {
        this.client = client;
        this.location = location;
    }

    protected final Pattern loginTicketInputPattern = Pattern.compile(".*(<input.+name=\"lt\"[^<]*>).*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    protected final Pattern loginTicketValuePattern = Pattern.compile(".*value=\"(.*)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Test
    public void testExtractLoginTicketValue() {
        String content = "\n<input name=\"lt\" value=\"test\"/> <pfff/>";
        String value = extractLoginTicket(content);
        assertThat("extracted 'test' value", value, is("test"));
    }

    protected String extractLoginTicket(String content) {
        Matcher inputMatcher = loginTicketInputPattern.matcher(content);
        if (inputMatcher.matches() == false) {
            throw new Error("Cannot extract ticket input");
        }
        String inputContent = inputMatcher.group(1);
        Matcher valueMatcher = loginTicketValuePattern.matcher(inputContent);
        if (valueMatcher.matches() == false) {
            throw new Error("Cannot find ticket value");
        }
        String value = valueMatcher.group(1);
        return value;
    }

    protected final Pattern loginLocationFormPattern = Pattern.compile(".*(<form.*id=\"fm1\"[^<]*>).*", Pattern.DOTALL);

    protected final Pattern loginLocationActionPattern = Pattern.compile(".*action=(\".*\").*", Pattern.DOTALL);

    protected String extractLoginLocation(String content) {
        Matcher formMatcher = loginLocationFormPattern.matcher(content);
        if (formMatcher.matches() == false) {
            throw new Error("Cannot find login form");
        }
        Matcher actionMatcher = loginLocationActionPattern.matcher(formMatcher.group(1));
        if (actionMatcher.matches() == false) {
            throw new Error("Cannot find login action");
        }
        return actionMatcher.group(1);
    }

    protected final Pattern redirectLinkPattern = Pattern.compile(".*(<a.*href=\".*?ticket=.*\"[^<]*>).*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    protected final Pattern redirectValuePattern = Pattern.compile(".*href=\"(.*)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Test
    public void testExtractRedirectLink() {
        String content = "\n<input name=\"lt\" value=\"test\"/> <pfff/>";
        String value = extractLoginTicket(content);
        assertThat("extracted 'test' value", value, is("test"));
    }

    protected String extractRedirectLink(String content) {
        Matcher inputMatcher = redirectLinkPattern.matcher(content);
        if (inputMatcher.matches() == false) {
            throw new Error("Cannot extract redirect link");
        }
        String inputContent = inputMatcher.group(1);
        Matcher valueMatcher = redirectValuePattern.matcher(inputContent);
        if (valueMatcher.matches() == false) {
            throw new Error("Cannot extract redirect value");
        }
        String value = valueMatcher.group(1);
        return value;
    }

    public String fetchServiceTicket(HttpMethod page) throws HttpException, IOException {
        client.executeMethod(page);
        try {
            if (page.getStatusCode() != HttpStatus.SC_OK) {
                throw new Error("Cannot get login form");
            }
            return extractLoginTicket(page.getResponseBodyAsString());
        } finally {
            page.releaseConnection();
        }
    }

    public String fetchServiceLocation(HttpMethod page) throws HttpException, IOException {
        client.executeMethod(page);
        try {
            if (page.getStatusCode() != HttpStatus.SC_OK) {
                throw new Error("Cannot authenticate");
            }
            return extractRedirectLink(page.getResponseBodyAsString());
        } finally {
            page.releaseConnection();
        }
    }

    abstract class Page {

        HttpMethod method;

        String bodyContent;

        String location() {
            try {
                return method.getURI().getURI();
            } catch (URIException e) {
                throw new Error("Cannot access to method location");
            }
        }

        abstract Page handleNewContent(String... args);

        void handleNewParams(String... args) {

        }

        Page next(String... args) {
            handleNewParams(args);
            try {
                client.executeMethod(method);
            } catch (Exception e) {
                throw new Error("execution error", e);
            }
            try {
                if (method.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
                    method.releaseConnection();
                    String location = method.getResponseHeader("Location").getValue();
                    method = new GetMethod(location);
                    client.executeMethod(method);
                }
                if (method.getStatusCode() != HttpStatus.SC_OK) {
                    throw new Error("server error " + method.getStatusLine());
                }
                bodyContent = method.getResponseBodyAsString();
            } catch (Exception e) {
                throw new Error("no content", e);
            } finally {
                method.releaseConnection();
            }
            return handleNewContent(args);
        }

        String getTicketGranting() {
            for (Cookie cookie : client.getState().getCookies()) {
                if ("CASTGC".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
            return null;
        }

        boolean hasTicketGranting() {
            return getTicketGranting() != null;
        }
    }

    class InitialPage extends Page {

        InitialPage(String location) {
            method = new GetMethod(location);
        }

        public void setProxyTicket(String ticket, String proxy, String service) throws HttpException, IOException {
            method.setQueryString(new NameValuePair[] {
                    new NameValuePair("ticket", ticket),
                    new NameValuePair("proxy", proxy),
                    new NameValuePair("service", service)
            });
        }

        String extractLoginTicket() {
            return CasGreeter.this.extractLoginTicket(bodyContent);
        }

        @Override
        void handleNewParams(String... args) {
           if (args.length == 1) {
            try {
                setProxyTicket(args[0], args[1], args[2]);
            } catch (Exception e) {
                throw new Error("Cannot set ticket granting");
            }
           }
        }


        @Override
        Page handleNewContent(String... args) {
            if (hasTicketGranting()) {
                return new ServicePage(location());
            }
            return new CredentialsPage(location());
        }
    }

    class CredentialsPage extends Page {
        CredentialsPage(String location) {
            method = new PostMethod(location);
        }

        void setParams(String ticket, String username, String password) {
            ((PostMethod)method).setRequestBody(new NameValuePair[] { new NameValuePair("lt", ticket), new NameValuePair("username", username), new NameValuePair("password", password),
                    new NameValuePair("_eventId", "submit"), new NameValuePair("submit", "LOGIN") });
        }

        @Override
        void handleNewParams(String... args) {
            setParams(args[0], args[1], args[2]);
        }

        @Override
        Page handleNewContent(String... args) {
            if (hasTicketGranting()) {
                return new ServicePage(location());
            }
            return new CredentialsPage(location());
        }

    }

    class ServicePage extends Page {

        ServicePage(String location) {
            method = new GetMethod(location);
        }

        @Override
        ServicePage handleNewContent(String... args) {
            return this;
        }


    }

    public String credsLogon(String username, String password) throws HttpException, IOException {
        InitialPage initialPage = new InitialPage(location);
        Page credentialsPage = initialPage.next();
        Page servicePage = credentialsPage.next(initialPage.extractLoginTicket(), username, password);
        return servicePage.getTicketGranting();
    }

    public String proxyLogon(String ticket, String proxy, String service) throws IllegalArgumentException, HttpException, IOException {
        InitialPage initialPage = new InitialPage(location);
        initialPage.setProxyTicket(ticket, proxy, service);
        Page servicePage= initialPage.next();
        return servicePage.getTicketGranting();
    }
}

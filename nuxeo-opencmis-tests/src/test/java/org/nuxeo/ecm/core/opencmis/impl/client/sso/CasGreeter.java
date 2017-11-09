/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin (aka matic)
 */
package org.nuxeo.ecm.core.opencmis.impl.client.sso;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

/**
 * Manages the web flow for authenticating a client with CAS.
 *
 * @author matic
 */
public class CasGreeter {

    protected final CloseableHttpClient client;

    protected final CookieStore cookieStore;

    protected final String location;

    protected String ticket;

    public CasGreeter(CloseableHttpClient client, CookieStore cookieStore, String location) {
        this.client = client;
        this.cookieStore = cookieStore;
        this.location = location;
    }

    protected final Pattern loginTicketInputPattern = Pattern.compile(".*(<input.+name=\"lt\"[^<]*>).*", Pattern.DOTALL
            | Pattern.CASE_INSENSITIVE);

    protected final Pattern loginTicketValuePattern = Pattern.compile(".*value=\"(.*)\".*", Pattern.DOTALL
            | Pattern.CASE_INSENSITIVE);

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

    protected final Pattern redirectLinkPattern = Pattern.compile(".*(<a.*href=\".*?ticket=.*\"[^<]*>).*",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    protected final Pattern redirectValuePattern = Pattern.compile(".*href=\"(.*)\".*", Pattern.DOTALL
            | Pattern.CASE_INSENSITIVE);

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

    public String fetchServiceTicket(HttpUriRequest request) throws HttpException, IOException {
        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new Error("Cannot get login form");
            }
            String body = EntityUtils.toString(response.getEntity());
            return extractLoginTicket(body);
        }
    }
    public String fetchServiceLocation(HttpUriRequest request) throws HttpException, IOException {
        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new Error("Cannot authenticate");
            }
            String body = EntityUtils.toString(response.getEntity());
            return extractRedirectLink(body);
        }
    }

    abstract class Page {

        URI location;

        boolean post;

        HttpEntity entity;

        String bodyContent;

        Page(URI location) {
            this.location = location;
        }

        abstract Page handleNewContent(String... args);

        void handleNewParams(String... args) {

        }

        Page next(String... args) {
            handleNewParams(args);
            HttpUriRequest request;
            if (!post) {
                request = new HttpGet(location);
            } else {
                request = new HttpPost(location);
                ((HttpPost) request).setEntity(entity);
            }
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new Error("server error " + response.getStatusLine());
                }
                bodyContent = EntityUtils.toString(response.getEntity());
            } catch (Exception e) {
                throw new Error("no content", e);
            }
            return handleNewContent(args);
        }

        String getTicketGranting() {
            for (Cookie cookie : cookieStore.getCookies()) {
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

        InitialPage(URI location) {
            super(location);
        }

        public void setProxyTicket(String ticket, String proxy, String service) {
            try {
                location = new URIBuilder(location).setParameter("ticket", ticket) //
                                                   .setParameter("proxy", proxy)
                                                   .setParameter("service", service)
                                                   .build();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        String extractLoginTicket() {
            return CasGreeter.this.extractLoginTicket(bodyContent);
        }

        @Override
        void handleNewParams(String... args) {
            if (args.length == 1) {
                setProxyTicket(args[0], args[1], args[2]);
            }
        }

        @Override
        Page handleNewContent(String... args) {
            if (hasTicketGranting()) {
                return new ServicePage(location);
            }
            return new CredentialsPage(location);
        }
    }

    class CredentialsPage extends Page {
        CredentialsPage(URI location) {
            super(location);
            post = true;
        }

        void setParams(String ticket, String username, String password) {
            try {
                entity = new UrlEncodedFormEntity(Arrays.asList( //
                        new BasicNameValuePair("lt", ticket), //
                        new BasicNameValuePair("username", username), //
                        new BasicNameValuePair("password", password), //
                        new BasicNameValuePair("_eventId", "submit"), //
                        new BasicNameValuePair("submit", "LOGIN")));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        void handleNewParams(String... args) {
            setParams(args[0], args[1], args[2]);
        }

        @Override
        Page handleNewContent(String... args) {
            if (hasTicketGranting()) {
                return new ServicePage(location);
            }
            return new CredentialsPage(location);
        }

    }

    class ServicePage extends Page {

        ServicePage(URI location) {
            super(location);
        }

        @Override
        ServicePage handleNewContent(String... args) {
            return this;
        }

    }

    public String credsLogon(String username, String password) throws HttpException, IOException {
        InitialPage initialPage = new InitialPage(URI.create(location));
        Page credentialsPage = initialPage.next();
        Page servicePage = credentialsPage.next(initialPage.extractLoginTicket(), username, password);
        return servicePage.getTicketGranting();
    }

    public String proxyLogon(String ticket, String proxy, String service) throws IllegalArgumentException,
            HttpException, IOException {
        InitialPage initialPage = new InitialPage(URI.create(location));
        initialPage.setProxyTicket(ticket, proxy, service);
        Page servicePage = initialPage.next();
        return servicePage.getTicketGranting();
    }
}

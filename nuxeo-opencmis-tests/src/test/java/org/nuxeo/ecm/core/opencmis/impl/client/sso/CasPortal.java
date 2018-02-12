/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;
import org.nuxeo.ecm.core.opencmis.impl.client.protocol.http.HttpURLInstaller;

/**
 * The following application figure out a typical interaction between a portal, a CAS server and a Nuxeo repository. The
 * use case is fully documented in the documentation center http://doc.nuxeo.com/display/NXDOC/CAS2+Authentication.
 *
 * @author Stephane Lacoin (aka matic)
 */
public class CasPortal {

    protected static final String CMIS_LOCATION = "http://127.0.0.1:8080/nuxeo/atom/cmis";

    protected static final String CAS_LOCATION = "http://127.0.0.1:8080/cas";

    protected static final String HOME_LOCATION = "http://127.0.0.1:9090/home";

    protected static final String TICKET_LOCATION = "http://127.0.0.1:9090/ticket";

    protected static final String TICKET_ACCEPT_LOCATION = TICKET_LOCATION.concat("/accept");

    protected static final String TICKET_LOGON_LOCATION = TICKET_LOCATION.concat("/logon");

    protected String casLoginLocation() {
        return String.format("%s/login?service=%s/validate", CAS_LOCATION, TICKET_LOCATION);
    }

    protected String casServiceValidateLocation(String ticket) {
        return String.format("%s/serviceValidate?ticket=%s&service=%s/validate&pgtUrl=%s/accept", CAS_LOCATION, ticket,
                TICKET_LOCATION, TICKET_LOCATION);
    }

    protected String casProxyLocation(String ticket, String targetServiceLocation) {
        return String.format("%s/proxy?pgt=%s&targetService=%s", CAS_LOCATION, ticket, targetServiceLocation);
    }

    protected HttpURLConnection connect(String location) {
        try {
            return (HttpURLConnection) new URL(location).openConnection();
        } catch (Exception e) {
            throw new Error("Bad location", e);
        }
    }

    protected Pattern proxyGrantingTicketPattern = Pattern.compile(
            ".*<cas:proxyGrantingTicket>(.*)</cas:proxyGrantingTicket>.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected String extractText(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.matches()) {
            throw new Error(String.format("Cannot extract '%s' from '%s'", pattern.pattern(), content));
        }
        return matcher.group(1);
    }

    protected String extractProxyGrantingTicket(String content) {
        return extractText(proxyGrantingTicketPattern, content);
    }

    @Test
    public void testExtractProxyGrantingTicket() {
        String ticket = extractProxyGrantingTicket("<cas:proxyGrantingTicket>test</cas:proxyGrantingTicket>");
        assertThat(ticket, is("test"));
    }

    protected String validateServiceTicket(String serviceTicket) throws IOException {
        HttpURLConnection connection = connect(casServiceValidateLocation(serviceTicket));
        if (connection.getResponseCode() != HttpServletResponse.SC_OK) {
            throw new Error("Cannot validate ticket");
        }
        try (InputStream in = connection.getInputStream()) {
            String content = IOUtils.toString(in, StandardCharsets.UTF_8);
            String iou = extractProxyGrantingTicket(content);
            return proxyGrantingTickets.remove(iou);
        }
    }

    protected Pattern proxyTicketPattern = Pattern.compile(".*<cas:proxyTicket>(.*)</cas:proxyTicket>.*",
            Pattern.DOTALL);

    protected String extractProxyTicket(String content) {
        return extractText(proxyTicketPattern, content);
    }

    @Test
    public void testExtractProxyTicket() {
        String ticket = extractProxyTicket("...\n<cas:proxyTicket>test</cas:proxyTicket>\n...");
        assertThat(ticket, is("test"));
    }

    protected String requestProxyTicket(String proxyGrantingTicket, String targetServiceLocation) throws IOException {
        HttpURLConnection proxyConnection = connect(casProxyLocation(proxyGrantingTicket, targetServiceLocation));
        if (proxyConnection.getResponseCode() != HttpServletResponse.SC_OK) {
            throw new Error("Cannot get service ticket for proxy");
        }
        try (InputStream in = proxyConnection.getInputStream()) {
            String proxyContent = IOUtils.toString(in, StandardCharsets.UTF_8);
            return extractProxyTicket(proxyContent);
        }
    }

    public class ValidateServiceTicketServlet extends HttpServlet {
        public static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String serviceTicket = req.getParameter("ticket");
            String proxyGrantingTicket = validateServiceTicket(serviceTicket);
            String serviceTargetTicket = requestProxyTicket(proxyGrantingTicket, CMIS_LOCATION);
            CasClient cmis = new CasClient(CMIS_LOCATION);
            try {
                cmis.newGreeter().proxyLogon(serviceTargetTicket, TICKET_ACCEPT_LOCATION, CMIS_LOCATION);
                cmis.saveClientContext();
                req.getSession().setAttribute("CMIS", cmis);
            } catch (Exception e) {
                throw new ServletException("cannot connect to cmis server", e);
            }

            // redirect to portal
            resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            resp.setHeader("Location", HOME_LOCATION);
        }
    }

    protected Map<String, String> proxyGrantingTickets = new HashMap<>();

    public class AcceptProxyGrantingTicketServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String id = req.getParameter("pgtId");
            String iou = req.getParameter("pgtIou");
            proxyGrantingTickets.put(iou, id);
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    public class HomeServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            HttpSession http = req.getSession(false);
            if (http == null) {
                // initiate CAS logon
                resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                resp.setHeader("Location", casLoginLocation());
                return;
            }

            // restore thread context
            CasClient cmis = (CasClient) http.getAttribute("CMIS");
            cmis.restoreClientContext();

            // fetch remote documents
            Session repository;
            try {
                repository = cmis.connect();
            } catch (Exception e) {
                throw new ServletException("cannot connect to repo", e);
            }
            Folder folder = repository.getRootFolder();

            PrintWriter writer = resp.getWriter();
            for (Property<?> prop : folder.getProperties()) {
                writer.append(String.format("%s=%s\n", prop.getDisplayName(), prop.getValue()));
            }

            resp.setStatus(HttpStatus.SC_OK);
        }

    }

    protected final Server server = new Server();

    protected void start() {
        HttpURLInstaller.install();
        Server server = new Server();
        Connector connector = new SocketConnector();
        connector.setHost("127.0.0.1");
        connector.setPort(9090);
        connector.setMaxIdleTime(60 * 1000); // 60 seconds
        server.addConnector(connector);

        Context context = new Context(server, "/", Context.SESSIONS);
        context.setBaseResource(Resource.newClassPathResource("/jetty-test"));

        ServletHolder holder;

        holder = new ServletHolder(new HomeServlet());
        context.addServlet(holder, "/home");

        holder = new ServletHolder(new ValidateServiceTicketServlet());
        context.addServlet(holder, "/ticket/validate");

        holder = new ServletHolder(new AcceptProxyGrantingTicketServlet());
        context.addServlet(holder, "/ticket/accept");

        try {
            server.start();
        } catch (Exception e) {
            throw new Error("Cannot start jetty server", e);
        }

    }

    public static void main(String args[]) {
        CasPortal app = new CasPortal();
        app.start();
    }
}

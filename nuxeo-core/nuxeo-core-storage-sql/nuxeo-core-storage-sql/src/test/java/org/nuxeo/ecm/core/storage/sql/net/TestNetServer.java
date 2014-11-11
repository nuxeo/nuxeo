/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.Writer;
import java.net.ConnectException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;

public class TestNetServer {

    private static final int TEST_PORT = 9736;

    private static final String TEST_CTX = "/nuxeotest";

    private static final String TEST_PATH = "/p";

    public static HttpClient httpClient;

    @BeforeClass
    public static void beforeClass() {
        httpClient = new HttpClient();
    }

    @AfterClass
    public static void afterClass() {
        NetServer.shutDown();
        httpClient = null;
    }

    @Test
    public void test() throws Exception {
        assertNull(NetServer.instance);

        ServerDescriptor descr = new ServerDescriptor();
        descr.host = "127.0.0.1";
        descr.port = TEST_PORT;
        descr.path = TEST_CTX;

        NetServer.add(descr, "aa", new DummyServlet("aa"), TEST_PATH + "aa");
        checkServlet(descr, "aa", "1", HttpStatus.SC_OK);

        // add another servlet
        NetServer.add(descr, "bb", new DummyServlet("bb"), TEST_PATH + "bb");
        checkServlet(descr, "bb", "2", HttpStatus.SC_OK);

        // remove first servlet
        NetServer.remove(descr, "aa");
        checkServlet(descr, "aa", "3", HttpStatus.SC_NOT_FOUND);
        checkServlet(descr, "bb", "4", HttpStatus.SC_OK);

        // remove second servlet
        NetServer.remove(descr, "bb");
        checkServlet(descr, "aa", "5", HttpStatus.SC_NOT_FOUND);
        checkServlet(descr, "bb", "6", HttpStatus.SC_NOT_FOUND);

        // shutdown occurred
        assertNull(NetServer.instance);
    }

    protected static void checkServlet(ServerDescriptor descr, String name,
            String val, int expectedStatus) throws IOException, HttpException {
        GetMethod m = new GetMethod(descr.getUrl() + TEST_PATH + name + "?q="
                + val);
        try {
            int status = httpClient.executeMethod(m);
            assertEquals(expectedStatus, status);
            if (status != HttpStatus.SC_OK) {
                return;
            }
            String res = m.getResponseBodyAsString();
            assertEquals(name + " " + val, res);
        } catch (ConnectException e) {
            if (expectedStatus != HttpStatus.SC_NOT_FOUND) {
                throw e;
            }
        } finally {
            m.releaseConnection();
        }
    }

    public static class DummyServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        public final String name;

        public DummyServlet(String name) {
            this.name = name;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String q = req.getParameter("q");
            resp.setContentType("text/plain");
            Writer writer = resp.getWriter();
            writer.write(name + " " + q);
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

}

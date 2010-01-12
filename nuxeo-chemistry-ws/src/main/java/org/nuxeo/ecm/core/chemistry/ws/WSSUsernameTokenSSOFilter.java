/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.chemistry.ws;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class WSSUsernameTokenSSOFilter implements Filter {

    private static final Log log = LogFactory.getLog(WSSUsernameTokenSSOFilter.class);

    public static final String MOD_SSO_REMOTE_USER = "Remote_User";

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String CHARSET_EQ = "charset=";

    private static final String SOAPENV_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    private static final String WSSE_USERNAME = "Username";

    private static final String WSSE_SECURITY = "Security";

    private static final String SOAPENV_MUSTUNDERSTAND = "mustUnderstand";

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        // read body in memory (assumed to be small)
        byte[] body = FileUtils.readBytes(servletRequest.getInputStream());
        String username = null;

        // parse XML
        String encoding = getEncodingFromContentType(servletRequest.getHeader(CONTENT_TYPE));
        InputSource input = new InputSource(new ByteArrayInputStream(body));
        input.setEncoding(encoding);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(input);
            // find <wsse:UserName>
            NodeList nodes = doc.getElementsByTagNameNS(WSSE_NS, WSSE_USERNAME);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String text = el.getTextContent();
                if (text != null && text.length() != 0) {
                    username = text;
                    break;
                }
            }
            // strip soapenv:mustUnderstand="1" from <wsse:Security>
            boolean reserialize = false;
            nodes = doc.getElementsByTagNameNS(WSSE_NS, WSSE_SECURITY);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String attr = el.getAttributeNS(SOAPENV_NS,
                        SOAPENV_MUSTUNDERSTAND);
                if ("1".equals(attr)) {
                    el.removeAttributeNS(SOAPENV_NS, SOAPENV_MUSTUNDERSTAND);
                    reserialize = true;
                    break;
                }
            }
            if (reserialize) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    Transformer trans = TransformerFactory.newInstance().newTransformer();
                    trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
                            "yes");
                    trans.setOutputProperty(OutputKeys.INDENT, "no");
                    Result outputTarget = new StreamResult(out);
                    trans.transform(new DOMSource(doc), outputTarget);
                } catch (TransformerException e) {
                    throw (IOException) new IOException().initCause(e);
                }
                body = out.toByteArray();
            }

        } catch (Exception e) {
            log.error("Cannot parse XML: " + e, e);
        }

        // chain to next filter
        chain.doFilter(new RequestWrapper(servletRequest, username, body),
                response);
    }

    public static String getEncodingFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        int pos = contentType.indexOf(CHARSET_EQ);
        if (pos < 0) {
            return null;
        }
        String encoding = contentType.substring(pos + CHARSET_EQ.length());
        int semi = encoding.indexOf(';');
        if (semi >= 0) {
            encoding = encoding.substring(0, semi);
        }
        encoding = encoding.trim();
        int len = encoding.length();
        if (len > 2 && encoding.charAt(0) == '"'
                && encoding.charAt(len - 1) == '"') {
            encoding = encoding.substring(1, len - 1);
        }
        return encoding.trim();
    }

    /**
     * Request wrapper to add a new header providing mod_sso compatibility, and
     * provide a given body.
     */
    public static class RequestWrapper extends HttpServletRequestWrapper {

        private final String username;

        private byte[] body;

        public RequestWrapper(HttpServletRequest request, String username,
                byte[] body) {
            super(request);
            this.username = username;
            this.body = body;
        }

        @Override
        public String getHeader(String name) {
            if (username == null || !MOD_SSO_REMOTE_USER.equalsIgnoreCase(name)) {
                return super.getHeader(name);
            }
            return username;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream stream = new ServletInputStreamWrapper(body);
            body = null;
            return stream;
        }

    }

    /**
     * ServletInputStream backed by an arbitrary InputStream.
     */
    public static class ServletInputStreamWrapper extends ServletInputStream {

        private InputStream in;

        public ServletInputStreamWrapper(byte[] bytes) throws IOException {
            this.in = new ByteArrayInputStream(bytes);
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }

        @Override
        public int read(byte b[]) throws IOException {
            return in.read(b);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            return in.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return in.skip(n);
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public void close() throws IOException {
            // free memory held by the original ByteArrayInputStream
            in = new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public boolean markSupported() {
            return in.markSupported();
        }

        @Override
        public void mark(int readlimit) {
            in.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            in.reset();
        }
    }

}

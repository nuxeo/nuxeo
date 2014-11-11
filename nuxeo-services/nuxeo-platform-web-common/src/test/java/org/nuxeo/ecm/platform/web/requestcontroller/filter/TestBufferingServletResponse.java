/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.web.requestcontroller.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingServletOutputStream;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingHttpServletResponse;

public class TestBufferingServletResponse extends TestCase {

    private static final Log log = LogFactory.getLog(TestBufferingServletResponse.class);

    protected OutputStream bout;

    protected BufferingHttpServletResponse response;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bout = new ByteArrayOutputStream();
        ResponseProxy responseProxy = new ResponseProxy(bout);
        HttpServletResponse httpServletResponse = getFakeResponse(responseProxy);
        response = new BufferingHttpServletResponse(httpServletResponse);
    }

    public void test() throws Exception {
        BufferingServletOutputStream out = response.getOutputStream();
        out.write('A');
        out.write("BC".getBytes());
        out.write("DDDEFF".getBytes(), 2, 3);
        assertEquals("", bout.toString());
        out.flush();
        assertEquals("", bout.toString());
        out.close();
        assertEquals("", bout.toString());
        out.stopBuffering();
        assertEquals("ABCDEF", bout.toString());
    }

    public void testEmpty() throws Exception {
        BufferingServletOutputStream out = response.getOutputStream();
        assertEquals("", bout.toString());
        out.write(new byte[0]);
        out.flush();
        assertEquals("", bout.toString());
        out.close();
        assertEquals("", bout.toString());
        out.stopBuffering();
        assertEquals("", bout.toString());
    }

    public void testWriter() throws Exception {
        PrintWriter w = response.getWriter();
        w.write("abc");
        // no flush, let stopBuffering do it
        response.stopBuffering();
        assertEquals("abc", bout.toString());
        w.write("def");
        w.flush();
        assertEquals("abcdef", bout.toString());
    }

    protected void doBig(String initial) throws Exception {
        BufferingServletOutputStream out = response.getOutputStream();
        if (StringUtils.isEmpty(initial)) {
            initial = "";
        } else {
            out.write(initial.getBytes());
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        for (int i = 0; i < BufferingServletOutputStream.MAX + 10; i++) {
            buf.write('K');
        }
        byte[] bytes = buf.toByteArray();
        out.write(bytes);
        out.write("DEF".getBytes());
        out.flush();
        assertEquals("", bout.toString());
        out.close();
        assertEquals("", bout.toString());
        out.stopBuffering();
        assertEquals(initial + buf.toString() + "DEF", bout.toString());
    }

    public void testBig() throws Exception {
        doBig("ABC");
    }

    public void testBig2() throws Exception {
        // directly switch to file
        doBig(null);
    }

    protected HttpServletResponse getFakeResponse(ResponseProxy responseProxy) {
        ClassLoader cl = getClass().getClassLoader();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                cl, new Class[] { HttpServletResponse.class }, responseProxy);
        return response;
    }

    public static class ResponseProxy implements InvocationHandler {

        public ServletOutputStream sout;

        public ResponseProxy(final OutputStream out) {
            sout = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    out.write(b);
                }
            };
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            String name = method.getName();
            if (name.equals("getOutputStream")) {
                return sout;
            }
            log.error(name);
            return null;
        }
    }

}

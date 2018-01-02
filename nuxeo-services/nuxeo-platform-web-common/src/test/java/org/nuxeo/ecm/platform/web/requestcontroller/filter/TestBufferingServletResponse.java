/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.web.requestcontroller.filter;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.io.download.BufferingServletOutputStream;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingHttpServletResponse;

public class TestBufferingServletResponse {

    private static final Log log = LogFactory.getLog(TestBufferingServletResponse.class);

    protected OutputStream bout;

    protected BufferingHttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        bout = new ByteArrayOutputStream();
        ResponseProxy responseProxy = new ResponseProxy(bout);
        HttpServletResponse httpServletResponse = getFakeResponse(responseProxy);
        response = new BufferingHttpServletResponse(httpServletResponse);
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testBig() throws Exception {
        doBig("ABC");
    }

    @Test
    public void testBig2() throws Exception {
        // directly switch to file
        doBig(null);
    }

    protected HttpServletResponse getFakeResponse(ResponseProxy responseProxy) {
        ClassLoader cl = getClass().getClassLoader();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(cl,
                new Class[] { HttpServletResponse.class }, responseProxy);
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
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (name.equals("getOutputStream")) {
                return sout;
            }
            log.error(name);
            return null;
        }
    }

}

/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.common.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestURLStreamHandlerFactoryInstaller {

    public static class TestHandlerFactory implements URLStreamHandlerFactory {

        static int invokeCount = 0;

        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            invokeCount += 1;
            return new TestHandler();
        }

    }

    public static class TestHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

    }

    URLStreamHandlerFactory f1;
    URLStreamHandlerFactory f2;

    @Before
    public void setUp() throws Exception {
        TestHandlerFactory.invokeCount = 0;
        f1 = new TestHandlerFactory();
        URLStreamHandlerFactoryInstaller.installURLStreamHandlerFactory(f1);

        f2 = new TestHandlerFactory();
        URLStreamHandlerFactoryInstaller.installURLStreamHandlerFactory(f2);
    }

    @After
    public void tearDown() throws Exception {
        URLStreamHandlerFactoryInstaller.uninstallURLStreamHandlerFactory();
    }

    boolean checkInstalled() {
        boolean installed = true;
        try {
            new URL("test:foo");
        } catch (MalformedURLException e) {
            installed = false;
        }
        return installed;
    }

    @Test
    public void testInstaller() throws Exception {
        assertTrue(checkInstalled());

        URLStreamHandlerFactoryInstaller.uninstallURLStreamHandlerFactory();
        assertFalse(checkInstalled());
    }

    @Test
    public void testReset() throws Exception {
        new URL("test:pfff"); // create test protocol handler
        assertEquals(1, TestHandlerFactory.invokeCount);

        new URL("test:pfff"); // use cached handler
        assertEquals(1, TestHandlerFactory.invokeCount);

        URLStreamHandlerFactoryInstaller.uninstallURLStreamHandlerFactory(f1); // reset cache
        new URL("test:pfff"); // create new test protocol handler
        assertEquals(2, TestHandlerFactory.invokeCount);
    }

}

/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.common.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import junit.framework.TestCase;

import org.nuxeo.common.utils.URLStreamHandlerFactoryInstaller.FactoryStack;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestURLStreamHandlerFactoryInstaller extends TestCase {

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


    @Override
    protected void setUp() throws Exception {
        TestHandlerFactory.invokeCount = 0;
        f1 = new TestHandlerFactory();
        URLStreamHandlerFactoryInstaller.installURLStreamHandlerFactory(f1);

        f2 = new TestHandlerFactory();
        URLStreamHandlerFactoryInstaller.installURLStreamHandlerFactory(f2);
    }

    @Override
    protected void tearDown() throws Exception {
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

    public void testInstaller() throws Exception {
        assertTrue(checkInstalled());
        URLStreamHandlerFactoryInstaller.uninstallURLStreamHandlerFactory();
        assertFalse(checkInstalled());
    }


    public void testReset() throws MalformedURLException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        new URL("test:pfff"); // create test protocol handler
        assertEquals(1, TestHandlerFactory.invokeCount);
        new URL("test:pfff"); // use cached handler
        assertEquals(1, TestHandlerFactory.invokeCount);
        URLStreamHandlerFactoryInstaller.uninstallURLStreamHandlerFactory(f1); // reset cache
        new URL("test:pfff"); // create new test protocol handler
        assertEquals(2, TestHandlerFactory.invokeCount);
    }
}

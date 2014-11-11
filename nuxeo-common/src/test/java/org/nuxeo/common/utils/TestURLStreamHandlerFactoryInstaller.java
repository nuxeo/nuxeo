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

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestURLStreamHandlerFactoryInstaller extends TestCase {

    public void testInstaller() throws Exception {
        URLStreamHandlerFactory f1 = new URLStreamHandlerFactory() {
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return null;
            }
        };
        URLStreamHandlerFactoryInstaller.installURLStreamHandlerFactory(f1);

        URLStreamHandlerFactory f2 = new URLStreamHandlerFactory() {
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return null;
            }
        };
        URLStreamHandlerFactoryInstaller.installURLStreamHandlerFactory(f2);

        assertEquals(f2, URLStreamHandlerFactoryInstaller.getStack().pop());
        assertEquals(f1, URLStreamHandlerFactoryInstaller.getStack().pop());
        assertTrue(URLStreamHandlerFactoryInstaller.getStack().isEmpty());

    }

}

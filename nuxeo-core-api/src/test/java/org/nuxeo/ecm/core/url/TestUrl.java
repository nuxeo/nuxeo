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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.url;

import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;

public class TestUrl extends TestCase {

    public void test() throws IOException {
        URL url1 = URLFactory.getURL("http://toto.com/");
        assertEquals("http://toto.com/", url1.toString());

        URL url2 = URLFactory.getURL("nxdoc://toto/titi/");
        assertEquals("nxdoc://toto/titi/", url2.toString());
        url2.openConnection();

        URL url3 = URLFactory.getURL("nxobj://toto/titi/");
        assertEquals("nxobj://toto/titi/", url3.toString());
        url3.openConnection();
    }

}

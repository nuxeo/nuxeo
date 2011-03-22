/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

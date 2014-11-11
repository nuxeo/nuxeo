/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.html;

import java.io.IOException;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.html.JSUtils;
import org.nuxeo.theme.themes.ThemeException;

public class TestJSUtils extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
    }

    public void testCompressSource() throws ThemeException, IOException {
        String expected = Utils.readResourceAsString("test1-expected.js");
        String actual = JSUtils.compressSource(Utils.readResourceAsString("test1.js"));
        assertEquals(expected, actual);
    }

}

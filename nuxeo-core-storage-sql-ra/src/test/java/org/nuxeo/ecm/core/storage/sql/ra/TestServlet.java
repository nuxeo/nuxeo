/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.ra;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;

import org.glassfish.embed.GFApplication;
import org.glassfish.embed.ScatteredWar;

/**
 * @author Florent Guillaume
 */
public class TestServlet extends GlassFishTestCase {

    public void testResourceAdapter() throws Exception {

        ScatteredWar war = new ScatteredWar(
                "testservlet",
                new File("src/test-servlet/resources"),
                new File("src/test-servlet/resources/WEB-INF/web.xml"),
                Collections.singleton(new File("target/test-servlet-classes").toURI().toURL()));

        GFApplication servlet = glassfish.deploy(war);

        URL url = new URL("http://localhost:" + httpPort +
                "/testservlet/testing");
        BufferedReader br = new BufferedReader(new InputStreamReader(
                url.openConnection().getInputStream()));
        assertEquals("this is the test servlet", br.readLine());

        servlet.undeploy();
    }
}

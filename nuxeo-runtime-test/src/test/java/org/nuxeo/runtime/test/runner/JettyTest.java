/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 * $Id$
 */
package org.nuxeo.runtime.test.runner;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;


@RunWith(FeaturesRunner.class)
@Features(JettyFeature.class)
@Jetty(port=9090)
public class JettyTest {

    @Inject org.mortbay.jetty.Server server;

    @Test public void jettyComponentIsDeployed() throws Exception {
        assertNotNull(server);
    }

}

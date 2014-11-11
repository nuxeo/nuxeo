/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica@nuxeo.com
 */
package org.nuxeo.ecm.core.test;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.dumbster.smtp.SimpleSmtpServer;

/**
 * @since 5.7
 */
public class FakeSmtpMailServerFeature extends SimpleFeature {

    public static final int SERVER_PORT = 2525;

    SimpleSmtpServer server;

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        server = SimpleSmtpServer.start(SERVER_PORT);

    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        if (Framework.isInitialized()) {
            Framework.getProperties().put("mail.transport.host", "127.0.0.1");
            Framework.getProperties().put("mail.transport.port", SERVER_PORT);
        }
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        server.stop();
    }

}

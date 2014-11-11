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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.dumbster.smtp.SimpleSmtpServer;

/**
 * @since 5.7
 */
public class FakeSmtpMailServerFeature extends SimpleFeature {

    public static final int SERVER_PORT = 2525;

    public static final String SERVER_HOST = "127.0.0.1";

    public static SimpleSmtpServer server;

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        server = SimpleSmtpServer.start(SERVER_PORT);
        if (Framework.isInitialized()) {

            File file = new File(Environment.getDefault().getConfig(),
                    "mail.properties");
            file.getParentFile().mkdirs();
            List<String> mailProperties = new ArrayList<String>();
            mailProperties.add(String.format("mail.smtp.host = %s", SERVER_HOST));
            mailProperties.add(String.format("mail.smtp.port = %s", SERVER_PORT));
            FileUtils.writeLines(file, mailProperties);

            Framework.getProperties().put("mail.transport.host", SERVER_HOST);
            Framework.getProperties().put("mail.transport.port", SERVER_PORT);
        }

    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}

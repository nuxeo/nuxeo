/*
 * (C) Copyright 2013-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.runtime.test.runner.RunnerFeature;

import com.dumbster.smtp.SimpleSmtpServer;

/**
 * @since 5.7
 */
public class FakeSmtpMailServerFeature implements RunnerFeature {

    public static final int SERVER_PORT = 2525;

    public static final String SERVER_HOST = "127.0.0.1";

    public static SimpleSmtpServer server;

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        server = SimpleSmtpServer.start(SERVER_PORT);
        if (Framework.isInitialized()) {
            File file = new File(Environment.getDefault().getConfig(), "mail.properties");
            List<String> mailProperties = new ArrayList<>();
            mailProperties.add(String.format("mail.smtp.host = %s", SERVER_HOST));
            mailProperties.add(String.format("mail.smtp.port = %s", SERVER_PORT));
            FileUtils.writeLines(file, mailProperties);

            Framework.getProperties().put("mail.transport.host", SERVER_HOST);
            Framework.getProperties().put("mail.transport.port", String.valueOf(SERVER_PORT));
        }
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) {
        if (server != null) {
            server.stop();
        }
    }
}

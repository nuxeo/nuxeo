/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.connect.tools.report;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.nuxeo.ecm.core.management.statuses.NuxeoInstanceIdentifierHelper;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ResourcePublisher;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Reports aggregator, exposed as a service in the runtime.
 *
 * @since 8.3
 */
public class ReportComponent extends DefaultComponent {

    public class Endpoint implements Server {

        @Override
        public void run(String host, int port, String... names) throws IOException {
            try (Socket sock = new Socket(host, port)) {
                ReportComponent.this.run(sock.getOutputStream(), new HashSet<>(Arrays.asList(names)));
            }
        }

    }

    public static ReportComponent instance;

    final ReportConfiguration configuration = new ReportConfiguration();

    void run(OutputStream out, Set<String> names) throws IOException {
        try (JsonGenerator json = Json.createGenerator(out)) {
            json.writeStartObject();
            try {
                json.writeStartObject(NuxeoInstanceIdentifierHelper.getServerInstanceName());
                try {
                    for (ReportContribution contrib : configuration.filter(names)) {
                        json.write(contrib.name, contrib.instance.snapshot());
                    }
                } finally {
                    json.writeEnd();
                }
            } finally {
                json.writeEnd();
            }
        } catch (IOException cause) {
            throw cause;
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP) {
                    return;
                }
                Framework.removeListener(this);
                instance = null;
            }
        });
        instance = this;
        Framework.getService(ResourcePublisher.class).registerResource("connect-report", "connect-report", Server.class, new Endpoint());
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof ReportContribution) {
            configuration.addContribution((ReportContribution) contribution);
        } else {
            throw new IllegalArgumentException(String.format("unknown contribution of type %s in %s", contribution.getClass(), contributor));
        }
    }

}

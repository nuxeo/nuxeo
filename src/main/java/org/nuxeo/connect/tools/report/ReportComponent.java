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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.json.stream.JsonGenerator;

import org.nuxeo.connect.tools.report.json.JsonFactory;
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

    static ReportComponent instance;

    final ReportConfiguration configuration = new ReportConfiguration();

    final JsonFactory jsonFactory = new JsonFactory();

    final ReportInvoker invoker = new ReportInvoker() {

        @Override
        public String snapshot(String dirpath) throws IOException {
            Path filepath = Files.createTempFile(Paths.get(dirpath), "snapshot-", ".json");
            try (JsonGenerator json = jsonFactory.createGenerator(Files.newOutputStream(filepath, StandardOpenOption.CREATE_NEW))) {
                ReportComponent.this.snapshot(json);
            }
            return filepath.toString();
        }

    };

    void snapshot(JsonGenerator json) throws IOException {
        json.writeStartArray();
        try {
            for (ReportContribution contrib : configuration) {
                json.writeStartObject();
                try {
                    json.write("name", contrib.name);
                    contrib.instance.snapshot(json);
                } finally {
                    json.writeEnd();
                }
            }
        } finally {
            json.writeEnd();
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
        Framework.getService(ResourcePublisher.class).registerResource("connect-report", "connect-report", ReportInvoker.class, invoker);
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

/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.tools.report.config;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;

import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.tools.report.ReportWriter;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.info.ConfigurationInfo;
import org.nuxeo.launcher.info.DistributionInfo;
import org.nuxeo.launcher.info.InstanceInfo;
import org.nuxeo.launcher.info.KeyValueInfo;
import org.nuxeo.launcher.info.PackageInfo;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.json.impl.writer.JsonXmlStreamWriter;

/**
 * Write Runtime Configuration in json
 *
 * @since 8.4
 */
public class ConfigReport implements ReportWriter {

    @Override
    public void write(OutputStream output) throws IOException {
        try {
            ConfigurationGenerator configurationGenerator = new ConfigurationGenerator();
            configurationGenerator.init();
            PackageUpdateService packageUpdateService = new StandaloneUpdateService(configurationGenerator.getEnv());
            packageUpdateService.initialize();
            InstanceInfo info = configurationGenerator.getServerConfigurator()
                                                      .getInfo(getCLID(), packageUpdateService.getPackages());
            JAXBContext context = JAXBContext.newInstance(InstanceInfo.class, DistributionInfo.class, PackageInfo.class,
                    ConfigurationInfo.class, KeyValueInfo.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(info, jsonWriter(context, output));
        } catch (PackageException | JAXBException cause) {
            throw new IOException("Cannot write runtime configuration", cause);
        }
    }

    protected String getCLID() {
        try {
            return LogicalInstanceIdentifier.instance().getCLID();
        } catch (NoCLID cause) {
            return "no-clid";
        }
    }

    protected XMLStreamWriter jsonWriter(JAXBContext context, OutputStream out) {
        JSONConfiguration config = JSONConfiguration.mapped()
                                                    .rootUnwrapping(true)
                                                    .attributeAsElement("key", "value")
                                                    .build();
        config = JSONConfiguration.createJSONConfigurationWithFormatted(config, true);
        return JsonXmlStreamWriter.createWriter(new OutputStreamWriter(out), config, "");
    }

}

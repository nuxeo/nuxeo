/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.io.yaml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.core.OperationChainContribution.Operation;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

/**
 * Operation to YAML converter
 *
 * @since 5.9.4
 */
public class YamlWriter {

    public static void toYaml(OutputStream out, OperationDocumentation info) throws IOException {
        toYaml(out, (Object) info);
    }

    public static void toYaml(OutputStream out, Operation info) throws IOException {
        toYaml(out, (Object) info);
    }

    protected static void toYaml(OutputStream out, Object info) throws IOException {
        if (info == null) {
            return;
        }
        DumperOptions options = new DumperOptions();
        options.setAllowReadOnlyProperties(true);
        options.setDefaultFlowStyle(FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new YamlAutomationRepresenter(), options);
        Writer writer = new OutputStreamWriter(out);
        yaml.dump(info, writer);
    }

}

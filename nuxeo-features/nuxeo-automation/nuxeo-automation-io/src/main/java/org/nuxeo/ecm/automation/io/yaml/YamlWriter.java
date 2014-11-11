/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    public static void toYaml(OutputStream out, OperationDocumentation info)
            throws IOException {
        toYaml(out, (Object) info);
    }

    public static void toYaml(OutputStream out, Operation info)
            throws IOException {
        toYaml(out, (Object) info);
    }

    protected static void toYaml(OutputStream out, Object info)
            throws IOException {
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

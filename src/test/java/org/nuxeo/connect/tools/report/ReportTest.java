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
import java.nio.file.Paths;
import java.util.Arrays;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue.ValueType;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(ReportFeature.class)
public class ReportTest {

    @Inject
    ReportFeature report;

    void mxreport(String name, String operation, String... keys) throws IOException {
        JsonObject json = report.snapshot(name);
        Assertions.assertThat(json).containsKeys("request", "value");
        Assertions.assertThat(json.getJsonObject("request").getJsonString("type")).is(new Condition<JsonString>() {

            @Override
            public boolean matches(JsonString value) {
                return operation.equals(value.getString());
            }
        });
        if (json.get("value").getValueType() == ValueType.OBJECT) {
            Assertions.assertThat(json.getJsonObject("value")).containsKeys(keys);
        } else {
            json.getJsonArray("value").containsAll(Arrays.asList(keys));
        }
    }

    @Test
    public void mxinfos() throws IOException {
        mxreport("mx-infos", "list", "JMImplementation", "java.util.logging");
    }

    @Test
    public void mxnames() throws IOException {
        mxreport("mx-names", "search", "JMImplementation", "java.util.logging");
    }

    @Test
    public void mxattributes() throws IOException {
        mxreport("mx-attributes", "read", "JMImplementation:type=MBeanServerDelegate");
    }

    @Test
    public void apidoc() throws IOException {
        JsonObject apidoc = report.snapshot("apidoc");
        Assertions.assertThat(apidoc).containsKeys("org.nuxeo.apidoc.introspection.RuntimeSnapshot");
        return;
    }

    @Test
    public void snapshot() throws IOException {
        Framework.getService(ReportInvoker.class).snapshot(Paths.get("target"));
    }

}
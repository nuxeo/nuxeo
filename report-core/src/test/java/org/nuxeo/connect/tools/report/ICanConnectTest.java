/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.tools.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.connect.tools.report.ICanConnectTest.Given;
import org.nuxeo.connect.tools.report.ICanConnectTest.Then;
import org.nuxeo.connect.tools.report.ICanConnectTest.When;
import org.nuxeo.connect.tools.report.client.Connector;
import org.nuxeo.connect.tools.report.client.Provider;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.annotation.ScenarioState;
import com.tngtech.jgiven.junit.ScenarioTest;

/**
 *
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features(ReportFeature.class)
public class ICanConnectTest extends ScenarioTest<Given, When, Then> {

    public ICanConnectTest() {
        super();
    }

    static class Given extends Stage<Given> {

        @ProvidedScenarioState
        RuntimeService runtime = Framework.getRuntime();

        Given the_runtime_is_started() {
            Assert.assertTrue(runtime.isStarted());
            return self();
        }

        Given the_connect_report_component_is_installed() {
            Assert.assertNotNull(runtime.getComponent("service:org.nuxeo.connect.tools.report"));
            return self();
        }
    }

    static class When extends Stage<When> {
        @ProvidedScenarioState
        Iterable<Provider> providers;

        When i_connect_with_providers() {
            providers = Connector.connector().connect();
            return self();
        }

        When there_is_at_least_one_provider_available() {
            Assert.assertTrue(providers.iterator().hasNext());
            return self();
        }
    }

    static class Then extends Stage<Then> {
        @ExpectedScenarioState
        Iterable<Provider> providers;

        @ScenarioState
        List<String> reports = new LinkedList<>();

        Then i_can_invoke_reports_remotely() throws IOException {
            for (Provider provider : providers) {
                reports.add(provider.snapshot("target"));
            }
            return self();
        }

        Then i_can_read_them() throws IOException {
            for (String file : reports) {
                Files.readAllLines(Paths.get(file));
            }
            return self();
        }
    }

    @Test
    public void i_can_connect() throws IOException {
        given()
            .the_runtime_is_started().and()
            .the_connect_report_component_is_installed();
        when()
            .i_connect_with_providers().and()
            .there_is_at_least_one_provider_available();
        then()
            .i_can_invoke_reports_remotely().and()
            .i_can_read_them();
    }
}

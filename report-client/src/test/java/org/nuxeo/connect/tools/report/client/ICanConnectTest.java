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
package org.nuxeo.connect.tools.report.client;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.json.Json;
import javax.json.JsonObject;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.connect.tools.report.ReportFeature;
import org.nuxeo.connect.tools.report.client.ReportConnector;
import org.nuxeo.connect.tools.report.client.ICanConnectTest.Given;
import org.nuxeo.connect.tools.report.client.ICanConnectTest.Then;
import org.nuxeo.connect.tools.report.client.ICanConnectTest.When;
import org.nuxeo.ecm.core.management.statuses.NuxeoInstanceIdentifierHelper;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
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

    public static class Given extends Stage<Given> {

        @ProvidedScenarioState
        RuntimeService runtime = Framework.getRuntime();

        public Given the_runtime_is_started() {
            Assert.assertTrue(runtime.isStarted());
            return self();
        }

        public Given the_connect_report_component_is_installed() {
            Assert.assertNotNull(runtime.getComponent("service:org.nuxeo.connect.tools.report"));
            return self();
        }
    }

    public static class When extends Stage<When> {
        @ProvidedScenarioState
        ReportConnector connector;

        public When i_connect_with_providers() {
            connector = ReportConnector.of();
            return self();
        }

        public When there_is_at_least_one_server_available() {
            Assert.assertTrue(connector.discover().iterator().hasNext());
            return self();
        }

        @ProvidedScenarioState
        JsonObject json;

        public When i_feed_a_report() throws IOException, InterruptedException, ExecutionException {
            json = connector.feed(Json.createObjectBuilder()).build();
            return self();
        }
    }

    public static class Then extends Stage<Then> {
        @ExpectedScenarioState
        JsonObject json;

        public Then it_contains_this_runtime_report() throws IOException {
            Assertions.assertThat(json).containsKey(NuxeoInstanceIdentifierHelper.getServerInstanceName());
            return self();
        }

        public <T extends Stage<T>> T which(T t) {
            return t;
        }
    }

    @Test
    public void i_can_report() throws IOException, InterruptedException, ExecutionException {
        given()
                .the_runtime_is_started().and()
                .the_connect_report_component_is_installed();
        when()
                .i_connect_with_providers().and()
                .there_is_at_least_one_server_available().and()
                .i_feed_a_report();
        then()
                .it_contains_this_runtime_report();
    }

}

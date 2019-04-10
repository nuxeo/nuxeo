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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.connect.tools.report.ICanRunTest.Given;
import org.nuxeo.connect.tools.report.ICanRunTest.Then;
import org.nuxeo.connect.tools.report.ICanRunTest.When;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunnerWithParms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tngtech.jgiven.CurrentStep;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.annotation.ScenarioState.Resolution;
import com.tngtech.jgiven.attachment.Attachment;
import com.tngtech.jgiven.attachment.MediaType;
import com.tngtech.jgiven.junit.ScenarioTest;

/**
 * Checks that the aggregated report could be ran and reloaded in memory.
 *
 */
@RunWith(FeaturesRunnerWithParms.class)
@Features(ReportFeature.class)
public class ICanRunTest extends ScenarioTest<Given, When, Then> {

    public static class Given extends Stage<Given> {

        @ProvidedScenarioState
        ObjectMapper mapper = DistributionSnapshot.jsonMapper();

        @ProvidedScenarioState
        ReportRunner runner;

        public Given the_runtime_is_started() {
            Assertions.assertThat(Framework.getRuntime()).is(new Condition<RuntimeService>() {

                @Override
                public boolean matches(RuntimeService runtime) {
                    return runtime.isStarted();
                }
            });
            return self();
        }

        public Given the_runner_is_installed() {
            runner = Framework.getService(ReportRunner.class);
            Assertions.assertThat(runner).isNotNull();
            return self();
        }

        @ProvidedScenarioState(resolution = Resolution.NAME)
        Set<String> reports;

        public Given it_generate_reports_of(String... names) {
            reports = runner.list();
            Assertions.assertThat(reports).contains(names);
            return self();
        }

    }

    public static class When extends Stage<When> {
        @ExpectedScenarioState
        ReportRunner runner;

        @ExpectedScenarioState(resolution = Resolution.NAME)
        Set<String> reports;

        @ProvidedScenarioState
        String json;

        public When i_run_a_report() throws IOException {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                runner.run(out, reports);
                json = out.toString();
            }
            return self();
        }

    }

    public static class Then extends Stage<Then> {

        @ExpectedScenarioState
        ObjectMapper mapper;

        @ExpectedScenarioState
        String json;

        @ProvidedScenarioState
        ObjectNode node;

        @ProvidedScenarioState
        CurrentStep current;

        public Then i_can_load_the_json() throws JsonProcessingException, IOException {
            current.addAttachment(Attachment.fromText(json, MediaType.PLAIN_TEXT_UTF_8));
            node = (ObjectNode) mapper.reader().readTree(json);
            return self();
        }

    }

    @Test
    public void i_can_run_a_report() throws IOException {
        given()
                .the_runtime_is_started().and()
                .the_runner_is_installed().and()
                .it_generate_reports_of("mx-names", "mx-infos", "mx-attributes", "mx-thread-dump", "mx-thread-deadlocked",
                        "mx-thread-monitor-deadlocked", "mx-class-histogram", "apidoc");
        when()
                .i_run_a_report();
        then()
                .i_can_load_the_json();
    }
}

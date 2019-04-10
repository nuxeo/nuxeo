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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.output.TeeOutputStream;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.connect.tools.report.ICanReportTest.Given;
import org.nuxeo.connect.tools.report.ICanReportTest.Then;
import org.nuxeo.connect.tools.report.ICanReportTest.When;
import org.nuxeo.connect.tools.report.ReportConfiguration.Contribution;
import org.nuxeo.launcher.info.InstanceInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunnerWithParms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.BeforeStage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.Hidden;
import com.tngtech.jgiven.annotation.IntroWord;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.annotation.ScenarioStage;
import com.tngtech.jgiven.annotation.ScenarioState;
import com.tngtech.jgiven.annotation.ScenarioState.Resolution;
import com.tngtech.jgiven.junit.ScenarioTest;

/**
 * Runs registered reports and checks they can be reloaded.
 *
 * @since 8.3
 */
@RunWith(FeaturesRunnerWithParms.class)
@Features(ReportFeature.class)
public class ICanReportTest extends ScenarioTest<Given, When, Then> {

    @DataProvider
    public static Object[][] i_can_run_mx_reports$provider() {
        return new Object[][] {
                { "mx-infos", "list", "JMImplementation", "java.util.logging" },
                { "mx-names", "search", "JMImplementation:type=MBeanServerDelegate", "java.util.logging:type=Logging" },
                { "mx-attributes", "read", "JMImplementation:type=MBeanServerDelegate" },
                { "mx-thread-dump", "exec" },
                { "mx-thread-deadlocked", "exec" },
                { "mx-thread-monitor-deadlocked", "exec" },
                { "mx-class-histogram", "exec" }
        };
    }

    @Test
    @UseDataProvider(value = "i_can_run_mx_reports$provider")
    public void i_can_run_mx_reports(String aReport, String aType, String... someKeys) throws IOException {
        // @formatter:off
        given()
               .the_report_to_run$name(aReport).and()
               .the_report_component_is_installed().and()
               .the_report_is_registered();
        when()
              .i_run_the_report().and()
              .i_unmarshall();
        then()
              .the_report_is_a_mx_report().which()
                  .have_a_request().which()
                      .is_of_type$type(aType).end()
                  .have_a_value().which()
                      .contains_these_$keys(someKeys).end()
                  .end();
        // formatter:on

    }

    @Test
    public void i_can_run_apidoc_report() throws IOException {
        // @formatter:off
        given()
               .the_report_to_run$name("apidoc").and()
               .the_report_component_is_installed().and()
               .the_report_is_registered();
        when()
            .i_run_the_report().and()
            .i_unmarshall();
        then()
              .the_report_is_a_runtime_snapshot().which()
                  .i_can_unmarshall_it().and()
                  .contains_the_bundle$bundle("org.nuxeo.connect.tools.report.core").end();
        // formatter:on
    }

    @Test
    public void i_can_run_config_report() throws IOException {
        // @formatter:off
        given()
               .the_report_to_run$name("config").and()
               .the_report_component_is_installed().and()
               .the_report_is_registered();
        when()
            .i_run_the_report().and()
            .i_unmarshall();
        then()
              .the_report_is_a_config().end();
        // formatter:on
    }

    public static class Given extends Stage<Given> {

        @ScenarioState(resolution = Resolution.NAME)
        String name;

        public Given the_report_to_run$name(String aName) {
            name = aName;
            return self();
        }

        @ProvidedScenarioState
        ObjectMapper mapper = DistributionSnapshot.jsonMapper();

        @ProvidedScenarioState
        ReportRunner component;

        public Given the_report_component_is_installed() {
            component = Framework.getService(ReportRunner.class);
            Assert.assertNotNull(component);
            return self();
        }

        @ProvidedScenarioState
        ReportWriter writer;

        public Given the_report_is_registered() {
            for (Contribution contrib : ((ReportComponent.Service) component).getConfiguration()) {
                if (contrib.name.equals(name)) {
                    writer = contrib.writer;
                    return self();
                }
            }
            throw new AssertionError(name + " is not contributed");
        }
    }


    public static class When extends Stage<When> {

        @ExpectedScenarioState
        ReportWriter writer;

        @ExpectedScenarioState
        ObjectMapper mapper;

        @ProvidedScenarioState
        ObjectNode report;

        @ProvidedScenarioState
        IOException error;

        @ProvidedScenarioState
        byte[] buffer;

        public When i_run_the_report() throws IOException {
            try (ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
                try (TeeOutputStream output = new TeeOutputStream(sink,
                        Files.newOutputStream(Paths.get("target/".concat(writer.getClass().getSimpleName()).concat(".json")),
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    writer.write(output);
                }
                buffer = sink.toByteArray();
            }
            return self();
        }

        public When i_unmarshall() throws IOException {
            try (InputStream source = new ByteArrayInputStream(buffer)) {
                report = (ObjectNode) mapper.readTree(source);
            }
            return self();
        }

    }

    public static class Then extends Stage<Then> {

        @ScenarioStage
        MXReport mx;


        public MXReport the_report_is_a_mx_report() {
            return mx;
        }

        @ScenarioStage
        RuntimeSnapshotReport apidoc;

        public RuntimeSnapshotReport the_report_is_a_runtime_snapshot() throws IOException {
            return apidoc;
        }

        @ScenarioStage
        ConfigReport config;

        public ConfigReport the_report_is_a_config() throws IOException {
            return config;
        }

        public static class MXReport extends Stage<MXReport> {

            @ScenarioStage
            MXReport.Request request;

            MXReport.Request have_a_request() {
                return request;
            }

            @ScenarioStage
            MXReport.Value value;

            MXReport.Value have_a_value() {
                return value;
            }

            @ScenarioStage
            Then then;

            @Hidden
            public Then end() {
                return then;
            }

            @IntroWord
            public <T extends Stage<?>> T and(T t) {
                return t;
            }

            @IntroWord
            public MXReport which() {
                return self();
            }

            static class Request extends Stage<MXReport.Request> {

                @ExpectedScenarioState
                ObjectNode report;

                JsonNode request;

                @BeforeStage
                void extractRequest() {
                    request = report.get("request");
                    Assert.assertNotNull(request);
                }

                MXReport.Request is_of_type$type(String type) {
                    Assert.assertEquals(type, request.get("type").asText());
                    return self();
                }

                @ScenarioStage
                MXReport outer;

                @Hidden
                MXReport end() {
                    return outer;
                }

                @IntroWord
                MXReport.Request which() {
                    return self();
                }

            }

            static class Value extends Stage<MXReport.Value> {

                @ExpectedScenarioState
                ObjectNode report;

                @ProvidedScenarioState
                JsonNode value;

                @ProvidedScenarioState
                ObjectMapper mapper;

                String keys;

                @BeforeStage
                void extractValue() {
                    value = report.get("value");
                    Assert.assertNotNull(value);
                }

                MXReport.Value contains_the_$key(String key) {
                    if (value.isObject()) {
                        Assertions.assertThat(value.get(key)).isNotEqualTo(MissingNode.getInstance());
                    } else if (value.isArray()) {
                        Assertions.assertThat(value).contains(mapper.getNodeFactory().textNode(key));
                    } else if (value.isTextual()) {
                        Assertions.assertThat(value.asText()).contains(key);
                    } else {
                        throw new AssertionError("???");
                    }
                    return self();
                }

                MXReport.Value contains_these_$keys(String... keys) {
                    for (String key : keys) {
                        self().contains_the_$key(key);
                    }
                    return self();
                }

                @ScenarioStage
                MXReport mxreport;

                @Hidden
                MXReport end() {
                    return mxreport;
                }

                @IntroWord
                MXReport.Value which() {
                    return self();
                }
            }

        }

        public static class RuntimeSnapshotReport extends Stage<RuntimeSnapshotReport> {

            @ExpectedScenarioState
            byte[] bytes;

            @ExpectedScenarioState
            ObjectNode report;

            @ProvidedScenarioState
            DistributionSnapshot snapshot;

            RuntimeSnapshotReport i_can_unmarshall_it() throws IOException {
                try (InputStream source = new ByteArrayInputStream(bytes)) {
                    snapshot = DistributionSnapshot.jsonReader().readValue(source);
                }
                return self();
            }

            RuntimeSnapshotReport contains_the_bundle$bundle(String id) {
                Assertions.assertThat(snapshot.getBundle(id)).isNotNull();
                return self();
            }

            @IntroWord
            RuntimeSnapshotReport which() {
                return self();
            }

            @ScenarioStage
            Then then;

            @Hidden
            Then end() {
                return then;
            }

            RuntimeSnapshotReport is_something() {
                return self();
            }
        }

    }


    public static class ConfigReport extends Stage<ConfigReport> {

        @ExpectedScenarioState
        byte[] bytes;

        @ExpectedScenarioState
        ObjectNode report;

        @ProvidedScenarioState
        InstanceInfo snapshot;

        ConfigReport i_can_unmarshall_it() throws IOException {
            throw new UnsupportedOperationException();
        }

        @IntroWord
        ConfigReport which() {
            return self();
        }

        @ScenarioStage
        Then then;

        @Hidden
        Then end() {
            return then;
        }

    }

}

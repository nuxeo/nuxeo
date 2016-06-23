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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.connect.tools.report.ReportProvider;
import org.nuxeo.connect.tools.report.ReportComponent;
import org.nuxeo.connect.tools.report.ReportConfiguration.Contribution;
import org.nuxeo.connect.tools.report.ICanRunReportTest.Given;
import org.nuxeo.connect.tools.report.ICanRunReportTest.Then;
import org.nuxeo.connect.tools.report.ICanRunReportTest.When;
import org.nuxeo.connect.tools.report.apidoc.APIDocReport;
import org.nuxeo.runtime.test.runner.Features;

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
 *
 *
 */
@RunWith(FeaturesRunnerWithParms.class)
@Features(ReportFeature.class)
public class ICanRunReportTest extends ScenarioTest<Given, When, Then> {

    public static class Given extends Stage<Given> {
        @ScenarioState(resolution = Resolution.NAME)
        String name;

        Given the_report_to_run$name(String aName) {
            name = aName;
            return self();
        }

        @ProvidedScenarioState
        ReportComponent component;

        Given the_report_component_is_installed() {
            component = ReportComponent.instance;
            Assert.assertNotNull(component);
            return self();
        }

        @ProvidedScenarioState
        ReportProvider report;

        Given the_report_is_registered() {
            for (Contribution contrib : ReportComponent.instance.configuration) {
                if (contrib.name.equals(name)) {
                    report = contrib.instance;
                    return self();
                }
            }
            throw new AssertionError(name + " is not contributed");
        }
    }

    public static class When extends Stage<When> {

        @ExpectedScenarioState
        ReportProvider report;

        @ProvidedScenarioState(resolution = Resolution.NAME)
        JsonObject json;

        @ProvidedScenarioState
        IOException error;

        When i_run_the_report() {
            try {
                json = report.snapshot();
            } catch (IOException cause) {
                error = cause;
            }
            return self();
        }

        When there_is_no_errors() {
            Assertions.assertThat(error).isNull();
            return self();
        }

    }

    public static class Then extends Stage<Then> {

        @ExpectedScenarioState(resolution = Resolution.NAME)
        JsonObject json;

        @ScenarioStage
        MXReport mx;

        MXReport the_report_is_a_mx_report() {
            return mx;
        }

        @ScenarioStage
        RuntimeSnapshotReport apidoc;

        RuntimeSnapshotReport the_report_is_a_runtime_snapshot() throws IOException {
            return apidoc;
        }

        Then which() {
            return self();
        }

        static class MXReport extends Stage<MXReport> {

            @ExpectedScenarioState(resolution = Resolution.NAME)
            JsonObject json;

            @ScenarioStage
            Request request;

            Request have_a_request() {
                return request;
            }

            @ScenarioStage
            Value value;

            Value have_a_value() {
                return value;
            }

            @ScenarioStage
            Then then;

            @Hidden
            Then end() {
                return then;
            }

            @IntroWord
            MXReport which() {
                return self();
            }

            static class Request extends Stage<Request> {

                @ExpectedScenarioState(resolution = Resolution.NAME)
                JsonObject json;

                JsonObject request;

                @BeforeStage
                void extractRequest() {
                    request = json.getJsonObject("request");
                    Assert.assertNotNull(request);
                }

                Request is_of_type$type(String type) {
                    Assert.assertEquals(type, request.getString("type"));
                    return self();
                }

                @ScenarioStage
                MXReport report;

                @Hidden
                MXReport end() {
                    return report;
                }

                @IntroWord
                Request which() {
                    return self();
                }

            }

            static class Value extends Stage<Value> {

                @ExpectedScenarioState(resolution = Resolution.NAME)
                JsonObject json;

                JsonValue value;

                String keys;

                @BeforeStage
                void extractValue() {
                    value = json.get("value");
                    Assert.assertNotNull(value);
                }

                Value contains_the_$key(String key) {
                    if (value.getValueType() == ValueType.OBJECT) {
                        Assertions.assertThat(((JsonObject) value)).containsKey(key);
                    } else if (value.getValueType() == ValueType.ARRAY) {
                        Assertions.assertThat((JsonArray) value).contains(jsonOf(key));
                    } else if (value.getValueType() == ValueType.STRING) {
                        Assertions.assertThat(((JsonString) value).getString()).contains(key);
                    } else {
                        throw new AssertionError("???");
                    }
                    return self();
                }

                Value contains_these_$keys(String... keys) {
                    for (String key : keys) {
                        self().contains_the_$key(key);
                    }
                    return self();
                }

                @ScenarioStage
                MXReport report;

                @Hidden
                MXReport end() {
                    return report;
                }

                @IntroWord
                Value which() {
                    return self();
                }
            }

        }

        static class RuntimeSnapshotReport extends Stage<RuntimeSnapshotReport> {

            @ExpectedScenarioState(resolution = Resolution.NAME)
            JsonObject json;

            @ProvidedScenarioState
            RuntimeSnapshot snapshot;

            @BeforeStage
            void is_a_runtime_snapshot() {
                Assertions.assertThat(json).containsKey(RuntimeSnapshot.class.getName());
            }

            RuntimeSnapshotReport i_can_unmarshall() throws IOException {
                snapshot = APIDocReport.snasphotOf(json);
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

    static JsonString jsonOf(String aValue) {
        JsonObject json = Json.createObjectBuilder().add("value", aValue).build();
        return (JsonString) json.get("value");
    }

    @DataProvider
    public static Object[][] i_can_run_mx_reports$provider() {
        return new Object[][] {
                { "mx-infos", "list", "JMImplementation", "java.util.logging" },
                { "mx-names", "search", "JMImplementation:type=MBeanServerDelegate", "java.util.logging:type=Logging" },
                { "mx-attributes", "read", "JMImplementation:type=MBeanServerDelegate" },
                { "mx-thread-dump", "exec" },
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
              .there_is_no_errors();
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
              .there_is_no_errors();
        then()
              .the_report_is_a_runtime_snapshot().which()
                  .i_can_unmarshall().and()
                  .contains_the_bundle$bundle("org.nuxeo.connect.tools.report.core").end();
        // formatter:on
    }

}

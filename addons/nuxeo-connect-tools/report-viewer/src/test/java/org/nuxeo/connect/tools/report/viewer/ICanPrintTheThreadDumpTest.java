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
package org.nuxeo.connect.tools.report.viewer;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.connect.tools.report.ICanReportTest;
import org.nuxeo.connect.tools.report.ReportFeature;
import org.nuxeo.connect.tools.report.ICanReportTest.Then.RuntimeSnapshotReport;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunnerWithParms;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tngtech.jgiven.CurrentStep;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.Hidden;
import com.tngtech.jgiven.annotation.ScenarioStage;
import com.tngtech.jgiven.attachment.Attachment;
import com.tngtech.jgiven.attachment.MediaType;
import com.tngtech.jgiven.junit.ScenarioTest;

import org.nuxeo.connect.tools.report.viewer.ICanPrintTheThreadDumpTest.Given;
import org.nuxeo.connect.tools.report.viewer.ICanPrintTheThreadDumpTest.When;
import org.nuxeo.connect.tools.report.viewer.ICanPrintTheThreadDumpTest.Then;

/**
 * Verify that we could print thread info serialized in JSON
 *
 * @since 8.4
 */
@RunWith(FeaturesRunnerWithParms.class)
@Features(ReportFeature.class)
public class ICanPrintTheThreadDumpTest extends ScenarioTest<Given, When, Then> {

    public static class Given extends ICanReportTest.Given {

    }

    public static class When extends ICanReportTest.When {

    }

    public static class Then extends ICanReportTest.Then {

    }

    @ScenarioStage
    ThenICanPrint thenICanPrint;

    @Test
    public void i_can_print_the_thread_dump() throws IOException {
        // @formatter:off
        given()
                .the_report_to_run$name("mx-thread-dump").and()
                .the_report_component_is_installed().and()
                .the_report_is_registered();
        when()
                .i_run_the_report().and()
                .i_unmarshall();
        then()
                .the_report_is_a_mx_report()
                .and(thenICanPrint)
                    .i_can_print_the_thread_dump()
                .end();
        // @formatter:on
    }

    public static class ThenICanPrint extends Stage<ThenICanPrint> {

        @ExpectedScenarioState
        ObjectNode report;

        @ExpectedScenarioState
        CurrentStep currentStep;

        public ThenICanPrint i_can_print_the_thread_dump() throws IOException {
            currentStep.addAttachment(
                    Attachment.fromText(
                            new ThreadDumpPrinter((ArrayNode) report.at(JsonPointer.compile("/value"))).print(new StringBuilder()).toString(),
                            MediaType.PLAIN_TEXT_UTF_8));
            return self();
        }

        @ScenarioStage
        RuntimeSnapshotReport outerStage;

        @Hidden
        RuntimeSnapshotReport end() {
            return outerStage;
        }
    }
}

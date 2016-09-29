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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.ParseException;
import org.assertj.core.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.connect.tools.report.ICanRunTest;
import org.nuxeo.connect.tools.report.ReportFeature;
import org.nuxeo.connect.tools.report.viewer.ICanRunTheViewerTest.Given;
import org.nuxeo.connect.tools.report.viewer.ICanRunTheViewerTest.Then;
import org.nuxeo.connect.tools.report.viewer.ICanRunTheViewerTest.When;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.tngtech.jgiven.CurrentStep;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.attachment.Attachment;
import com.tngtech.jgiven.attachment.MediaType;
import com.tngtech.jgiven.junit.ScenarioTest;

/**
 * Verify that we could print the report in an human friendly way using the main
 * API.
 *
 */
@RunWith(FeaturesRunner.class)
@Features(ReportFeature.class)
public class ICanRunTheViewerTest extends ScenarioTest<Given, When, Then> {

    static class Given extends ICanRunTest.Given {

    }

    static class When extends ICanRunTest.When {

    }

    static class Then extends Stage<Then> {

        @ExpectedScenarioState
        String json;

        @ExpectedScenarioState
        CurrentStep currentStep;

        public Then i_can_print_the_thread_dump() throws IOException, ParseException {
            try (OutputStream out =
                    Files.newOutputStream(Paths.get("target/report.json"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                out.write(json.getBytes());
            }
            Viewer.main(Arrays.array("-i", "target/report.json", "-o", "target/report.tdump"));
            currentStep.addAttachment(Attachment.fromTextFile(new File("target/report.tdump"), MediaType.PLAIN_TEXT_UTF_8));
            return self();
        }

    }

    @Test
    public void i_can_run_main() throws IOException, InterruptedException, ExecutionException, ParseException {
        given()
                .the_runtime_is_started().and()
                .the_runner_is_installed().and()
                .it_generate_reports_of("mx-thread-dump", "mx-thread-deadlocked", "mx-thread-monitor-deadlocked");
        when()
                .i_run_a_report();
        then()
                .i_can_print_the_thread_dump();
    }

}

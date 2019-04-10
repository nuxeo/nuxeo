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
package org.nuxeo.connect.tools.report.web;

import java.io.IOException;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.connect.tools.report.web.ICanRunReportFromWebTest.Given;
import org.nuxeo.connect.tools.report.web.ICanRunReportFromWebTest.Then;
import org.nuxeo.connect.tools.report.web.ICanRunReportFromWebTest.When;
import org.nuxeo.connect.tools.report.web.jgiven.Stage;
import org.nuxeo.ecm.webengine.test.WebEngineHomePage;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.web.Attachment;

import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.NestedSteps;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.junit.ScenarioTest;

/**
 * Checks that we can invoke a report using the web interface.
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(WebReportFeature.class)
public class ICanRunReportFromWebTest extends ScenarioTest<Given, When, Then> {

    @Inject
    WebEngineHomePage home;

    static class Given extends LoginStage<Given> {

    }

    static class When extends Stage<When> {

        @ExpectedScenarioState
        WebEngineHomePage homepage;

        RootPage rootpage;

        RunnerPage runnerpage;

        @ProvidedScenarioState
        Attachment attachment;

        @NestedSteps
        When I_navigate_the_runner_page() {
            return given().I_am_on_the_homepage().when().I_click_the_module_link().and().I_click_the_runner_link()
                    .then().the_selection_is$options("mx-thread-dump", "mx-infos", "mx-thread-monitor-deadlocked",
                            "mx-class-histogram", "mx-names", "mx-attributes", "config", "mx-thread-deadlocked",
                            "apidoc");
        }

        @NestedSteps
        When I_select$the_options(String... options) {
            runnerpage.select(options);
            return then().the_selection_is$options(options);
        }

        @NestedSteps
        When I_submit_the_selection() {
            runnerpage = runnerpage.submit();
            return then().I_received_the_attachment().and().it_is_correctly_named();
        }

        When I_am_on_the_homepage() {
            if (!homepage.getDriver().getCurrentUrl().equals(homepage.getConfiguration().getHome())) {
                homepage.home();
            }
            WebEngineHomeAssert.assertThat(homepage).isCurrent();
            return this;
        }

        When I_click_the_module_link() {
            rootpage = homepage.getModulePage("connect-tools", RootPage.class);
            return this;
        }

        When I_click_the_runner_link() {
            runnerpage = rootpage.navigateRunner();
            return this;
        }

        When the_selection_is$options(String... options) {
            RunnerAssert.assertThat(runnerpage).selectsOnly(options);
            return this;
        }

        When I_received_the_attachment() {
            attachment = runnerpage.getAttachment();
            return this;
        }

        When it_is_correctly_named() {
            Assertions.assertThat(attachment.getFilename()).isEqualTo("nuxeo-connect-tools-report.json");
            return this;
        }

        When iReceivedTheAttachment() {
            attachment = runnerpage.getAttachment();
            return this;
        }

        When itIsCorrectlyNamed() {
            Assertions.assertThat(attachment.getFilename()).isEqualTo("nuxeo-connect-tools-report.json");
            return this;
        }

    }

    static class Then extends Stage<Then> {

        @ProvidedScenarioState
        Attachment attachment;

        JsonObject report;

        String id;

        Then iCanParseTheAttachment() throws IOException {
            JsonObject json = (JsonObject) Json.createReader(attachment.getContent()).read();
            id = json.keySet().iterator().next();
            report = json.getJsonObject(id);
            return this;
        }

        Then itContainsOnly(String... options) {
            Assertions.assertThat(report.keySet()).containsOnly(options);
            return this;
        }

    }

    @Inject
    FeaturesRunner runner;


    @Before
    public void login() throws Exception {
        try {
            new LoginExecutor().with(runner).login();
        } catch (Throwable cause) {
            throw new AssertionError("cannot login", cause);
        }
    }

    @After
    public void logout() throws Exception {
        try {
            new LoginExecutor().with(runner).logout();
        } catch (Throwable cause) {
            throw new AssertionError("cannot logout", cause);
        }
    }

    @Test
    public void I_can_run_report() throws IOException {
        // @formatter:off
        given().
            the_homepage(home);

        when().
            I_navigate_the_runner_page().and().
            I_submit_the_selection();

        then().
             iCanParseTheAttachment();
        // @formatter:on
    }

    @Test
    public void I_can_select_report() throws IOException {
        // @formatter:off
        given().
            the_homepage(home);

        when().
            I_navigate_the_runner_page().and().
            I_select$the_options("mx-infos").and().
            I_submit_the_selection();

        then().
             iCanParseTheAttachment().and().
             itContainsOnly("mx-infos");
        // @formatter:on
    }
}

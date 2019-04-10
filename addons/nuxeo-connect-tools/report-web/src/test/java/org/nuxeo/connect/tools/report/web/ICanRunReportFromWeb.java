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

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.connect.tools.report.web.ICanRunReportFromWeb.Given;
import org.nuxeo.connect.tools.report.web.ICanRunReportFromWeb.Then;
import org.nuxeo.connect.tools.report.web.ICanRunReportFromWeb.When;
import org.nuxeo.ecm.webengine.test.WebEngineHomePage;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.web.Attachment;
import org.nuxeo.runtime.test.runner.web.Browser;
import org.nuxeo.runtime.test.runner.web.BrowserFamily;
import org.nuxeo.runtime.test.runner.web.HomePage;
import org.openqa.selenium.WebDriver;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.junit.ScenarioTest;

/**
 * Checks that we can invoke a report using the web interface.
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(WebReportFeature.class)
@Browser(type = BrowserFamily.HTML_UNIT)
@HomePage(type = WebEngineHomePage.class, url = "http://localhost:8082")
@Jetty(port = 8082)
public class ICanRunReportFromWeb extends ScenarioTest<Given, When, Then> {

    @Inject
    WebDriver driver;

    @Inject
    WebEngineHomePage home;

    static class Given extends Stage<Given> {

        @ProvidedScenarioState
        WebDriver driver;

        @ProvidedScenarioState
        WebEngineHomePage home;

        Given theWebDriver(WebDriver driver) {
            this.driver = driver;
            return self();
        }

        Given theHomePage(WebEngineHomePage home) {
            this.home = home;
            return self();
        }

        Given iMLoggedAdministrator() {
            home.getLoginPage().ensureLogin("Administrator", "Administrator");
            return self();
        }

    }

    static class When extends Stage<When> {
        @ExpectedScenarioState
        WebEngineHomePage home;

        RootPage root;

        When iNavigateTheRootModule() {
            root = home.getModulePage("nuxeo-connect-tools", RootPage.class);
            return self();
        }

        @ProvidedScenarioState
        ReportRunnerPage runner;

        When iNavigateTheRunnerPage() {
            runner = root.getRunnerPage();
            return self();
        }

        When iSubmitTheForm() {
            runner = runner.submit();
            return self();
        }
    }

    static class Then extends Stage<Then> {

        @ExpectedScenarioState
        ReportRunnerPage runner;

        Then iReceivedTheAttachment() {
            Attachment attachment = runner.getAttachment();
            Assertions.assertThat(attachment.getFilename()).isEqualTo("nuxeo-connect-tools-report.json");
            return self();
        }
    }

    @Test
    public void iCanRunWebEngine() {
        given()
                .theWebDriver(driver).and()
                .theHomePage(home).and()
                .iMLoggedAdministrator();
        when()
                .iNavigateTheRootModule().and()
                .iNavigateTheRunnerPage().and()
                .iSubmitTheForm();
        then()
                .iReceivedTheAttachment();
    }
}

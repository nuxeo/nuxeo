/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.connect.tools.report.web.jgiven.Stage;
import org.nuxeo.ecm.webengine.test.WebEngineHomePage;
import org.nuxeo.runtime.test.runner.web.Configuration;
import org.openqa.selenium.WebDriver;

import com.tngtech.jgiven.annotation.ProvidedScenarioState;

public class WebEngineHomeStage<SELF extends WebEngineHomeStage<SELF>> extends Stage<SELF> {

    @ProvidedScenarioState
    protected WebEngineHomePage homepage;

    @ProvidedScenarioState
    protected WebDriver driver;

    @ProvidedScenarioState
    protected Configuration configuration;

    public SELF the_homepage(@WebPageFormat WebEngineHomePage homepage) {
        this.homepage = homepage;
        this.driver = homepage.getDriver();
        this.configuration = homepage.getConfiguration();
        return self();
    }

    SELF I_am_on_the_homepage() {
        if (!homepage.getDriver().getCurrentUrl().equals(homepage.getConfiguration().getHome())) {
            I_navigate_the_homepage();
        }
        WebEngineHomeAssert.assertThat(homepage).isCurrent();
        return self();
    }

    SELF I_navigate_the_homepage() {
        homepage.home();
        return self();
    }
}
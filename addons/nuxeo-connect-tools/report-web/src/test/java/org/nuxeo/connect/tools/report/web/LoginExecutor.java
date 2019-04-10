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

import org.nuxeo.connect.tools.report.web.jgiven.StepsRunner;
import org.nuxeo.ecm.webengine.test.WebEngineHomePage;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

class LoginExecutor extends StepsRunner<LoginExecutor> {

    LoginStage<?> stage;

    LoginExecutor with(FeaturesRunner runner) {
        with(runner.getDescription().getTestClass());
        WebEngineHomePage homepage = runner.getInjector().getProvider(WebEngineHomePage.class).get();
        stage = scenario.addStage(LoginStage.class);
        stage.given().the_homepage(homepage);
        return this;
    }

    StepsRunner<LoginExecutor> login() throws Throwable {
        return run(LoginStage.class, "login", steps -> steps.I_login("Administrator", "Administrator"));
    }

    StepsRunner<LoginExecutor> logout() throws Throwable {
        return run(LoginStage.class, "logout", steps -> steps.I_logout());
    }
}
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

import org.nuxeo.ecm.webengine.test.LoginPage;
import org.nuxeo.ecm.webengine.test.WebEngineHomePage;

import com.tngtech.jgiven.annotation.NestedSteps;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;

class LoginStage<SELF extends LoginStage<SELF>> extends WebEngineHomeStage<SELF> {

    LoginPage loginpage;

    @ProvidedScenarioState
    String username;

    @ProvidedScenarioState
    String password;

    @Override
    public SELF the_homepage(WebEngineHomePage homepage) {
        super.the_homepage(homepage);
        loginpage = homepage.getLoginPage();
        return self();
    }

    SELF the_credentials_are_$the_login_name$the_password(String username, String password) {
        this.username = username;
        this.password = password;
        return self();
    }

    @NestedSteps
    SELF I_authenticate() {
        I_fill$the_username(username).and().I_fill$the_password(password).and().I_submit_the_form();
        return self();
    }

    SELF I_am_not_authenticated() {
        LoginAssert.assertThat(loginpage).isNotAuthenticated();
        return self();
    }

    SELF I_am_authenticated() {
        LoginAssert.assertThat(loginpage).isAuthenticated();
        return self();
    }

    SELF I_fill$the_username(String username) {
        this.username = username;
        return self();
    }

    SELF I_fill$the_password(String password) {
        this.password = password;
        return self();
    }

    SELF I_submit_the_form() {
        loginpage.ensureLogin(username, password);
        return self();
    }

    SELF I_click_logout() {
        loginpage.ensureLogout();
        return self();
    }

    @NestedSteps
    SELF I_login(String login, String password) {
        return given().I_am_on_the_homepage().and().I_am_not_authenticated().and()
                .the_credentials_are_$the_login_name$the_password(login, password).then().I_authenticate().then()
                .I_am_authenticated();
    }

    @NestedSteps
    SELF I_logout() {
        return given().I_am_on_the_homepage().and().I_am_authenticated().then().I_click_logout().then()
                .I_am_not_authenticated();
    }
}
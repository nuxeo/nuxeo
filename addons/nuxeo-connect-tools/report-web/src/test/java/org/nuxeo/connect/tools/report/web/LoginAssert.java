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

import org.assertj.core.api.AbstractAssert;
import org.nuxeo.ecm.webengine.test.LoginPage;

class LoginAssert extends AbstractAssert<LoginAssert,LoginPage>  {

    private LoginAssert(LoginPage actual, Class<?> selfType) {
        super(actual, selfType);
    }

    static LoginAssert assertThat(LoginPage actual) {
        return new LoginAssert(actual, LoginAssert.class);
    }

    LoginAssert isAuthenticated() {
        if (!actual.isAuthenticated()) {
            failWithMessage("is not authenticated");
        }
        return this;
    }

    LoginAssert isNotAuthenticated() {
        if (actual.isAuthenticated()) {
            failWithMessage("is authenticated");
        }
        return this;
    }

}
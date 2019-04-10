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
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;

public class RunnerAssert extends AbstractAssert<RunnerAssert,RunnerPage> {

    private RunnerAssert(RunnerPage page, Class<?> selfType) {
        super(page, selfType);
    }

    static RunnerAssert assertThat(RunnerPage page) {
        return new RunnerAssert(page, RunnerAssert.class);
    }


    public RunnerAssert isCurrent() {
        if (!actual.hasElement(By.xpath("//form[@action=\"/connect-tools/report/run\"]"))) {
            failWithMessage(actual + " is not on connect reports runner page");
        }
        return this;
    }

    public void selectsOnly(String... options) {
        Assertions.assertThat(actual.findSelectedOptions()).describedAs(actual + " selects only", (Object) options)
                .containsOnly(options);
    }
}

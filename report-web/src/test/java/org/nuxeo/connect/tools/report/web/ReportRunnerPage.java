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

import org.assertj.core.api.Assertions;
import org.nuxeo.runtime.test.runner.web.WebPage;
import org.openqa.selenium.By;

/**
 *
 *
 */
public class ReportRunnerPage extends WebPage {

    public ReportRunnerPage hasForm() {
        Assertions.assertThat(findElement(By.xpath("//input"))).isNotNull();
        return this;
    }

    public ReportRunnerPage submit() {
        findElement(By.xpath("//input")).submit();
        return getPage(ReportRunnerPage.class);
    }
}

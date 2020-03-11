/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Thierry Delprat
 */
package info.simplecloud.scimproxy.compliance.enteties;

import info.simplecloud.scimproxy.compliance.enteties.TestResult;

public class ReadableTestResult extends TestResult {

    public ReadableTestResult(TestResult result) {
        super(result.getStatus(), result.name, result.message, result.wire);
    }

    public boolean isFailed() {
        return getStatus()==TestResult.ERROR;
    }

    public String getDisplay() {
        return this.statusText + ": " + this.name;
    }

    public String getErrorMessage() {
        return this.message;
    }
}

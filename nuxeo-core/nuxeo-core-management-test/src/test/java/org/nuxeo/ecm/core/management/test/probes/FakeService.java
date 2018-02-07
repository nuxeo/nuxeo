/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.management.test.probes;

import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Fake runtime service to change its status on demand.
 *
 * @since 10.1
 */
public class FakeService extends DefaultComponent {

    protected ProbeStatus status;

    @Override
    public void start(ComponentContext context) {
        setSuccess();
    }

    @Override
    public void stop(ComponentContext context) {
        setFailure();
    }

    public ProbeStatus getStatus() {
        if (status == null) {
            // throw an exception for tests
            throw new IllegalArgumentException("Cannot find remote server!");
        }
        return status;
    }

    public void setSuccess() {
        status = ProbeStatus.newSuccess("success");
    }

    public void setFailure() {
        status = ProbeStatus.newFailure("fail");
    }

    public void setThrowException() {
        status = null;
    }

}

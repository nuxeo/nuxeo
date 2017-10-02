/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.core.management.statuses;

import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple probe to check if the runtime is started
 * 
 * @since 9.3
 */
public class RuntimeStartedProbe implements Probe {

    @Override
    public ProbeStatus run() {
        if (Framework.getRuntime() != null && Framework.getRuntime().isStarted()) {
            return ProbeStatus.newSuccess("Runtime started");
        } else {
            return ProbeStatus.newFailure("Runtime not started");
        }
    }
}

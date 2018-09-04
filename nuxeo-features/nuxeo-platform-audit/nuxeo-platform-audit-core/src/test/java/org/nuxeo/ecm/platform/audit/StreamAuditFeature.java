/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.audit;

import static org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener.STREAM_AUDIT_ENABLED_PROP;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@Features({ ManagementFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.runtime.datasource")
@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.ecm.core.persistence")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.ecm.platform.audit:nxaudit-ds.xml")
@Deploy("org.nuxeo.ecm.platform.audit:test-stream-audit-contrib.xml")
public class StreamAuditFeature extends AuditFeature {

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        // enable it for AuditLogger#await
        Framework.getProperties().setProperty(STREAM_AUDIT_ENABLED_PROP, "true");
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        // disable it for next tests
        Framework.getProperties().setProperty(STREAM_AUDIT_ENABLED_PROP, "false");
    }

}

/*
 * (C) Copyright 2012-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.test;

import org.nuxeo.audit.service.AuditComponent;
import org.nuxeo.audit.test.AuditFeature;
import org.nuxeo.ecm.automation.core.AutomationCoreFeature;
import org.nuxeo.ecm.collections.core.test.CollectionFeature;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.keyvalueblob.KeyValueBlobTransientStoreFeature;
import org.nuxeo.ecm.platform.filemanager.FileManagerFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.web.common.WebCommonFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

@Deploy("org.nuxeo.drive.core")
@Deploy("org.nuxeo.ecm.platform.search.core")
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.platform.webapp.types")
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-sync-root-cache-contrib.xml")
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-types-contrib.xml")
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-descendants-scrolling-cache-contrib.xml")
@Features({ //
        AuditFeature.class, //
        AutomationCoreFeature.class, //
        CollectionFeature.class, //
        FileManagerFeature.class, //
        KeyValueBlobTransientStoreFeature.class, //
        PlatformFeature.class, //
        WebCommonFeature.class })
@WithFrameworkProperty(name = AuditComponent.STREAM_AUDIT_VIRTUAL_EVENTS_ENABLED_PROP, value = "true")
public class NuxeoDriveFeature implements RunnerFeature {

    @Override
    public void start(FeaturesRunner runner) {
        String contrib;
        if (runner.getFeature(AuditFeature.class).isBackendSql()) {
            contrib = "OSGI-INF/test-nuxeodrive-sql-change-finder-contrib.xml";
        } else {
            contrib = "OSGI-INF/test-nuxeodrive-sql-change-finder-empty-contrib.xml";
        }
        try {
            RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
            harness.deployContrib("org.nuxeo.drive.core.test", contrib);
        } catch (Exception e) {
            throw new NuxeoException(e);
        }
    }
}

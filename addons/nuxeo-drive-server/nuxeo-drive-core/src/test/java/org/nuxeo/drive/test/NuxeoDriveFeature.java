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
package org.nuxeo.drive.test;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

@Features({ PlatformFeature.class, SQLAuditFeature.class })
@Deploy("org.nuxeo.drive.core")
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.runtime.reload")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.userworkspace.types")
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.platform.collections.core")
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.webapp.types")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-sync-root-cache-contrib.xml")
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-types-contrib.xml")
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-descendants-scrolling-cache-contrib.xml")
public class NuxeoDriveFeature implements RunnerFeature {

}

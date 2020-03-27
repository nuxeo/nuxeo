/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     - Ku Chang <kchang@nuxeo.com>
 */
package org.nuxeo.audit.storage.stream;

import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.RunnerFeature;

@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.restapi.server.search")
@Deploy("org.nuxeo.ecm.platform.restapi.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit")
@Deploy("org.nuxeo.elasticsearch.audit:audit-jpa-storage-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:nxaudit-ds.xml")
@Deploy("org.nuxeo.elasticsearch.audit:nxuidsequencer-ds.xml")
@Deploy("org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:audit-test-contrib.xml")
@Deploy("org.nuxeo.audit.storage.directory")
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.audit.storage.directory:OSGI-INF/test-stream-audit-storage-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.audit:OSGI-INF/test-audit-contrib.xml")
public class AuditStorageFeature implements RunnerFeature {

}

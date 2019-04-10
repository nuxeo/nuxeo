/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.test;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Elasticsearch audit feature cleaning up audit log after each test.
 *
 * @since 8.2
 */
@Features({ PlatformFeature.class, RepositoryElasticSearchFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.audit", "org.nuxeo.elasticsearch.seqgen", "org.nuxeo.elasticsearch.audit",
        "org.nuxeo.drive.elasticsearch" })
@LocalDeploy("org.nuxeo.drive.elasticsearch:OSGI-INF/test-nuxeodrive-elasticsearch-contrib.xml")
public class ESAuditFeature extends SimpleFeature {

}

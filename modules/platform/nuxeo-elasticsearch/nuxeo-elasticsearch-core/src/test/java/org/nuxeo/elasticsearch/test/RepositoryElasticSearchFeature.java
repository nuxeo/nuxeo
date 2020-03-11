/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.NuxeoLoginFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LogFeature;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Deploy("org.nuxeo.ecm.automation.server")
@Deploy("org.nuxeo.ecm.automation.io")
@Deploy("org.nuxeo.ecm.platform.forms.layout.export")
@Deploy("org.nuxeo.ecm.webengine.core")
@Deploy("org.nuxeo.ecm.webengine.jaxrs")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.core.management")
@Features({ NuxeoLoginFeature.class, LogFeature.class, RepositoryLightElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core.test:elastic-search-core-management-tests-component.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class RepositoryElasticSearchFeature extends RepositoryLightElasticSearchFeature {
    // Same as RepositoryLightElasticSearchFeature with more deployments
}

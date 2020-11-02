/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.server.jaxrs.search.test;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 11.1
 */
@Features({ RestServerFeature.class, RepositoryElasticSearchFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.platform.webapp.types")
@Deploy("org.nuxeo.ecm.platform.search.core")
@Deploy("org.nuxeo.ecm.platform.restapi.server.search")
@Deploy("org.nuxeo.ecm.platform.restapi.test:pageprovider-test-contrib.xml")
public class SearchRestFeature implements RunnerFeature {

}
